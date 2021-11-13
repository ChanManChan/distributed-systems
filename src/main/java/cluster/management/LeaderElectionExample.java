package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Node:- A process running on a dedicated machine as part of a distributed system, when two nodes have an edge between them,
 * it means those two processes can communicate with each other through the network.
 * Cluster:- Collection of computers/nodes connected to each other
 * The nodes in a cluster are working on the same task, and typically are running the same code
 * <p>
 * How to hand a task to a Cluster?
 * When we have a very large amount of data analyze or a complex computation to solve, we want to hand this task to a cluster of nodes.
 * The question then is, what part of the task is going to be performed by which node, after all the biggest benefit of a distributed system
 * is that we can parallelize the work and let each node work independently for that common goal.
 * <p>
 * Attempt 1:- We could manually distribute the work and assign each node a separate task, but this obviously would not scale
 * Attempt 2:- Manually elect a leader and that leader/master node will be in charge of distributing the work and collecting the results
 * Problem- Leader failure (entire cluster gets decommissioned)
 * Attempt 3:- Automatic leader election on demand by the nodes. If the master node becomes unavailable the remaining nodes will reelect a new leader
 * Later if the old leader joins the cluster after recovery, it should realize that its not a leader anymore
 * and would join as a regular node to help with the work.
 * <p>
 * Challenges of Master-Workers architecture:-
 * Each node knows only about itself - Service registry and discovery is required.
 * Failure detection mechanism is necessary to trigger automatic leader reelection in a cluster
 * <p>
 * Solution:- Apache Zookeeper - High performance Distributed System Coordination Service. Provides an abstraction layer for higher level distributed algorithms for our cluster
 * Instead of having our nodes communicating directly with each other to coordinate the work, they are going to communicate with the ZooKeeper servers directly instead
 * ZooKeepers abstraction and data model- Looks like a tree and is very similar to a file system
 * Each element in that tree or virtual file system is called a Znodes
 * Znodes - Hybrid between a file and a directory
 * They can store data inside like a file
 * They can have children znodes like a directory
 * Two types of Znodes:-
 * Persistent - persists between sessions (if our application disconnects from ZooKeeper and then reconnects again a persistent Znode that was created by our application stays intact with all its children and data.
 * Ephemeral - gets deleted when the session ends (ephemeral Znode gets deleted as soon as the application that created it disconnects from ZooKeeper)
 * <p>
 * Leader Election Algorithm:-
 * Step1- Every node that connects to zookeeper volunteers to become a leader. Each node submits its candidacy by adding a Znode that represents itself under the election parent.
 * Since ZooKeeper maintains a global order it can name each Znode according to the order of their addition.
 * Step2- After each node finishes creating a Znode it would query the current children of the election parent.
 * Because of that order that zookeeper provides us, each node when querying the children of the election parent is guaranteed to see all the Znodes created prior to its own Znodes creation
 * Step3- If the Znode that the current node created is the smallest number, it knows that it is now the leader.
 * If the Znode that the current node created is not the smallest, then the node knows that it's not the leader, and it is now waiting for instructions from the elected leader.
 * <p>
 * This is how we break the symmetry and arrive to a global agreement on the leader node
 */

// Fault Tolerance and Horizontal Scalability are very important properties

public class LeaderElectionExample implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ELECTION_NAMESPACE = "/election";
    private ZooKeeper zooKeeper;
    private String currentZnodeName;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        LeaderElectionExample leaderElectionExample = new LeaderElectionExample();
        leaderElectionExample.connectToZooKeeper();

        leaderElectionExample.volunteerForLeadership();
        leaderElectionExample.reelectLeader();

        leaderElectionExample.run(); // zooKeeper is event driven, the events from zooKeeper come on a different thread and
        // before zooKeeper even has a chance to respond to our application and trigger an event on another thread
        // our application simply finishes. Therefore, put the main thread into a wait state.

        leaderElectionExample.close(); // once the main thread wakes up and exits the run method, it will call the close method
        System.out.println("Disconnected from ZooKeeper, exiting application");
    }

    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Znode name: " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void reelectLeader() throws InterruptedException, KeeperException {
        Stat predecessorStat = null;
        String predecessorZnodeName = "";
        while (predecessorStat == null) {
            // repeat the process as long as we are not elected to be a leader or until we found an existing Znode to watch for failures
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

            Collections.sort(children);
            String smallestChild = children.get(0);

            if (smallestChild.equals(currentZnodeName)) {
                System.out.println("I am the leader");
                return;
            } else {
                System.out.println("I am not the leader, " + smallestChild + " is the leader");
                int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                predecessorZnodeName = children.get(predecessorIndex);
                // Race Condition - if we call exists() on a Znode that is already gone the return value of the exists() method is going to be null
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this); // watcher object will get notified if and when the predecessor Znode gets deleted
            }
        }

        System.out.println("Watching znode " + predecessorZnodeName);
        System.out.println();
    }

    public void connectToZooKeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to ZooKeeper");
                } else {
                    // handle lose connection to zooKeeper event
                    // wake up the main thread and allow our application to close resources and exit
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from ZooKeeper event");
                        zooKeeper.notifyAll();
                    }
                }
                break;
            case NodeDeleted:
                try {
                    reelectLeader();
                } catch (InterruptedException | KeeperException e) {
                    e.printStackTrace();
                }
        }
    }
}
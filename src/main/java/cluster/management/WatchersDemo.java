package cluster.management;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

/**
 * The Herd Effect:-
 * A large number of nodes waiting for an event
 * When the event happens all nodes get notified, and they all wake up
 * Even though they all wake up and try to accomplish something only one node can "succeed" (become the master in this case)
 * This indicates bad design, can negatively impact the performance and can completely freeze the cluster
 * <p>
 * Leader Reelection Attempt1:- Herd effect
 * If the leader dies, the small ZooKeeper cluster will have to notify a potentially large cluster of nodes (NodeDeleted Event)
 * Then all the nodes are going to call the getChildren() method to get a new view of the Znodes hierarchy. So they will all bombard ZooKeeper with requests all in the same time.
 * After leader is reelected all the nodes are going to start watching the Znode of the leader, so they will all send lot of requests simultaneously
 * With large number of nodes in a cluster, this is going to really overwhelm zookeeper, so we should avoid this type of design as much as possible
 * <p>
 * Leader Reelection Attempt2:-
 * After the initial leader election, instead of all the nodes watching the leaders Znode, each node is going to watch only Znode that comes right before it in the sequence of candidate Znodes.
 * This way if the leader dies, the only node that is going to be notified is its immediate successor. That node will need to call getChildren() again to make sure it owns the znode with the smallest sequence number and in that case it knows that it's the new leader.
 * In this configuration our cluster is forming a virtual chain of nodes where each node is watching another nodes back and protecting the overall system against the failure
 */

public class WatchersDemo implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;
    private static final String TARGET_ZNODE = "/target-znode";

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        WatchersDemo watchersDemo = new WatchersDemo();
        watchersDemo.connectToZooKeeper();
        watchersDemo.watchTargetZnode();
        watchersDemo.run();
        watchersDemo.close();
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

    public void watchTargetZnode() throws InterruptedException, KeeperException {
        // Watchers registered with getChildren(), exists() and getData() are one-time triggers
        // If we want to get future notifications, we need to register the watcher again
        Stat stat = zooKeeper.exists(TARGET_ZNODE, this);
        if (stat == null) {
            return;
        }

        byte[] data = zooKeeper.getData(TARGET_ZNODE, this, stat);
        List<String> children = zooKeeper.getChildren(TARGET_ZNODE, this);

        System.out.println("Data: " + new String(data) + " children: " + children);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        // get notified when these commands are run
        // create /target-znode "some test data"
        // set /target-znode "some new data"
        // create /target-znode/child_znode " "
        // deleteall /target-znode
        switch (watchedEvent.getType()) {
            case None:
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to ZooKeeper");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from ZooKeeper event");
                        zooKeeper.notifyAll();
                    }
                }
                break;
            case NodeDeleted:
                System.out.println(TARGET_ZNODE + " was deleted");
                break;
            case NodeCreated:
                System.out.println(TARGET_ZNODE + " was created");
                break;
            case NodeDataChanged:
                System.out.println(TARGET_ZNODE + " data changed");
                break;
            case NodeChildrenChanged:
                System.out.println(TARGET_ZNODE + " children changed");
                break;
        }
        try {
            watchTargetZnode();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}

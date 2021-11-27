package distributedsearch.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    private static final String ELECTION_NAMESPACE = "/election";
    private String currentZNodeName;
    private final ZooKeeper zooKeeper;
    private final OnElectionCallback onElectionCallback;

    public LeaderElection(ZooKeeper zooKeeper, OnElectionCallback onElectionCallback) {
        this.zooKeeper = zooKeeper;
        this.onElectionCallback = onElectionCallback;
    }

    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        String zNodePrefix = ELECTION_NAMESPACE + "/c_";
        String zNodeFullPath = zooKeeper.create(zNodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("ZNode name: " + zNodeFullPath);
        this.currentZNodeName = zNodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void reelectLeader() throws InterruptedException, KeeperException {
        Stat predecessorStat = null;
        String predecessorZNodeName = "";
        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
            Collections.sort(children);
            String smallestChild = children.get(0);

            if (smallestChild.equals(currentZNodeName)) {
                System.out.println("I am the leader");
                onElectionCallback.onElectedToBeLeader();
                return;
            } else {
                System.out.println("I am the worker");
                onElectionCallback.onWorker();
                int predecessorIndex = Collections.binarySearch(children, currentZNodeName) - 1;
                predecessorZNodeName = children.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZNodeName, this);
            }
        }
        System.out.println("Watching zNode: " + predecessorZNodeName);
        System.out.println();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case NodeDeleted:
                try {
                    reelectLeader();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (KeeperException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}

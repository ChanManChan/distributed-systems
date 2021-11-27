package distributedsearch;

import distributedsearch.management.LeaderElection;
import distributedsearch.management.OnElectionAction;
import distributedsearch.management.ServiceRegistry;
import distributedsearch.networking.WebServer;
import distributedsearch.search.UserSearchHandler;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

// application could be front-end or back-end depending on the cli arguments
// the application could be worker, coordinator or the web client
public class Application implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final int DEFAULT_PORT = 9090;
    private static final String BACK_END = "back-end";
    private static final String FRONT_END = "front-end";
    private static final String DEFAULT_TYPE = BACK_END;
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        int currentServerPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        String type = args.length > 0 ? args[1] : DEFAULT_TYPE;
        Application application = new Application();
        ZooKeeper zooKeeper = application.connectToZooKeeper();

        // the only difference between the worker service registry and the coordinator service registry is the ZNode
        // name we pass into the constructor which tells the service registry where to store the addresses
        ServiceRegistry workersServiceRegistry = new ServiceRegistry(zooKeeper, ServiceRegistry.WORKERS_REGISTRY_ZNODE);
        ServiceRegistry coordinatorsServiceRegistry = new ServiceRegistry(zooKeeper, ServiceRegistry.COORDINATORS_REGISTRY_ZNODE);

        if (type.equals(FRONT_END)) {
            // front-end just needs the address of the coordinator.
            // the coordinator then communicates with worker nodes.
            UserSearchHandler searchHandler = new UserSearchHandler(coordinatorsServiceRegistry);
            WebServer webServer = new WebServer(currentServerPort, searchHandler);
            webServer.startServer();
        } else if (type.equals(BACK_END)) {
            OnElectionAction onElectionAction = new OnElectionAction(workersServiceRegistry, coordinatorsServiceRegistry, currentServerPort);
            LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
            leaderElection.volunteerForLeadership();
            leaderElection.reelectLeader();
        }

        application.run();
        application.close();
        System.out.println("Disconnected from ZooKeeper, exiting application");
    }

    public ZooKeeper connectToZooKeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        return zooKeeper;
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait(); // main thread will wait until it receives notifyAll() indication
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
                    System.out.println("Disconnected from ZooKeeper event");
                    zooKeeper.notifyAll();
                }
                break;
        }
    }
}

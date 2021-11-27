package distributedsearch.management;

import distributedsearch.search.SearchCoordinator;
import distributedsearch.search.SearchWorker;
import distributedsearch.networking.WebClient;
import distributedsearch.networking.WebServer;
import org.apache.zookeeper.KeeperException;

import java.net.InetAddress;
import java.net.UnknownHostException;

// This is the place where depending on the role of this particular instance we instantiate all the components necessary for this node to perform its role.
public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry workersServiceRegistry;
    private final ServiceRegistry coordinatorsServiceRegistry;
    private final int port;
    private WebServer webServer;

    public OnElectionAction(ServiceRegistry workersServiceRegistry, ServiceRegistry coordinatorsServiceRegistry, int port) {
        this.workersServiceRegistry = workersServiceRegistry;
        this.coordinatorsServiceRegistry = coordinatorsServiceRegistry;
        this.port = port;
    }

    @Override
    public void onElectedToBeLeader() {
        workersServiceRegistry.unregisterFromCluster(); // new leader should unregister from the worker node pool
        workersServiceRegistry.registerForUpdates(); // update the new list of worker node children under WORKERS_REGISTRY_ZNODE = "/workers_service_registry"

        if (webServer != null) {
            webServer.stop();
        }

        SearchCoordinator searchCoordinator = new SearchCoordinator(workersServiceRegistry, new WebClient());
        webServer = new WebServer(port, searchCoordinator);
        webServer.startServer();

        try {
            String currentServerAddress = String.format("http://%s:%d%s", InetAddress.getLocalHost().getCanonicalHostName(), port, searchCoordinator.getEndpoint());
            coordinatorsServiceRegistry.registerToCluster(currentServerAddress);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (UnknownHostException | KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWorker() {
        SearchWorker searchWorker = new SearchWorker();
        if (webServer == null) {
            webServer = new WebServer(port, searchWorker);
            webServer.startServer();
        }

        try {
            String currentServerAddress = String.format("http://%s:%d%s", InetAddress.getLocalHost().getCanonicalHostName(), port, searchWorker.getEndpoint());
            workersServiceRegistry.registerToCluster(currentServerAddress);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (UnknownHostException | KeeperException e) {
            e.printStackTrace();
        }
    }
}

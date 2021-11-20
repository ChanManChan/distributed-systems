package distributed.search;

import org.apache.zookeeper.KeeperException;

import java.net.InetAddress;
import java.net.UnknownHostException;

// This is the place where depending on the role of this particular instance we instantiate all the components necessary for this node to perform its role.
public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry serviceRegistry;
    private final int port;
    private WebServer webServer;

    public OnElectionAction(ServiceRegistry serviceRegistry, int port) {
        this.serviceRegistry = serviceRegistry;
        this.port = port;
    }

    @Override
    public void onElectedToBeLeader() {
        serviceRegistry.unregisterFromCluster();
        serviceRegistry.registerForUpdates();
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
            serviceRegistry.registerToCluster(currentServerAddress);
        } catch (UnknownHostException | KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

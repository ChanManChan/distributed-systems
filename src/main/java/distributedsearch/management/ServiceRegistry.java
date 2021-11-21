package distributedsearch.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {
    public static final String WORKERS_REGISTRY_ZNODE = "/workers_service_registry";
    public static final String COORDINATORS_REGISTRY_ZNODE = "/coordinators_service_registry";
    private final ZooKeeper zooKeeper;
    private List<String> allServiceAddresses = null;
    private String currentZNode = null;
    private final String serviceRegistryZNode;

    public ServiceRegistry(ZooKeeper zooKeeper, String serviceRegistryZNode) {
        this.zooKeeper = zooKeeper;
        this.serviceRegistryZNode = serviceRegistryZNode;
        createServiceRegistryNode();
    }

    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {
        if (currentZNode != null) {
            System.out.println("Already registered to service registry");
            return;
        }
        this.currentZNode = zooKeeper.create(serviceRegistryZNode + "/n_", metadata.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to service registry");
    }

    public void registerForUpdates() {
        try {
            updateAddresses();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public void unregisterFromCluster() {
        try {
            if (currentZNode != null && zooKeeper.exists(currentZNode, false) != null) {
                zooKeeper.delete(currentZNode, -1);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createServiceRegistryNode() {
        try {
            if (zooKeeper.exists(serviceRegistryZNode, false) == null) {
                zooKeeper.create(serviceRegistryZNode, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<String> getAllServiceAddresses() throws InterruptedException, KeeperException {
        if (allServiceAddresses == null) {
            updateAddresses();
        }
        return allServiceAddresses;
    }

    private synchronized void updateAddresses() throws InterruptedException, KeeperException {
        // go through all the children (worker) of/in the service registry namespace and keep track of its addresses
        List<String> workers = zooKeeper.getChildren(serviceRegistryZNode, this);
        List<String> addresses = new ArrayList<>(workers.size());

        for (String worker : workers) {
            String serviceFullPath = serviceRegistryZNode + "/" + worker;
            Stat stat = zooKeeper.exists(serviceFullPath, false);
            // if between get the list of children .getChildren() and calling the .exists() method, if that child zNode disappears then the result from the method exists() is going to be null
            // yet another race condition that we have no control over, but we handle it by continuing to the next zNode in the list.
            if (stat == null) {
                continue;
            }

            byte[] addressBytes = zooKeeper.getData(serviceFullPath, false, stat);
            String address = new String(addressBytes);
            addresses.add(address);
        }

        this.allServiceAddresses = Collections.unmodifiableList(addresses);
        System.out.println("The cluster addresses are: " + this.allServiceAddresses);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        try {
            updateAddresses();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}

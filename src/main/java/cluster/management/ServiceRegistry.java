package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service Registry with Zookeeper:-
 * Upon connection to zookeeper, each node is going to create an ephemeral znode under the service registry parent and store its address in it's znode for others to see.
 * Then any node that is interested in getting the address of another node simple needs to call getChildren() to get all the existing znode under the service registry and then call the getData() method to read the address inside the znode.
 * We need to store the minimum data to allow communication within the cluster
 * The address we will store will be in the form of:
 * Host Name:Port
 * http://127.0.0.1:8080
 */

/**
 * Fully automated service registry and discovery using ZooKeeper
 * Using the addresses published in the service registry we can establish the communication within the cluster
 */

public class ServiceRegistry implements Watcher {
    private static final String SERVICE_REGISTRY_NAMESPACE = "/service_registry";
    private final ZooKeeper zooKeeper;
    private String currentZNode = null;
    private List<String> allServicesAddresses = null;

    public ServiceRegistry(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
        createServiceRegistryZNode();
    }

    public void registerForUpdates() throws InterruptedException, KeeperException {
        updateAddresses();
    }

    public synchronized List<String> getAllServicesAddresses() throws InterruptedException, KeeperException {
        if (allServicesAddresses == null) {
            updateAddresses();
        }
        return allServicesAddresses;
    }

    public void unregisterFromCluster() throws InterruptedException, KeeperException {
        // if the node gracefully shut itself down or the worker suddenly becomes the leader, so it would want to unregister to avoid communicating with itself
        if (currentZNode != null && zooKeeper.exists(currentZNode, false) != null) {
            zooKeeper.delete(currentZNode, -1);
        }
    }

    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {
        if (this.currentZNode != null) {
            System.out.println("Already registered to service registry");
            return;
        }
        String zNodePrefix = SERVICE_REGISTRY_NAMESPACE + "/n_";
        this.currentZNode = zooKeeper.create(zNodePrefix, metadata.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to service registry");
    }

    private void createServiceRegistryZNode() {
        try {
            if (zooKeeper.exists(SERVICE_REGISTRY_NAMESPACE, false) == null) {
                zooKeeper.create(SERVICE_REGISTRY_NAMESPACE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void updateAddresses() throws InterruptedException, KeeperException {
        List<String> workerZNodes = zooKeeper.getChildren(SERVICE_REGISTRY_NAMESPACE, this);
        List<String> addresses = new ArrayList<>(workerZNodes.size());

        for (String workerZNode : workerZNodes) {
            String workerZNodeFullPath = SERVICE_REGISTRY_NAMESPACE + "/" + workerZNode;
            Stat stat = zooKeeper.exists(workerZNodeFullPath, false);
            if (stat == null) {
                continue;
            }

            byte[] addressBytes = zooKeeper.getData(workerZNodeFullPath, false, stat);
            String address = new String(addressBytes);
            addresses.add(address);
        }

        this.allServicesAddresses = Collections.unmodifiableList(addresses);
        System.out.println("The cluster addresses are: " + this.allServicesAddresses);
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

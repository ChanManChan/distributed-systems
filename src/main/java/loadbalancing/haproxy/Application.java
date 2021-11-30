package loadbalancing.haproxy;

public class Application {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("java -jar (jar name) PORT_NUMBER SERVER_NAME");
            return;
        }

        int currentServerPort = Integer.parseInt(args[0]);
        String serverName = args[1];
        WebServer webServer = new WebServer(currentServerPort, serverName);
        webServer.startServer();
    }
}

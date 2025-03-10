package depchain.client;

import java.net.*;
import java.security.PrivateKey;
import java.util.List;

import com.google.gson.Gson;
import depchain.client.domain.Client;
import depchain.common.*;
import depchain.common.domain.Entity;

public class ClientMain {
    private static int clientPort;
    private static String clientName;
    private static final Gson gson = new Gson();
    private static DatagramSocket socketToLeader;
    private static PrivateKey privateKey;
    private static final String MEMBERSHIP_FILE = "membership/membership.txt";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"<clientPort> <clientName>\"");
            System.exit(1);
        }
        int clientPort = Integer.parseInt(args[0]);
        // IMPORTANT -> if the client's name isn't "paulo", we're cooked
        String clientName = args[1];
        System.out.println("Client " + clientName + " started at port " + clientPort);
        List<Entity> membershipInfo = CommonUtils.loadMembership(MEMBERSHIP_FILE);
        boolean debug = true;
        Client client = new Client(clientName, clientPort, membershipInfo, debug);
        client.start();
    }
}

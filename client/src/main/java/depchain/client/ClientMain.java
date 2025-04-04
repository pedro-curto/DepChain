package depchain.client;

import java.util.List;

import depchain.client.domain.ByzantineClient;
import depchain.client.domain.Client;
import depchain.common.*;
import depchain.common.domain.Entity;

public class ClientMain {
    private static final String baseDir = System.getProperty("user.dir");
    private static final String MEMBERSHIP_FILE = baseDir + "/membership/membership.txt";

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"<clientPort> <clientName> <byzantineBehaviour>\"");
            System.exit(1);
        }
        int clientPort = Integer.parseInt(args[0]);
        String clientName = args[1];
        int byzantineBehaviour = Integer.parseInt(args[2]);
        System.out.println("Client " + clientName + " started at port " + clientPort + " with byzantine behaviour: " + byzantineBehaviour);
        List<Entity> membershipInfo = CommonUtils.loadMembership(MEMBERSHIP_FILE);
        boolean debug = false;
        if (byzantineBehaviour == 0) {
            Client client = new Client(clientName, clientPort, membershipInfo, debug);
            client.start();
        } else {
//            // Byzantine client
//            String byzantineType = switch (byzantineBehaviour) {
//                case 1 -> "fake-signature";
//                case 2 -> "replay-transfer";
//                default -> "unknown";
//            };
            Client client = new ByzantineClient(clientName, clientPort, membershipInfo, byzantineBehaviour, debug);
            System.out.println("Byzantine client created with type: " + byzantineBehaviour);
            client.start();
        }
    }
}

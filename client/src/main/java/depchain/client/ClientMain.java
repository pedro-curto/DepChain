package depchain.client;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import depchain.client.domain.Client;
import depchain.common.*;
import depchain.common.domain.Account;
import depchain.common.domain.Block;
import depchain.common.domain.Entity;

public class ClientMain {
    private static final String baseDir = System.getProperty("user.dir");
    private static final String MEMBERSHIP_FILE = baseDir + "/membership/membership.txt";

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
        boolean debug = false;
        Client client = new Client(clientName, clientPort, membershipInfo, debug, false);
        client.start();
    }
}

package depchain.client;

import java.net.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;
import depchain.client.domain.Client;
import depchain.common.*;
import depchain.client.links.PerfectLink;

import javax.crypto.SecretKey;

public class ClientMain {
    private static int clientPort;
    private static String clientName;
    private static String LEADER_FILE = "membership/leader.txt";
    private static final Gson gson = new Gson();
    private static DatagramSocket socketToLeader;
    private static PrivateKey privateKey;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"<clientPort> <clientName>\"");
            System.exit(1);
        }
        int clientPort = Integer.parseInt(args[0]);
        // IMPORTANT -> if the client's name isn't "paulo", we're cooked
        String clientName = args[1];
        // loads leader info
        Leader leader = LeaderLoader.leaderLoader(LEADER_FILE);
        System.out.println("Client " + clientName + " started at port " + clientPort);
        System.out.println("Leader is: " + leader);
        Client client = new Client(clientName, clientPort, leader);
        client.start();
    }
}

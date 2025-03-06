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

//        byte[] sendData;
//        socketToLeader = new DatagramSocket(clientPort);
//        privateKey = KeyUtils.readPrivateKey("membership/");
//        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
//        PerfectLink perfectLink = new PerfectLink(socketToLeader, messageQueue);
//        Thread messageDeliveringThread = new Thread(() -> deliverMessage(messageQueue));
//        messageDeliveringThread.start();
//        Scanner input = new Scanner(System.in);

//        // generate symmetric key to communicate with leader
//        SecretKey symKeyForLeader = Security.generateSecretKey();
//        PublicKey leaderPubKey = Security.getMemberPublicKey(leader.getName());
//        byte[] encryptedKey = Security.encryptSymKeyWithAsymKey(symKeyForLeader, leaderPubKey);
//        String b64EncryptedKey = Base64.getEncoder().encodeToString(encryptedKey);
//        String msgId = UUID.randomUUID().toString();
//        String dataToSign = msgId + clientName + b64EncryptedKey + "clientKey";
//        String signature = Security.makeDS(dataToSign, Security.getMemberPrivateKey(clientName));
//        Message keyMsg = new Message(msgId,
//                clientName, b64EncryptedKey, "clientKey");


//        while (true) {
//            String content = input.nextLine();
//            if (content.equals("QUIT")) {
//                socketToLeader.close();
//                input.close();
//                System.exit(0);
//            }
//            String msgId = UUID.randomUUID().toString();
//
//            Message msg = new Message(msgId, clientName, content, null, "client", "append");
//            String json = gson.toJson(msg);
//            sendData = json.getBytes();
//
//            System.out.println("Sending message: " + json);
//            DatagramPacket sendPacket = new DatagramPacket(
//                sendData,
//                sendData.length,
//                InetAddress.getByName(leader.getAddress()),
//                leader.getPort());
//            perfectLink.sendMessage(sendPacket, msgId);
//        }
    }

//    private static void deliverMessage(BlockingQueue<String> messageQueue) {
//        while (true) {
//            String message = null;
//            try {
//                message = messageQueue.take();
//            } catch (InterruptedException e) {
//                continue;
//            }
//            System.out.println("[SERVER GOT]: " + message);
//        }
//    }
}

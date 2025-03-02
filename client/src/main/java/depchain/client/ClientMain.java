package depchain.client;

import java.net.*;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;
import depchain.common.Leader;
import depchain.common.LeaderLoader;
import depchain.common.Message;
import depchain.client.links.PerfectLink;

public class ClientMain {
    private static int clientPort;
    private static String clientName;
    private static String LEADER_FILE = "membership/leader.txt";
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"<clientPort> <clientName>\"");
            System.exit(1);
        }
        clientPort = Integer.parseInt(args[0]);
        clientName = args[1];
        // loads leader info
        Leader leader = LeaderLoader.leaderLoader(LEADER_FILE);
        System.out.println("Client " + clientName + " started at port " + clientPort);
        System.out.println("Leader is: " + leader);

        byte[] sendData;
        DatagramSocket clientSocket = new DatagramSocket();
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        PerfectLink perfectLink = new PerfectLink(clientSocket, messageQueue);
        Thread messageDeliveringThread = new Thread(() -> deliverMessage(messageQueue));
        messageDeliveringThread.start();
        Scanner input = new Scanner(System.in);
        
        while (true) {
            String content = input.nextLine();
            if (content.equals("QUIT")) {
                clientSocket.close();
                input.close();
                System.exit(0);
            }
            String msgId = UUID.randomUUID().toString();

            Message msg = new Message(msgId, clientName, content, null, "client");
            String json = gson.toJson(msg);
            sendData = json.getBytes();

            System.out.println("Sending message: " + json);
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, 
                sendData.length, 
                InetAddress.getByName(leader.getAddress()),
                leader.getPort());
            perfectLink.sendMessage(sendPacket, msgId);
        }
    }

    private static void deliverMessage(BlockingQueue<String> messageQueue) {
        while (true) {
            String message = null;
            try {
                message = messageQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            System.out.println("[SERVER GOT]: " + message);
        }
    }
}

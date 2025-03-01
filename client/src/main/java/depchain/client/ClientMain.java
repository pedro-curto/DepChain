package depchain.client;

import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;
import depchain.client.utils.KeyUtils;
import depchain.common.Message;
import depchain.common.SignatureUtils;
import depchain.client.links.PerfectLink;

public class ClientMain {
    private static final String ADDRESS = "localhost";
    private static final int SERVER_PORT = 5001;
    private static int clientPort;
    private static String clientName;
    private static String privKeyPath;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"<clientPort> <clientName>\"");
            System.exit(1);
        }
        clientPort = Integer.parseInt(args[0]);
        clientName = args[1];
        privKeyPath = "keys/" + clientName + "/" + clientName + ".privkey";
        System.out.println("Client " + clientName + " started and listening on port " + clientPort);
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
            // sign msgId, senderId, msgContent
            String dataToSign = msgId + clientName + content;
            // loads private key from keys/clientName/clientName.privkey and signs dataToSign
            PrivateKey privateKey = KeyUtils.readPrivateKey(privKeyPath);
            String signature = SignatureUtils.makeDS(dataToSign, privateKey);

            Message msg = new Message(msgId, "client", content, signature);
            Gson gson = new Gson();
            String json = gson.toJson(msg);
            sendData = json.getBytes();

            System.out.println("Sending message: " + json);
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, 
                sendData.length, 
                InetAddress.getByName(ADDRESS), 
                SERVER_PORT);
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

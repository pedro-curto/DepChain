package depchain.client.domain;

import com.google.gson.Gson;
import depchain.client.links.PerfectLink;
import depchain.common.Leader;
import depchain.common.Message;
import depchain.common.Security;
import depchain.common.session.Session;
import depchain.common.MessageType;

import javax.crypto.SecretKey;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private final int port;
    private final String myName;
    private final Leader leader;
    private final Gson gson = new Gson();
    private PerfectLink perfectLink;
    private BlockingQueue<String> messageQueue;
    // session has session key and socket for communication with leader
    private Session sessionWithLeader;

    public Client(String clientName, int port, Leader leader) {
        this.myName = clientName;
        this.port = port;
        this.leader = leader;
    }

    public void start() throws Exception {
        // init socket, messageQueue and the perfectLink abstraction
        DatagramSocket socket = new DatagramSocket(port);
        messageQueue = new LinkedBlockingQueue<>();
        perfectLink = new PerfectLink(socket, messageQueue);

        // handshake with leader to deliver the session key
        SecretKey sessionKey = Security.generateSecretKey();
        sessionWithLeader = new Session(UUID.randomUUID().toString(), sessionKey, leader.getPort(), leader.getAddress());
        sendSessionKeyToLeader(sessionKey);

        // start a thread to deliver incoming messages
        Thread messageDeliveringThread = new Thread(() -> deliverMessage(messageQueue));
        messageDeliveringThread.start();

        // start processing user input
        processUserInput();
    }

    private void sendSessionKeyToLeader(SecretKey sessionKey) throws Exception {
        // encrypt session key using leader's public key
        PublicKey leaderPubKey = Security.getMemberPublicKey(leader.getName());
        byte[] encryptedKey = Security.encryptSymKeyWithAsymKey(sessionKey, leaderPubKey);
        String b64EncryptedKey = Base64.getEncoder().encodeToString(encryptedKey);
        // concatenate message fields and generate hmac for integrity check
        String msgId = UUID.randomUUID().toString();
        String dataForHMAC = msgId + myName + b64EncryptedKey + "clientKey";
        String hmac = Security.generateHMAC(dataForHMAC, sessionKey);

        // create special handshake message
        Message keyMsg = new Message(msgId, myName, b64EncryptedKey, hmac, MessageType.KEY_EXCHANGE);
        String json = gson.toJson(keyMsg);
        DatagramPacket packet = new DatagramPacket(
                json.getBytes(),
                json.length(),
                InetAddress.getByName(leader.getAddress()),
                leader.getPort());
        perfectLink.sendMessage(packet, msgId);
        System.out.println("[Client] Sent session key to leader.");
    }

    private void processUserInput() throws Exception {
        Scanner input = new Scanner(System.in);
        while (true) {
            String content = input.nextLine();
            if (content.equals("QUIT")) {
                DatagramSocket socket = sessionWithLeader.getSocket();
                socket.close();
                input.close();
                System.exit(0);
            }
            String msgId = UUID.randomUUID().toString();
            Message msg = new Message(msgId, myName, content, MessageType.CLIENT_APPEND);
            String json = gson.toJson(msg);
            DatagramPacket sendPacket = new DatagramPacket(
                    json.getBytes(),
                    json.length(),
                    InetAddress.getByName(leader.getAddress()),
                    leader.getPort());
            perfectLink.sendMessage(sendPacket, msgId);
            System.out.println("[Client] Sent message: " + json);
        }
    }

    private static void deliverMessage(BlockingQueue<String> messageQueue) {
        while (true) {
            String message;
            try {
                message = messageQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            System.out.println("[SERVER GOT]: " + message);
        }
    }
}

package depchain.client;

import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.util.Scanner;
import java.util.UUID;

import com.google.gson.Gson;
import depchain.client.utils.KeyUtils;
import depchain.library.Message;
import depchain.library.SignatureUtils;
import depchain.client.links.PerfectLink;

public class ClientMain {
    private static PerfectLink perfectLink;
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
        perfectLink = new PerfectLink(clientSocket);
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
            //content = msgId + "||" + content;
            
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

    
}

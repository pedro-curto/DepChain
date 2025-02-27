package depchain.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.UUID;

import depchain.client.links.PerfectLink;

public class UDPClient {
    private static PerfectLink perfectLink;
    private static final String ADDRESS = "localhost";
    private static final int PORT = 5001;

    public static void main(String[] args) throws IOException {
        byte[] sendData;
        DatagramSocket clientSocket = new DatagramSocket();
        perfectLink = new PerfectLink(clientSocket);
        Scanner input = new Scanner(System.in);
        
        while (true) {
            String cmd = input.nextLine();
            if (cmd.equals("QUIT")) {
                clientSocket.close();
                input.close();
                System.exit(0);
            }
            String msgId = UUID.randomUUID().toString();
            cmd = msgId + "||" + cmd;
            
            sendData = cmd.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, 
                sendData.length, 
                InetAddress.getByName(ADDRESS), 
                PORT);
            perfectLink.sendMessage(sendPacket, msgId);
        }
    }

    
}

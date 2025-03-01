package depchain.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import depchain.client.links.PerfectLink;

public class UDPClient {
    private static final String ADDRESS = "localhost";
    private static final int PORT = 5001;

    public static void main(String[] args) throws IOException {
        byte[] sendData;
        DatagramSocket clientSocket = new DatagramSocket();
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        PerfectLink pf = new PerfectLink(clientSocket, messageQueue);
        Thread messageDeliveringThread = new Thread(() -> deliverMessage(messageQueue));
        messageDeliveringThread.start();
        
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
            pf.sendMessage(sendPacket, msgId);
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

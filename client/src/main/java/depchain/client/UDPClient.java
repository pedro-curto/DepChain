package depchain.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import static depchain.client.utils.Links.sendMessage;

public class UDPClient {

    public static void main(String[] args) throws IOException {
        DatagramPacket sendPacket;
        byte[] sendData;
        // datagram socket (UDP)
        DatagramSocket clientSocket = new DatagramSocket();
        // client timeout is 5 seconds
        clientSocket.setSoTimeout(5000);
        Scanner input = new Scanner(System.in);
        while (true) {
            String cmd = input.nextLine();
            // when we type "QUIT", client goes bye-bye
            if (cmd.equals("QUIT")) {
                clientSocket.close();
                System.exit(1);
            }
            sendData = cmd.getBytes();

            sendMessage(clientSocket, sendData, sendData.length, "127.0.0.1", 5001);
        }
    }
}

package depchain.client.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

public final class Links {

    private static final int MSG_MAX_SIZE = 1024;

    private static final Random rand = new Random();

    private static final ArrayList<String> messagesList = new ArrayList<String>();

    public static void sendMessage(DatagramSocket socket, byte[] data, int size, String address, int port) throws IOException {

        DatagramPacket sendPacket = new DatagramPacket(data, size, InetAddress.getByName(address), port);

        sendMessagePL(socket, sendPacket);
    }

    private static void sendMessageFLL(DatagramSocket socket, DatagramPacket msg) throws IOException {
        socket.send(msg);
        System.out.println("(FLL) message sent");
    }

    private static void sendMessageSL(DatagramSocket socket,DatagramPacket msg) throws IOException {

        int n_sends = 10;

        // while (true)... (after timeout implementation)
        while (n_sends-- > 0) {
            sendMessageFLL(socket, msg);
        }
        System.out.println("(SL) message sent");
    }

    private static void sendMessagePL(DatagramSocket socket, DatagramPacket msg) throws IOException {
        sendMessageSL(socket, msg);
        System.out.println("·(PL) message sent");
    }
    public static DatagramPacket receiveMessages(DatagramSocket sock, boolean loss) throws IOException {

        byte[] recvData = new byte[MSG_MAX_SIZE];
        DatagramPacket recvPacket = new DatagramPacket(recvData, MSG_MAX_SIZE);

        while (true) {
            sock.receive(recvPacket);

            // Simulation of message loss
            if (loss && rand.nextFloat() < 0.3) {
                // Message was lost
                continue;

            }

            // Message was received
            String message = new String(recvPacket.getData(), 0, recvPacket.getLength());
            InetAddress src = recvPacket.getAddress();

            deliverMessageFLL(message, src);
        }
    }

    private static boolean wasMessageAlreadyDelivered(String message) {
        for (String m : messagesList) {
            if (m.equals(message)) {
                return true;
            }
        }
        return false;
    }

    private static void addMessageToDelivered(String message) {
        messagesList.add(message);
    }

    private static void deliverMessageFLL(String message, InetAddress src) {
        System.out.println("(FLL) " + src + " -> " + message);
        deliverMessageSL(message, src);
    }

    private synchronized static void deliverMessageSL(String message, InetAddress src) {
        // Synchronized because of message delivery verification (find alternative)
        System.out.println("(SL) " + src + " -> " + message);

        if (!wasMessageAlreadyDelivered(message)) {
            deliverMessagePL(message, src);
            addMessageToDelivered(message);
        }
    }


    private static void deliverMessagePL(String message, InetAddress src) {
        System.out.println("·(PL) " + src + " -> " + message);

        // TODO
    }
}


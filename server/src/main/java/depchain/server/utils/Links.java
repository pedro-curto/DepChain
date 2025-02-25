package depchain.server.utils;

import java.io.IOException;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.util.Random;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
public final class Links {

    private static final Random rand = new Random();
    public static DatagramPacket receiveMessageWithLoss(DatagramSocket sock, byte[] recvData, int size, boolean loss) throws IOException {

        DatagramPacket recvPacket = new DatagramPacket(recvData, size);
        sock.receive(recvPacket);

        // Simulation of message loss
        if (!loss && rand.nextFloat() > 0.3) {
            // Message was received
            return recvPacket;
        }
        // Message was lost
        return null;
    }

}

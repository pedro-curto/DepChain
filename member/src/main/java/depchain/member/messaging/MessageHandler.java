package depchain.member.messaging;

import java.net.InetAddress;

public interface MessageHandler {
	void handleMessage(String message, InetAddress address, int port);
}

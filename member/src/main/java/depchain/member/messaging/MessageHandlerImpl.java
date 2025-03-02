package depchain.member.messaging;

import com.google.gson.Gson;
import depchain.common.Message;
import depchain.common.SignatureUtils;
import depchain.member.links.PerfectLink;
import depchain.member.membership.Member;
import depchain.member.membership.MemberData;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MessageHandlerImpl implements MessageHandler {

	private final Member myself;
	private final PerfectLink senderLink; // used to broadcast messages
	private final Gson gson = new Gson();

	public MessageHandlerImpl(Member myself, PerfectLink senderLink) {
		this.myself = myself;
		this.senderLink = senderLink;
	}

	@Override
	public void handleMessage(String jsonMessage, InetAddress senderAddress, int senderPort) {
		Message msg = gson.fromJson(jsonMessage, Message.class);
		System.out.println("[Handler] Received message from " + msg.getSenderId() + ": " + msg.getMsgContent());

		// if message comes from a client and i'm the leader, sign and broadcast
		if ("client".equalsIgnoreCase(msg.getSenderId()) && myself.isLeader()) {
			System.out.println("[Handler] I am the leader and got a client message. Signing and broadcasting...");
			try {
				// sign message and broadcast it
				// TODO -> check what we're supposed to sign
				String signature = SignatureUtils.makeDS(msg.getMsgContent(), myself.getKeyPair().getPrivate());
				msg.setSignature(signature);
				String signedJson = gson.toJson(msg);
				broadcastMessage(signedJson);
			} catch (Exception e) {
				System.err.println("[Handler] Error signing message: " + e.getMessage());
			}
		} else {
			// if message comes from a member, verify signature
			if (!"client".equalsIgnoreCase(msg.getSenderId())) {
				System.out.println("[Handler] Received message from another member. Verifying signature...");
				try {
					// TODO -> verify signature
					System.out.println("[Handler] To Do: Implement signature verification!");
				} catch (Exception e) {
					System.err.println("[Handler] Error verifying signature: " + e.getMessage());
				}
			}
		}
	}

	private void broadcastMessage(String jsonMessage) {
		for (MemberData member : myself.getMembers()) {
			// TODO skip broadcasting to self (acho que isto não está bem)
			if (member.getMemberName().equalsIgnoreCase(myself.getMembers().get(0).getMemberName())) {
				continue;
			}
			try {
				InetAddress address = InetAddress.getByName(member.getAddress());
				byte[] data = jsonMessage.getBytes();
				DatagramPacket packet = new DatagramPacket(data, data.length, address, member.getPort());
				// use PerfectLink's send method to send
				senderLink.sendMessage(packet, myself.getMemberName());
				System.out.println("[Handler] Broadcasted message to " + member.getMemberName());
			} catch (UnknownHostException e) {
				System.err.println("[Handler] Unknown host for " + member.getMemberName() + ": " + e.getMessage());
			}
		}
	}
}

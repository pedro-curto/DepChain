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
	private final PerfectLink perfectLink; // used to broadcast messages
	private final Gson gson = new Gson();

	public MessageHandlerImpl(Member myself, PerfectLink perfectLink) {
		this.myself = myself;
		this.perfectLink = perfectLink;
	}

	@Override
	public void handleMessage(String jsonMessage, InetAddress senderAddress, int senderPort) {
		// messages that we can receive are: from a client, a broadcast from another member, or an ack from another member
		Message msg = gson.fromJson(jsonMessage, Message.class);
		System.out.println("[Handler] Received message from " + msg.getSenderId() + ": " + msg.getMsgContent());
		switch (msg.getMsgType()) {
			case "client":
				handleClientMessage(msg, senderAddress, senderPort);
				break;
			case "broadcast":
				handleBroadcastMessage(msg);
				break;
			case "clientAck":
				handleClientAck(msg);
				break;
			default:
				System.err.println("[Handler] Unknown message type: " + msg.getMsgType());
		}
	}

	private void handleClientAck(Message msg) {
		System.out.println("[Handler] TO DO: IMPLEMENT HANDLECLIENTACK");
	}

	private void handleBroadcastMessage(Message msg) {
		System.out.println("[Handler] TO DO: IMPLEMENT HANDLEBROADCASTMESSAGE");

	}

	private void handleClientMessage(Message msg, InetAddress senderAddress, int senderPort) {
		// if message comes from a client, we:
		// (client) -> send an ack back
		// (other members) -> sign the message, broadcast it
		System.out.println("[Handler] Received message from client: " + msg.getMsgContent());
		if (!myself.isLeader()) {
			System.out.println("[Handler] I am not the leader, ignoring client message");
			return;
		}
		try {
			// first, send ack back to client
			Message ack = new Message(msg.getMsgId(), myself.getMemberName(), null, null, "clientAck");
			byte[] ackData = gson.toJson(ack).getBytes();
			DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, senderAddress, senderPort);
			System.out.println("[Handler] Sending ack back to client: " + ack);
			perfectLink.sendAckToClient(ackPacket, msg.getMsgId());
			// after, sign message and broadcast it to other members
			// TODO -> check what we're supposed to sign
			String signature = SignatureUtils.makeDS(msg.getMsgContent(), myself.getKeyPair().getPrivate());
			msg.setSignature(signature);
			msg.setMsgType("broadcast");
			String signedJson = gson.toJson(msg);
			//broadcastMessage(signedJson);
		} catch (Exception e) {
			System.err.println("[Handler] Error signing message: " + e.getMessage());
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
				perfectLink.sendMessage(packet, myself.getMemberName());
				System.out.println("[Handler] Broadcasted message to " + member.getMemberName());
			} catch (UnknownHostException e) {
				System.err.println("[Handler] Unknown host for " + member.getMemberName() + ": " + e.getMessage());
			}
		}
	}
}

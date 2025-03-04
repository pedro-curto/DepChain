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
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;

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
			default:
				System.err.println("[Handler] Unknown message type: " + msg.getMsgType());
		}
	}

	private void handleBroadcastMessage(Message msg) {
		System.out.println("[Handler] Received message: " + msg);
		// check digital signature
		String dataToVerify = msg.getMsgId() + msg.getSenderId() + msg.getMsgContent() + msg.getMsgType();
		System.out.println("[Handler] Data to verify: " + dataToVerify);
		boolean verified;
		try {
			// gets public key of the member that sent the message
			PublicKey senderPublicKey = SignatureUtils.getMemberPublicKey(msg.getSenderId());
			System.out.println("[Handler] Public key of sender: " + Base64.getEncoder().encodeToString(senderPublicKey.getEncoded()));
			verified = SignatureUtils.verifyDS(msg.getSignature(), dataToVerify, senderPublicKey);
		} catch (Exception e) {
			System.err.println("[Handler] Error verifying signature: " + e.getMessage());
			return;
		}
		if (!verified) {
			System.err.println("[Handler] Invalid signature, ignoring message");
			return;
		} else {
			System.out.println("[Handler] Signature verified");
		}
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
			Message ack = new Message(msg.getMsgId(), myself.getMemberName(), null, null, "clientAck", null);
			byte[] ackData = gson.toJson(ack).getBytes();
			DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, senderAddress, senderPort);
			System.out.println("[Handler] Sending ack back to client: " + ack);
			perfectLink.sendAckToClient(ackPacket, msg.getMsgId());
			// after, sign message and broadcast it to other members
			// TODO -> check what we're supposed to sign
			String msgId = UUID.randomUUID().toString();
			String dataToSign = msgId + myself.getMemberName() + msg.getMsgContent() + "broadcast";
			System.out.println("[Handler] Data to sign: " + dataToSign);
			String signature = SignatureUtils.makeDS(dataToSign, myself.getKeyPair().getPrivate());
			Message leaderMessage = new Message(msgId, myself.getMemberName(), msg.getMsgContent(), signature, "broadcast", msg.getRequest());
			byte[] data = gson.toJson(leaderMessage).getBytes();
			broadcastMessage(data);
		} catch (Exception e) {
			System.err.println("[Handler] Error signing message: " + e.getMessage());
		}
	}

	private void broadcastMessage(byte[] data) {
		for (MemberData member : myself.getMembers()) {
			// TODO skip broadcasting to self (acho que isto não está bem)
			if (member.getMemberName().equalsIgnoreCase(myself.getMemberName())) {
				continue;
			}
			try {
				InetAddress address = InetAddress.getByName(member.getAddress());
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

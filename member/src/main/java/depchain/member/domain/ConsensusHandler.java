package depchain.member.domain;

import depchain.common.DCLogger;
import depchain.common.domain.ConsensusState;
import depchain.common.domain.Entity;
import depchain.common.messaging.*;
import depchain.member.state.BlockchainState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class ConsensusHandler implements Runnable {
	private BlockingQueue<Message> messageQueue;
	private Member member;
	private Map<Integer, Entity> clients;
	private Map<Integer, Entity> members;
	private ConsensusState state;
	private BlockchainState blockchainState;
	private final DCLogger dcLogger = new DCLogger(ConsensusHandler.class, true);

	public ConsensusHandler(BlockingQueue<Message> messageQueue,
							Member member,
							ConsensusState state,
							BlockchainState blockchainState,
							List<Entity> members,
							List<Entity> clients)
	{
		this.messageQueue = messageQueue;
		this.member = member;
		this.state = state;
		this.blockchainState = blockchainState;
		this.clients = clients.stream().collect(Collectors.toMap(Entity::getPort, client -> client));
		this.members = members.stream().collect(Collectors.toMap(Entity::getPort, m -> m));
	}

	@Override
	public void run() {
		dcLogger.log("Starting consensus handler");
		dcLogger.log("My initial state: " + state);
		while (true) {
			try {
				Message message = messageQueue.take();
				dcLogger.log("Received message of type: " + message.getType());

				switch (message.getType()) {
					case APPEND:
						// TODO
						// call init() or propose()?
						// then they send READ messages to all members
						AppendMessage appendMessage = (AppendMessage) message;
						dcLogger.log("Received append message: " + appendMessage);
						handleAppend(appendMessage);
						break;
					case READ:
						ReadMessage readMessage = (ReadMessage) message;
						handleRead(readMessage);
						break;
					case STATE:
						StateMessage stateMessage = (StateMessage) message;
						handleState(stateMessage);
						break;
					case COLLECTED:
						CollectedMessage collectedMessage = (CollectedMessage) message;
						handleCollected(collectedMessage);
						break;
					case WRITE:
						WriteMessage writeMessage = (WriteMessage) message;
						handleWrite(writeMessage);
						break;
					case ACCEPT:
						AcceptMessage acceptMessage = (AcceptMessage) message;
						handleAccept(acceptMessage);
						break;
					default:
						dcLogger.log("Unknown message type");
				}
			} catch (InterruptedException e) {
				dcLogger.error("Error while processing message: " + e.getMessage());
			}
		}
	}

	private void handleAppend(AppendMessage appendMessage) {
		// sends a READ message to all members
	}

	private void handleRead(ReadMessage readMessage) {

	}

	private void handleAccept(AcceptMessage acceptMessage) {
	}

	private void handleWrite(WriteMessage writeMessage) {
		
	}

	private void handleCollected(CollectedMessage collectedMessage) {
		
	}

	private void handleState(StateMessage stateMessage) {
		
	}


}

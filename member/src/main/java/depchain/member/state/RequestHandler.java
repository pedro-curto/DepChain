package depchain.member.state;

import depchain.common.messaging.*;

public class RequestHandler {

    private final BlockchainState blockchainState;

    public RequestHandler(BlockchainState blockchainState) {
        this.blockchainState = blockchainState;
    }

    public void handleAppend(String content) {
        blockchainState.appendString(content);
    }

    public void handleRead(ReadMessage readMessage) {
        // TODO
    }
    public void handleState(StateMessage stateMessage) {
        // TODO
        // send consensus state
    }
    public void handleCollected(CollectedMessage collectedMessage) {
        // TODO
        // send all states received
    }
    public void handleWrite(WriteMessage writeMessage) {
        // TODO
    }
    public void handleAccept(AcceptMessage acceptMessage) {
        // TODO
    }
}

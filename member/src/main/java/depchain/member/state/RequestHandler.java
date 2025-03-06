package depchain.member.state;

import depchain.common.Request;

public class RequestHandler {

    private final BlockchainState blockchainState;

    public RequestHandler(BlockchainState blockchainState) {
        this.blockchainState = blockchainState;
    }

    public void handleRequest(Request request) {
        System.out.println("[MESSAGE]: " + request);

        switch (request.getType()) {
            case APPEND:
                handleAppend(request.getContent());
                break;
            default:
                System.err.println("Unknown command");
                break;
        }
    }

    public void handleAppend(String content) {
        blockchainState.appendString(content);
    }
}

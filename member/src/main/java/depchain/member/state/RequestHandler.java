package depchain.member.state;

import depchain.common.Request;

public class RequestHandler {

    private final static String APPEND = "append";
    private final BlockchainState blockchainState;

    public RequestHandler(BlockchainState blockchainState) {
        this.blockchainState = blockchainState;
    }

    public void handleRequest(Request request) {
        System.out.println("[MESSAGE]: " + request);

        switch (request.getAction()) {
            case APPEND:
                handleAppend(request.getContent());
                break;
            default:
                System.out.println("Unkwown action");
                break;
        }
    }

    public void handleAppend(String content) {
        blockchainState.appendString(content);
    }
}

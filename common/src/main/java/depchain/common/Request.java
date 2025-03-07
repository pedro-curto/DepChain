package depchain.common;

import depchain.common.messaging.MessageType;

public class Request {

    private final MessageType type;

    private final String content;

    public Request(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public MessageType getType() {return type;}
}

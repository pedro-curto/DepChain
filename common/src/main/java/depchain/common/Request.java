package depchain.common;

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

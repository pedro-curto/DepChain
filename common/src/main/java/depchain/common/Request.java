package depchain.common;

public class Request {

    private final String action;

    private final String content;

    public Request(String action, String content) {
        this.action = action;
        this.content = content;
    }

    public String getAction() {
        return action;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "<" + action + ", " + content + ">";
    }
}

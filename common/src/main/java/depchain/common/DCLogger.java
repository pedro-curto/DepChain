package depchain.common;

public class DCLogger {
    private final String className;
    private final boolean debug;

    public DCLogger(Class<?> clazz, boolean debug) {
        this.debug = debug;
        this.className = clazz.getSimpleName();
    }

    public void verbose(String message) {
        if (!debug) return;
        System.out.println("[" + className + "] " + message);
    }

    public void log(String message) {
        if (!debug) return;
        System.out.println("[" + className + "] " + message);
    }

    public void error(String message) {
        System.err.println("[ERROR @ " + className + "] " + message);
    }

	public void alert(String receivedReplayedMessage) {
        System.out.println("[ALERT @ " + className + "] " + receivedReplayedMessage);
	}
}
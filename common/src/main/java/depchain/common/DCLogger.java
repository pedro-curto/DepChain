package depchain.common;

public class DCLogger {
    private final String className;
	private final boolean debug;
    
    public DCLogger(Class<?> clazz, boolean debug) {
        this.className = clazz.getSimpleName();
		this.debug = debug;
    }
    
    public void log(String message) {
		if (!debug) return;
        System.out.println("[" + className + "] " + message);
    }

	public void error(String s) {
		System.err.println("[ERROR @ " + className + "] " + s);
	}
}

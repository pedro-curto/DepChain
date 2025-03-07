package depchain.common;

public class DCLogger {
    private final String className;
    
    public DCLogger(Class<?> clazz) {
        this.className = clazz.getSimpleName();
    }
    
    public void log(String message) {
        System.out.println("[" + className + "] " + message);
    }
}

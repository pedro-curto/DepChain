package depchain.common;

import java.io.IOException;
import java.util.logging.*;

public class DCLogger {
    //private final Logger logger;
    private final String className;
    private final boolean debug;

    public DCLogger(Class<?> clazz, boolean debug, String logFilePath) {
        this.debug = debug;
        this.className = clazz.getSimpleName();
        //this.logger = Logger.getLogger(clazz.getName());

        // logs to a specific file (to read during a test)
        //try {
        //    FileHandler fileHandler = new FileHandler(logFilePath);
        //    fileHandler.setFormatter(new SimpleFormatter());
        //    logger.addHandler(fileHandler);
        //    //logger.setUseParentHandlers(false); // no console output
        //} catch (IOException e) {
        //    System.err.println("Failed to initialize logger for " + clazz.getSimpleName() + ": " + e.getMessage());
        //}
    }

    public void log(String message) {
        if (!debug) return;
        //logger.info(message);
        System.out.println("[" + className + "] " + message);
    }

    public void error(String message) {
        //logger.severe(message);
        System.err.println("[ERROR @ " + className + "] " + message);
    }
}
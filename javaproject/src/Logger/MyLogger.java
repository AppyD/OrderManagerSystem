package Logger;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class MyLogger {

    private Logger logger;

    public MyLogger(String className) {
        logger = org.apache.log4j.Logger.getLogger(className);
    }

    public void logException(Exception ex) {
        PropertyConfigurator.configure("resources/log4j.properties");
        logger.debug("ISSUE", ex);
    }

    private void logTrade(Object trade) {
        PropertyConfigurator.configure("resources/log4jv2.properties");
    }
}

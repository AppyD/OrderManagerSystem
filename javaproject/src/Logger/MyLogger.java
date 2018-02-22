package Logger;
import Ref.Instrument;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class MyLogger {

    private Logger logger;

    public MyLogger(String className, Exception ex) {
        logException(className, ex);
    }
    public MyLogger(String className, int tradeID, int clientID, int clientOrderID, int size, Instrument instrument, float price) {
        logTrade(className, tradeID, clientID, clientOrderID, size, instrument, price);
    }

    public void logException(String className, Exception ex) {
        logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4j.properties");
        logger.error("ISSUE", ex);
    }

    private void logTrade(String className, int tradeID, int clientID, int clientOrderID, int size, Instrument instrument, float price) {
        logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4jv2.properties");
        logger.info("TRADE ID: " + tradeID + " -- Client ID: " + clientID + " -- Client Order ID: " + clientOrderID + " -- quantity: " + size + " --  instrument: " + instrument + " --  price: " + price);
    }
}

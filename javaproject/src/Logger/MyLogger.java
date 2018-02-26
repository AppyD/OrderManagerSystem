package Logger;

import Ref.Instrument;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public final class MyLogger {

    private Logger logger;

    public MyLogger(String className, String string) {
        logInfo(className, string);
    }
    public MyLogger(String className, Exception ex) {
        logException(className, ex);
    }
    public MyLogger(String className, int OrderID, int clientID, int clientOrderID, int size, Instrument instrument, double price) {
        logTrade(className, OrderID, clientID, clientOrderID, size, instrument, price);
    }
    public MyLogger(String className, int clientID, int clientOrderID, int sliceSize, Instrument instrument) {
        logSlice(className, clientID, clientOrderID, sliceSize, instrument);
    }
    public MyLogger(String className, int clientID, int clientOrderID, int fillID, int sliceID, int size, double price) {
        logFill(className, clientID, clientOrderID, fillID, sliceID, size, price);
    }

    public void logInfo(String className, String string){
        logger = org.apache.log4j.Logger.getLogger(className);
    public static void logInfo(String className, String string){
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4jv2.properties");
        logger.info(string);
    }

    public static void logException(String className, Exception ex) {
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4j.properties");
        logger.error("ISSUE", ex);
    }

    public static void logOrder(String className, int OrderID, int clientID, int clientOrderID, int size, Instrument instrument, double price) {
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4jv2.properties");
        logger.info("ORDER -- ORDER ID: " + OrderID + " -- Client ID: " + clientID + " -- Client Order ID: " + clientOrderID + " -- Quantity: " + size + " --  Instrument: " + instrument + " --  Price: " + price);
    }

    private void logSlice(String className,              int clientID, int clientOrderID, int sliceSize, Instrument instrument) {
        logger = org.apache.log4j.Logger.getLogger(className);
    public static void logSlice(String className, int clientID, int clientOrderID, int sliceSize, Instrument instrument) {
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4jv2.properties");
        logger.info("SLICE --             -- Client ID: " + clientID + " -- Client Order ID: " + clientOrderID + " -- Slice Size: " + sliceSize + " --  Instrument: " + instrument);
    }

    private void logFill(String className,               int clientID, int clientOrderID, int fillID, int sliceID, int size, double price) {
        logger = org.apache.log4j.Logger.getLogger(className);
    public static  void logFill(String className, int clientID, int clientOrderID, int fillID, int sliceID, int size, double price) {
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4jv2.properties");
        logger.info("FILL  --             -- Client ID: " + clientID + " -- Client Order ID: " + clientOrderID + " -- Fill ID: " + fillID + " -- Slice ID: " + sliceID + " -- Slice Size: " + size + " --  Price: " + price);
    }
}

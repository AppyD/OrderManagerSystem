package Logger;

import Ref.Instrument;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public final class MyLogger {

    private Logger logger;

    public static void logInfo(String className, String string) {
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4jv2.properties");
        logger.info(string);
    }

    public static void logException(String className, String message, Exception ex) {
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4j.properties");
        logger.error(message, ex);
    }

    public static void logOrder(String className, int OrderID, int clientID, int clientOrderID, int size, Instrument instrument, double price, Boolean tradeType) {
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4jv2.properties");
        String tt;
        if (tradeType)
            tt = "BUY";
        else
            tt = "SELL";
        logger.info("ORDER ID: " + OrderID + " -- Client ID: " + clientID + " -- Client Order ID: " + clientOrderID + " -- Quantity: " + size + " --  Instrument: " + instrument + " --  Price: " + price + " -- " + tt);
    }

    public static void logSlice(String className, int clientID, int clientOrderID, int sliceID, int sliceSize, Instrument instrument) {
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4jv2.properties");
        logger.info("SLICE       -- Client ID: " + clientID + " -- Client Order ID: " + clientOrderID + " -- Slice ID: " + sliceID + " -- Slice Size: " + sliceSize + " --  Instrument: " + instrument);
    }

    public static  void logFill(String className, int clientID, int clientOrderID, int sliceID, int size, double price) {
        Logger logger = org.apache.log4j.Logger.getLogger(className);
        PropertyConfigurator.configure("resources/log4jv2.properties");

        logger.info("FILL        -- Client ID: " + clientID + " -- Client Order ID: " + clientOrderID + " -- Slice ID: " + sliceID + " -- Fill Size:  " + size + " --  Price: " + price);
    }
}

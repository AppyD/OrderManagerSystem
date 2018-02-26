package OrderClient;

import java.io.IOException;
import OrderManager.Order;

public interface Client{
	//Outgoing messages
	int sendOrder(String buyOrSell)throws IOException;
	void sendCancel(int id);
	
	//Incoming messages
	void partialFill(Order order);
	void fullyFilled(Order order);
	void cancelled(Order order);
	
	void messageHandler();
}
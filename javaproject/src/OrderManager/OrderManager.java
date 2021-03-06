package OrderManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import Database.Database;
import LiveMarketData.LiveMarketData;
import Logger.MyLogger;
import OrderClient.NewOrderSingle;
import OrderRouter.Router;
import Ref.Instrument;
import TradeScreen.TradeScreen;

public class OrderManager {

	private static LiveMarketData liveMarketData;
	private Map<Integer,Order> orders = new ConcurrentHashMap<>(); // debugger will do this line as it gives state to the object
														 // currently recording the number of new order messages we get. TODO why? use it for more?
	// maybe we can use it for outstandingOrders, and remove any orders when they
	private int id = 0; 								 // debugger will do this line as it gives state to the object
	private Socket[] orderRouters;						 // debugger will skip these lines as they disappear at compile time into 'the object'/stack
	private Socket[] clients;
	private Socket trader;
	private MyLogger logger;

	private Socket connect(InetSocketAddress location) throws InterruptedException {
		boolean connected=false;
		int tryCounter=0;

		while(!connected && tryCounter<600) {
			try{
				Socket s = new Socket(location.getHostName(),location.getPort());
				s.setKeepAlive(true);
				return s;
			} catch (IOException e) {
//				Thread.sleep(1000);
				tryCounter++;
			}
		}
		System.out.println("Failed to connect to " + location.toString());
		return null;
	}

	//@param args the command line arguments
	public OrderManager(InetSocketAddress[] orderRouters,
						InetSocketAddress[] clients,
						InetSocketAddress trader,
						LiveMarketData liveMarketData) throws IOException, ClassNotFoundException, InterruptedException {
		MyLogger.logInfo(OrderManager.class.getName(), "Starting systems..."); // Signifies the start of the log for each run.
		this.liveMarketData = liveMarketData;
		this.trader = connect(trader);
		//for the router connections, copy the input array into our object field.
		//but rather than taking the address we create a socket+ephemeral port and connect it to the address
		this.orderRouters = new Socket[orderRouters.length];
		int i = 0; //need a counter for the the output array
		this.clients = new Socket[clients.length];
		int clientId, routerId;
		Socket client, router;

		for (InetSocketAddress location : orderRouters) {
			this.orderRouters[i] = connect(location);
			i++;
		}

		//repeat for the client connections
		i = 0;
		for (InetSocketAddress location : clients) {
			this.clients[i] = connect(location);
			i++;
		}

		boolean ordersToDo = true;
		//main loop, wait for a message, then process it
		while(ordersToDo){

			//TODO this is pretty cpu intensive, use a more modern polling/interrupt/select approach
			//we want to use the array index as the clientId, so use traditional for loop instead of foreach
			for (clientId=0; clientId<this.clients.length; clientId++) { //check if we have data on any of the sockets
				client = this.clients[clientId];
				if (0 < client.getInputStream().available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages
					ObjectInputStream is = new ObjectInputStream(client.getInputStream()); //create an object inputStream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop
					String method = (String)is.readObject();
					System.out.println(Thread.currentThread().getName() + " calling " + method);
					switch (method) { //determine the type of message and process it
						//call the newOrder message with the clientId and the message (clientMessageId,NewOrderSingle)
						case "newOrderSingle":
							newOrder(clientId, is.readInt(), (NewOrderSingle)is.readObject());
							break;
						default:
							throw new IllegalArgumentException("ERROR: Message type " + method + "is unknown.");
					}
				}
			}

			for (routerId=0; routerId<this.orderRouters.length; routerId++) { //check if we have data on any of the sockets
				router = this.orderRouters[routerId];
				if (0 < router.getInputStream().available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages
					ObjectInputStream is = new ObjectInputStream(router.getInputStream()); //create an object inputStream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop
					String method = (String)is.readObject();
					System.out.println(Thread.currentThread().getName() + " calling " + method);
					switch (method) { //determine the type of message and process it
						case "bestPrice":
							int OrderId = is.readInt();
							int SliceId = is.readInt();
							Order slice = orders.get(OrderId).slices.get(SliceId);
							slice.bestPrices[routerId] = is.readDouble();
							slice.bestPriceCount += 1;
							if(slice.bestPriceCount == slice.bestPrices.length)
								reallyRouteOrder(SliceId, slice);
							break;
						case "newFill":
							newFill(is.readInt(), is.readInt(), is.readInt(), is.readDouble());
							break;
					}
				}
			}

			if (this.trader != null && 0 < this.trader.getInputStream().available()) {
				ObjectInputStream is = new ObjectInputStream(this.trader.getInputStream());
				String method = (String) is.readObject();
				System.out.println(Thread.currentThread().getName() + " calling " + method);
				switch (method) {
					case "acceptOrder":
						acceptOrder(is.readInt());
						break;
					case "sliceOrder":
						sliceOrder(is.readInt(), is.readInt());
						break;
					case "endTrade":
						endTrade(is.readInt(), (Order)is.readObject());
						for (Order order: this.orders.values()){
							if (order.sizeRemaining() != 0){
								break;
							}
						}
						ordersToDo = false;
						break;
				}
			}
		}
		System.out.println("All orders have been processed. System exiting...");
		// close router connections
		i=0;
		for (InetSocketAddress location : orderRouters) {
			this.orderRouters[i].close();
			i++;
		}
		System.out.println("Router connections have been closed.");
		// close client connections
		i = 0;
		for (InetSocketAddress location : clients) {
			this.clients[i].close();
			i++;
		}
		System.out.println("Client connections have been closed.");
		// close trader connection
		this.trader.close();
		System.out.println("Trader connection has closed.");
		System.out.println("System closed successfully. Goodbye.");
		System.exit(0);
	}

	private void endTrade(int id, Order o){
		System.out.println("Order " + o.transactionID + " for client " + o.clientID + " has been completed.");
	}

	private void newOrder(int clientID, int clientOrderID, NewOrderSingle nos) throws IOException {
		orders.put(id, new Order(clientID, clientOrderID, nos.instrument, nos.size, nos.price));
		MyLogger.logOrder(OrderManager.class.getName(), id, clientID, clientOrderID, nos.size, nos.instrument, nos.price);
		//send a message to the client with 39=A; //OrdStatus is Fix 39, 'A' is 'Pending New'
		ObjectOutputStream os = new ObjectOutputStream(clients[clientID].getOutputStream());
		//newOrderSingle acknowledgement;  //clientOrderID =11 (Fix 11?)
		os.writeObject("11=" + clientOrderID + "; 35=A; 39=A;");
		os.flush();
		sendOrderToTrader(id,orders.get(id),TradeScreen.api.newOrder);
		//send the new order to the trading screen
		//don't do anything else with the order, as we are simulating high touch orders and so need to wait for the trader to accept the order
		id++;
	}

	private void sendOrderToTrader(int id, Order o, Object method) throws IOException {
		ObjectOutputStream ost = new ObjectOutputStream(trader.getOutputStream());
		ost.writeObject(method);
		ost.writeInt(id);
		ost.writeObject(o);
		ost.flush();
	}

	public void acceptOrder(int id) throws IOException {
		Order o = orders.get(id);
		if (o.OrdStatus!='A') { //Pending New
			System.out.println("ERROR: accepting order that has already been accepted");
			return;
		}
		o.OrdStatus = '0'; //New
		ObjectOutputStream os = new ObjectOutputStream(clients[(int) o.clientID].getOutputStream());
		System.out.println("New order accepted: clientOrderID=" + id);  //newOrderSingle acknowledgement;
		os.writeObject("11=" + o.clientOrderID + "; 35=A; 39=0");  //ClientOrderID =11
		os.flush();
		price(id, o);
	}

	public void sliceOrder(int id, int sliceSize) throws IOException {
		Order o = orders.get(id);
		//slice the order. We have to check this is a valid size.
		//Order has a list of slices, and a list of fills, each slice is a child order and each fill is associated with either a child order or the original order
		if(sliceSize > o.sizeRemaining() - o.totalSizeOfSlices()){
			System.out.println("ERROR: sliceSize is bigger than remaining size to be filled on the order");
			return;
		}
		int sliceId = o.newSlice(sliceSize);
		Order slice = o.slices.get(sliceId);
		internalCross(id, slice);
		int sizeRemaining = o.slices.get(sliceId).sizeRemaining();
		if (sizeRemaining > 0) {
			routeOrder(id, sliceId, sizeRemaining, slice);
		}
	}

	private void internalCross(int id, Order o) throws IOException {
		for(Map.Entry<Integer, Order> entry : orders.entrySet()){
			if(entry.getKey() == id)
				continue;
			Order matchingOrder = entry.getValue();
			if (!(matchingOrder.instrument.equals(o.instrument) && matchingOrder.initialMarketPrice == o.initialMarketPrice))
				continue;
			//TODO add support here and in Order for limit orders
			int sizeBefore = o.sizeRemaining();
			o.cross(matchingOrder);
			if (sizeBefore != o.sizeRemaining()) {
				sendOrderToTrader(id, o, TradeScreen.api.cross);
			}
		}
	}

	private void cancelOrder() {
		//TODO
	}

	private void newFill(int id, int sliceId, int size, double price) throws IOException {
		Order o = orders.get(id);
		Instrument instrument = o.instrument;
		double salePrice = o.initialMarketPrice;
		if (price < salePrice)   // TODO: Check if this is correct - it is right for a buyer, as they would want to pay <= a maximum price (ie initialMarketPrice).
			salePrice = price;   // Otherwise, use salePrice for the newFill, ie someone completed the fill at the asking price.
		o.slices.get(sliceId).createFill(sliceId, size, salePrice);
		MyLogger.logFill(OrderManager.class.getName(), (int) o.clientID, o.clientOrderID, id, sliceId, size, salePrice);

		if (o.sizeRemaining() == 0) // this is never being run
			Database.write(o);
		sendOrderToTrader(id, o, TradeScreen.api.fill);
	}

	private void routeOrder(int id, int sliceId, int size, Order order) throws IOException {
		for(Socket r : orderRouters){
			ObjectOutputStream os = new ObjectOutputStream(r.getOutputStream());
			os.writeObject(Router.api.priceAtSize);
			os.writeInt(id);
			os.writeInt(sliceId);
			os.writeObject(order.instrument);
			os.writeInt(size); // changed order.sizeRemaining() to size
			os.flush();
		}

		//need to wait for these prices to come back before routing
		order.bestPrices = new double[orderRouters.length];
		order.bestPriceCount = 0;
	}

	private void reallyRouteOrder(int sliceId, Order o) throws IOException {
		//TODO this assumes we are buying rather than selling
		int minIndex = 0;
		double min = o.bestPrices[0];
		for (int i=1; i<o.bestPrices.length; i++) {
			if (min > o.bestPrices[i]) {
				minIndex = i;
				min = o.bestPrices[i];
			}
		}
		ObjectOutputStream os = new ObjectOutputStream(orderRouters[minIndex].getOutputStream());
		os.writeObject(Router.api.routeOrder);
		os.writeInt((int) o.transactionID);
		os.writeInt(sliceId);
		os.writeInt(o.sizeRemaining());
		os.writeObject(o.instrument);
		os.flush();
	}

	private void sendCancel(Order order, Router orderRouter) {
		//orderRouter.sendCancel(order);
		//order.orderRouter.writeObject(order);
	}

	private void price(int id, Order o) throws IOException {
//		liveMarketData.setPrice(o); // doesn't do anything right now, as interface is not yet implemented // Harry: I think it might be changing the initialMarketPrice.
		sendOrderToTrader(id, o, TradeScreen.api.price);
	}

	private void printOrders() {
		System.out.println("Orders so far: " + orders.entrySet());
	}

}
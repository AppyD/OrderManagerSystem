package OrderManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import LiveMarketData.LiveMarketData;
import Logger.MyLogger;
import OrderClient.NewOrderSingle;
import OrderRouter.Router;
import TradeScreen.TradeScreen;

public class OrderManager {

	private static final Random RANDOM_NUM_GENERATOR = new Random();
	private static LiveMarketData liveMarketData;
	private Map<Integer,Order> orders = new HashMap<>(); // debugger will do this line as it gives state to the object
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
		logger = new MyLogger(OrderManager.class.getName(), "Starting systems..."); // Signifies the start of the log for each run.
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

		//main loop, wait for a message, then process it
		while(true){

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
							throw new IllegalArgumentException("ERROR: Message type " + method + " from client is unknown.");
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
							newFill(is.readInt(), is.readInt(), is.readInt());
							break;
					}
				}
			}

			if (this.trader != null && 0 < this.trader.getInputStream().available()) {
				ObjectInputStream is = new ObjectInputStream(this.trader.getInputStream());
				String method = (String) is.readObject(); //TODO: the exception is thrown here - no more data coming from the stream...
				System.out.println(Thread.currentThread().getName() + " calling " + method);
				switch (method) {
					case "acceptOrder":
						acceptOrder(is.readInt());
						break;
					case "sliceOrder":
						sliceOrder(is.readInt(), is.readInt());
						break;
//					case "fill":
//						newFill
				}
			}
		}
	}

	private void newOrder(int clientID, int clientOrderID, NewOrderSingle nos) throws IOException {
		orders.put(id, new Order(clientID, clientOrderID, nos.instrument, nos.size, nos.price));
		logger = new MyLogger(OrderManager.class.getName(), id, clientID, clientOrderID, nos.size, nos.instrument, nos.price);
		//send a message to the client with 39=A; //OrdStatus is Fix 39, 'A' is 'Pending New'
		ObjectOutputStream os = new ObjectOutputStream(clients[clientID].getOutputStream());
		//newOrderSingle acknowledgement;  //clientOrderID =11 (Fix 11?)
		os.writeObject("11=" + clientOrderID + "; 35=A; 39=A;");
		os.flush();
		sendOrderToTrader(id, orders.get(id), TradeScreen.api.newOrder);
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
			routeOrder(id, sliceId, sizeRemaining, slice); // problem with changing this to a while loop
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

	private void newFill(int id, int sliceId, int fillSize) throws IOException {
		Order o = orders.get(id);
		// Calculate a sale price based on a random market fluctuation of up to 3% plus or minus from the initial market value.
		double salePrice = o.initialMarketPrice;
		double marketVariation = (salePrice*3/100)*RANDOM_NUM_GENERATOR.nextDouble(); // CHANGE: currently set to a variance of within 3% of the initial market value.
		if (RANDOM_NUM_GENERATOR.nextInt()%2 == 0)
			salePrice -= marketVariation;

		o.slices.get(sliceId).createFill(sliceId, fillSize, salePrice);
		MyLogger logger = new MyLogger(OrderManager.class.getName(), (int) o.clientID, o.clientOrderID, id, sliceId, fillSize, salePrice);

		if (o.sizeRemaining() == 0) // this is never being run
			logger.logInfo(OrderManager.class.getName(), "Order ID " + id + " has been fully filled.");
		sendOrderToTrader(id, o, TradeScreen.api.fill);
	}

	private void routeOrder(int id, int sliceId, int size, Order order) throws IOException {
		for(Socket r : orderRouters){
			ObjectOutputStream os = new ObjectOutputStream(r.getOutputStream());
			os.writeObject(Router.api.priceAtSize);
			os.writeInt(id);
			os.writeInt(sliceId);
			os.writeObject(order.instrument);
			os.writeInt(order.sizeRemaining());
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
		os.writeInt((int) o.orderID);
		os.writeInt(sliceId);
		os.writeInt(o.sizeRemaining()); // total size remaining of order, not slices in order
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
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import LiveMarketData.LiveMarketData;
import OrderManager.OrderManager;

public class Main{

	public static void main(String[] args) throws IOException{

		//Create and start 2 sample clients
		MockClient c1 = new MockClient("Client 1",2000);
		MockClient c2 = new MockClient("Client 2",2001);
		c1.start();
		c2.start();
		
		//start sample routers
		(new SampleRouter("Router LSE",2010)).start();
		(new SampleRouter("Router BATE",2011)).start();
	
		(new Trader("Trader James",2020)).start();
		//start order manager
		InetSocketAddress[] clients = {new InetSocketAddress("localhost",2000),
									   new InetSocketAddress("localhost",2001)};
		InetSocketAddress[] routers = {new InetSocketAddress("localhost",2010),
		                     		   new InetSocketAddress("localhost",2011)};
		InetSocketAddress   trader  =  new InetSocketAddress("localhost",2020);
		LiveMarketData liveMarketData = new SampleLiveMarketData();
		(new MockOM("Order Manager", routers, clients, trader, liveMarketData)).start();
	}

}

class MockClient extends Thread {
	int port;

	MockClient(String name,int port) {
		this.port = port;
		this.setName(name);
	}

	public void run() {
		try {
			SampleClient client = new SampleClient(port);
			if (port == 2000) {
				client.sendOrder();
				int id = client.sendOrder();
				//TODO client.sendCancel(orderID);
				client.messageHandler();
			} else {
				if (port == 2004) { // differentiates between clients
					client.sendOrder(); // creates and sends an order
					int id = client.sendOrder();
					//TODO client.sendCancel(orderID);
					client.messageHandler();
				} else {
					client.sendOrder();
					client.messageHandler();
				}
			}
		}
		catch(IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
}

class MockOM extends Thread{
	InetSocketAddress[] clients;
	InetSocketAddress[] routers;
	InetSocketAddress trader;
	LiveMarketData liveMarketData;

	MockOM(String name,InetSocketAddress[] routers,InetSocketAddress[] clients,InetSocketAddress trader,LiveMarketData liveMarketData){
		this.clients=clients;
		this.routers=routers;
		this.trader=trader;
		this.liveMarketData=liveMarketData;
		this.setName(name);
	}

	@Override
	public void run(){
		try{
			//In order to debug constructors you can do F5 F7 F5
			new OrderManager(routers,clients,trader,liveMarketData);
		}catch(IOException | ClassNotFoundException | InterruptedException ex){
			// Set up logging
			final Logger logger = Logger.getLogger(Main.class.getName());
			PropertyConfigurator.configure("resources/log4j.properties");
			logger.debug("SEVERE; null",ex);
		}
	}
}
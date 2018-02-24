import java.util.Random;
import LiveMarketData.LiveMarketData;
import OrderManager.Order;

//TODO this should really be in its own thread, and reading from the actual stock exchanges...
public class SampleLiveMarketData implements LiveMarketData {

	private static final Random RANDOM_NUM_GENERATOR = new Random();

	public void setPrice(Order o) {
		o.initialMarketPrice = 199*RANDOM_NUM_GENERATOR.nextDouble();
	}

	public double getPrice() {
		return 199*RANDOM_NUM_GENERATOR.nextDouble();
	}
}

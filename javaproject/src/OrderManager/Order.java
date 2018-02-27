package OrderManager;

import java.io.Serializable;
import java.util.ArrayList;
import Logger.MyLogger;
import Ref.Instrument;

public class Order implements Serializable {
	public long orderID;
	public int clientOrderID;
    long clientID;
//	short orderRouter;
	public int size;
//	double price;           // For recording the price the client wishes to pay/receive for this order. TODO: Harry says implement this.
//	public int side;		// 1=Buy, 2=Sell.
    public Instrument instrument;
	double[] bestPrices;
	int bestPriceCount;
    public double initialMarketPrice;
	public ArrayList<Order> slices;
    private ArrayList<Fill> fills;
    char OrdStatus = 'A';                 //OrdStatus is Fix 39, 'A' is 'Pending New'
    // Status state

    // The constructor for a new order.
	public Order(long clientId, int ClientOrderID, Instrument instrument, int size, double initialMarketPrice) {
		this.clientID = clientId;
		this.clientOrderID = ClientOrderID;
		this.instrument = instrument;
		this.size = size;
		this.initialMarketPrice = initialMarketPrice;
		slices = new ArrayList<>();
		fills = new ArrayList<>();
	}

    // Calculates the total size of the orders in the 'slices' ArrayList.
	public int totalSizeOfSlices() {
		int totalSizeOfSlices = 0;
		for (Order c : slices)
			totalSizeOfSlices += c.size;
		return totalSizeOfSlices;
	}

	// Adds a new order to the 'slices' ArrayList, and returns the index of this new order within the List.
	public int newSlice(int sliceSize) {
		int sliceID = slices.size();
		slices.add(new Order(clientID, clientOrderID, instrument, sliceSize, initialMarketPrice)); // changed ID to clientID
		MyLogger.logSlice(Order.class.getName(), (int) clientID, clientOrderID, sliceID, sliceSize, instrument);
		return slices.size() - 1;
	}

	// Returns the total size of orders within both 'fills' and 'slices' ArrayLists.
	private int sizeFilled() {
		int filledSoFar = 0;
		for(Fill f : fills)
			filledSoFar += f.size;
		for(Order c : slices)
			filledSoFar += c.sizeFilled();
		return filledSoFar;
	}

	// Returns the total size remaining for the order to be completed.
	public int sizeRemaining() {
		int sizeFilled = sizeFilled();
		return size - sizeFilled;
	}

	// Not really sure what this is meant to do.
	float price() {
		//TODO this is buggy as it doesn't take account of slices. Let them fix it
		float sum = 0;
		for (Fill fill : fills)
			sum += fill.price;
		return sum/fills.size();
	}

	// Adds a new fill to the 'fills' ArrayList, and updated the OrdStatus of the current order.
	void createFill(long sliceId, int size, double price) {
		fills.add(new Fill(sliceId, size, price));
		if (sizeRemaining() == 0)
			OrdStatus = '2';
		else
			OrdStatus = '1';				// TODO: where does OrdStatus get used?
	}

	// This matches an order with this one, to try to complete as many trades as possible (using the slices ArrayList).
	void cross(Order matchingOrder) {
		//pair slices first and then parent
		for (Order slice : slices) {
			if (slice.sizeRemaining() == 0)
				continue;

			//TODO: could optimise this to not start at the beginning every time
			for (Order matchingSlice : matchingOrder.slices) {
				int sizeM = matchingSlice.sizeRemaining();
				if (sizeM == 0)
					continue;
				int sizeS = slice.sizeRemaining();
				if (sizeS <= sizeM) {
					 slice.createFill(slice.orderID, sizeS, initialMarketPrice);
					 matchingSlice.createFill(matchingSlice.orderID, sizeS, initialMarketPrice);
					 break; // Breaks out with slice.sizeRemaining() = 0
				}
				// Else, (sizeS > sizeM)
				slice.createFill(slice.orderID, sizeM, initialMarketPrice);
				matchingSlice.createFill(matchingSlice.orderID, sizeM, initialMarketPrice);
			}

			// Don't understand when this would ever be the case... or either of the next two big loops for that matter
			int sizeS = slice.sizeRemaining();
			int mParent = matchingOrder.sizeRemaining() - matchingOrder.totalSizeOfSlices();
			if(sizeS>0 && mParent>0) {
				if (sizeS >= mParent) {
					slice.createFill(slice.orderID, sizeS, initialMarketPrice);
					matchingOrder.createFill(matchingOrder.orderID, sizeS, initialMarketPrice);
				} else {
					slice.createFill(slice.orderID, mParent, initialMarketPrice);
					matchingOrder.createFill(matchingOrder.orderID, mParent, initialMarketPrice);
				}
			}

			//no point continuing if we didn't fill this slice, as we must already have fully filled the matchingOrder
			if(slice.sizeRemaining() > 0)
				break;
		}

		if (sizeRemaining() > 0) {
			for (Order matchingSlice : matchingOrder.slices) {
				int sizeM = matchingSlice.sizeRemaining();
				if (sizeM == 0)
					continue;
				int sizeS = sizeRemaining();
				if (sizeS <= sizeM) {
					 createFill(orderID, sizeS, initialMarketPrice);
					 matchingSlice.createFill(matchingSlice.orderID, sizeS, initialMarketPrice);
					 break;
				}
				//if (sizeS > sizeM)
				createFill(orderID, sizeM, initialMarketPrice);
				matchingSlice.createFill(matchingSlice.orderID, sizeM, initialMarketPrice);
			}

			int sizeS = sizeRemaining();
			int mParent = matchingOrder.sizeRemaining() - matchingOrder.totalSizeOfSlices();

			if(sizeS>0 && mParent>0){
				if (sizeS >= mParent) {
					createFill(orderID, sizeS, initialMarketPrice);
					matchingOrder.createFill(matchingOrder.orderID, sizeS, initialMarketPrice);
				} else {
					createFill(orderID, mParent, initialMarketPrice);
					matchingOrder.createFill(matchingOrder.orderID, mParent, initialMarketPrice);
				}
			}
		}
	}

	// Cancels a trade?
	void cancel() {
		//state = cancelled
	}
}

class Basket implements Serializable {

	Order[] orders;

	public Basket(Order[] orders) {
		this.orders = orders;
	}
}

class Fill implements Serializable {
	long sliceId;
	int size;
	double price;

	Fill(long sliceId, int size, double price) {
		this.sliceId = sliceId;
		this.size = size;
		this.price = price;
	}
}

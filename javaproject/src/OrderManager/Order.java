package OrderManager;

import java.io.Serializable;
import java.util.ArrayList;
import Ref.Instrument;

public class Order implements Serializable {
	public long transactionID; // I think this might basically be a sliceID, to uniquely identify each part when an order is sliced up?
	public int clientOrderID;
    long clientID;
//	short orderRouter;
	private int size;
//	double price;           // For recording the price the client wishes to pay/receive for this order. TODO: Harry says implement this.
//	public int side;		// 1=Buy, 2=Sell
    public Instrument instrument;
	double[] bestPrices;
	int bestPriceCount;
    public double initialMarketPrice;
	public ArrayList<Order> slices;
    private ArrayList<Fill> fills;
    char OrdStatus = 'A';                 //OrdStatus is Fix 39, 'A' is 'Pending New'
    // Status state

    // The constructor for a new order.
    public Order(long clientId, int ClientOrderID, Instrument instrument, int size) {
        this.clientID = clientId;
        this.clientOrderID = ClientOrderID;
        this.instrument = instrument;
        this.size = size;
        slices = new ArrayList<>();
        fills = new ArrayList<>();
    } //TODO: Harry says add price to constructor.

    // Calculates the total size of the orders in the 'slices' ArrayList.
	public int totalSizeOfSlices() {
		int totalSizeOfSlices = 0;
		for (Order c : slices)
			totalSizeOfSlices += c.size;
		return totalSizeOfSlices;
	}

	// Adds a new order to the 'slices' ArrayList, and returns the index of this new order within the List.
	public int newSlice(int sliceSize) {
		slices.add(new Order(transactionID, clientOrderID, instrument, sliceSize));
		return slices.size()-1;
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
		return size - sizeFilled();
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
	void createFill(long transactionID, int size, double price) {
		fills.add(new Fill(transactionID, size, price));
		if (sizeRemaining() == 0)
			OrdStatus = '2';
		else
			OrdStatus = '1';
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
					 slice.createFill(slice.transactionID, sizeS, initialMarketPrice);
					 matchingSlice.createFill(matchingSlice.transactionID, sizeS, initialMarketPrice);
					 break; // Breaks out with slice.sizeRemaining() = 0
				}
				// Else, (sizeS > sizeM)
				slice.createFill(slice.transactionID, sizeM, initialMarketPrice);
				matchingSlice.createFill(matchingSlice.transactionID, sizeM, initialMarketPrice);
			}

			// Don't understand when this would ever be the case... or either of the next two big loops for that matter
			int sizeS = slice.sizeRemaining();
			int mParent = matchingOrder.sizeRemaining() - matchingOrder.totalSizeOfSlices();
			if(sizeS>0 && mParent>0) {
				if (sizeS >= mParent) {
					slice.createFill(slice.transactionID, sizeS, initialMarketPrice);
					matchingOrder.createFill(matchingOrder.transactionID, sizeS, initialMarketPrice);
				} else {
					slice.createFill(slice.transactionID, mParent, initialMarketPrice);
					matchingOrder.createFill(matchingOrder.transactionID, mParent, initialMarketPrice);
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
					 createFill(transactionID, sizeS, initialMarketPrice);
					 matchingSlice.createFill(matchingSlice.transactionID, sizeS, initialMarketPrice);
					 break;
				}
				//if (sizeS > sizeM)
				createFill(transactionID, sizeM, initialMarketPrice);
				matchingSlice.createFill(matchingSlice.transactionID, sizeM, initialMarketPrice);
			}

			int sizeS = sizeRemaining();
			int mParent = matchingOrder.sizeRemaining() - matchingOrder.totalSizeOfSlices();

			if(sizeS>0 && mParent>0){
				if (sizeS >= mParent) {
					createFill(transactionID, sizeS, initialMarketPrice);
					matchingOrder.createFill(matchingOrder.transactionID, sizeS, initialMarketPrice);
				} else {
					createFill(transactionID, mParent, initialMarketPrice);
					matchingOrder.createFill(matchingOrder.transactionID, mParent, initialMarketPrice);
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
	long transactionID;
	int size;
	double price;

	Fill(long transactionID, int size, double price) {
		this.transactionID = transactionID;
		this.size = size;
		this.price = price;
	}
}

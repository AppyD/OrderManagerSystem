package OrderClient;

import java.io.Serializable;
import Ref.Instrument;

public class NewOrderSingle implements Serializable {
	public int size;
	public double price;
	public Instrument instrument;

	public NewOrderSingle(int size, double price, Instrument instrument) {
		this.size = size;
		this.price = price;
		this.instrument = instrument;
	}
}
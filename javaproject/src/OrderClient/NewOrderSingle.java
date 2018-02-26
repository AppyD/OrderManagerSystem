package OrderClient;

import java.io.Serializable;
import Ref.Instrument;
import com.sun.org.apache.xpath.internal.operations.Bool;

public class NewOrderSingle implements Serializable {
	public int size;
	public double price;
	public Instrument instrument;
	public Boolean tradeType;

	public NewOrderSingle(int size, double price, Instrument instrument, Boolean tradeType) {
		this.size = size;
		this.price = price;
		this.instrument = instrument;
		this.tradeType = tradeType;
	}
}
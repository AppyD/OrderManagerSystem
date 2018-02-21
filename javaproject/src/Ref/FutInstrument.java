package Ref;

import java.util.Date;

public class FutInstrument extends Instrument {
    Date expiry;
    Instrument underlier;

    public FutInstrument(Ric ric){
            super(ric);
        }
}

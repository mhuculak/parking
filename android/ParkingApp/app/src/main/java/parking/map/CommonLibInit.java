package parking.map;

import android.util.Log;
import android.content.Context;
import java.util.Locale;

public class CommonLibInit {

	protected static final String TAG = "location-updates-sample";

	public CommonLibInit(Context context, Locale locale) {
		ReverseGeocoder rg = new ReverseGeocoder(context, locale);
		Address.setReverseGeocoder(rg);
		Place.setReverseGeocoder(rg);
		MyGeocoder gc = new MyGeocoder(context, locale);
		Position.setGeocoder(gc);
		Place.setGeocoder(gc);
	}
}
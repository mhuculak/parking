package parking.map;

import android.content.Context;
import android.location.Geocoder;
import android.location.Address;
import java.util.Locale;
import java.util.List;
import android.util.Log;

public class ReverseGeocoder implements ReverseGeocoderInterface {

	private Context context;
	private Locale locale;
	protected static final String TAG = "geocode";

	public ReverseGeocoder(Context context, Locale locale) {
		this.context = context;
		this.locale = locale;
	}

	public parking.map.Address reverseGeocode(parking.map.Position p) {
		Log.i(TAG, "reverse geocode "+p.toString());
		try {
			Geocoder geocoder;
			List<android.location.Address> addresses;
			geocoder = new Geocoder(context, locale);
			Log.i(TAG, "reverse geocode 1 result...");
			addresses = geocoder.getFromLocation(p.getLatitude(), p.getLongitude(), 1);
			Log.i(TAG, "reverse geocode got "+addresses.size()+" results");
			int nearest = 0;
			for ( int i=0 ; i<addresses.get(nearest).getMaxAddressLineIndex() ; i++) {
				Log.i(TAG, addresses.get(nearest).getAddressLine(i));
			}
			if (nearest < addresses.size()) {
				return getAddress(addresses.get(nearest));
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			Log.i(TAG, "Caught exception "+ex+" while attempting reverse geocode");
		}
		return null;
	}

	public static parking.map.Address getAddress(android.location.Address address) {
		parking.map.Address addr = new parking.map.Address();
		Log.i(TAG, "set street number to " + address.getPremises());
		addr.setStreetNumber(address.getPremises());
		Log.i(TAG, "set street name to " + address.getThoroughfare());
		addr.setStreetName(address.getThoroughfare());
		Log.i(TAG, "set town to " + address.getSubLocality());
		addr.setTown(address.getSubLocality());
		Log.i(TAG, "set city to " + address.getLocality());
		addr.setCity(address.getLocality());
		Log.i(TAG, "set state/prov to " + address.getAdminArea());
		addr.setStateProv(address.getAdminArea());
		Log.i(TAG, "set admin region to " + address.getSubAdminArea());
		addr.setAdminRegion(address.getSubAdminArea());
		Log.i(TAG, "set country to " + address.getCountryName());
		addr.setCountry(address.getCountryName());
		Log.i(TAG, "set postal to " + address.getPostalCode());
		addr.setPostalCodeZIP(address.getPostalCode());
		return addr;
	}


}
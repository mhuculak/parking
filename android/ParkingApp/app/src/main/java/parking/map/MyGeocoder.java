package parking.map;

import android.content.Context;
import android.location.Geocoder;
import android.util.Log;

import java.util.List;
import java.util.Locale;

public class MyGeocoder implements parking.map.GeocoderInterface {

	private Context context;
	private Locale locale;
	protected static final String TAG = "geocode";

	public MyGeocoder(Context context, Locale locale) {
		this.context = context;
		this.locale = locale;
	}

	public parking.map.Position geocode(String place, parking.map.Position orig, double maxDist) {
		Geocoder geocoder = new Geocoder(context, locale);
		List<android.location.Address> addresses;
		try {
			addresses = geocoder.getFromLocationName(place, 10);
			int nearest = getNearest(addresses, orig);
			if (nearest < addresses.size()) {
				parking.map.Position p = new parking.map.Position(addresses.get(nearest).getLatitude(), addresses.get(nearest).getLongitude());
				if (maxDist == 0.0 || orig == null || Position.getDistanceKm(p, orig) < maxDist) {
					return p;
				}
			}
		}
		catch (Exception ex) {
			Log.i(TAG, "Caught exception "+ex+" while attempting to get location from name");
		}
		return null;
    }

	public Place findPlace(String place, Position orig, double maxDist) {
		Geocoder geocoder = new Geocoder(context, locale);
		List<android.location.Address> addresses;
		try {
			addresses = geocoder.getFromLocationName(place, 10);
			int nearest = getNearest(addresses, orig);
			if (nearest < addresses.size()) {
				parking.map.Position p = new parking.map.Position(addresses.get(nearest).getLatitude(), addresses.get(nearest).getLongitude());
				if (maxDist == 0.0 || orig == null || Position.getDistanceKm(p, orig) < maxDist) {
					return new Place(ReverseGeocoder.getAddress(addresses.get(nearest)), p);
				}
			}
		}
		catch (Exception ex) {
			Log.i(TAG, "Caught exception "+ex+" while attempting to get location from name");
		}
		return null;
	}


	private int getNearest(List<android.location.Address> addresses, parking.map.Position orig) {
		if (orig == null) {
			Log.i(TAG, "return 0 because orig is null");
			return 0;
		}
		int nearest = 0;
		double minDist = -1;
		Log.i(TAG, "find nearest of "+addresses.size());
		for ( int i=0 ; i<addresses.size() ; i++) {
			Position p = new Position(addresses.get(i).getLatitude(), addresses.get(i).getLongitude());
			double dist = Position.getDistanceKm(p, orig);
			for ( int j=0 ; j<addresses.get(i).getMaxAddressLineIndex() ; j++) {
				Log.i(TAG, addresses.get(i).getAddressLine(j));
			}
			Log.i(TAG, "distance is "+dist);
			if (minDist<0 || dist<minDist) {
				minDist = dist;
				nearest = i;
				Log.i(TAG, "nearest is "+minDist+" index = "+i);
			}
		}
		return nearest;
	}
}
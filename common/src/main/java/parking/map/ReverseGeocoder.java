package parking.map;

import com.google.maps.GeoApiContext;
import com.google.maps.model.LatLng;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

import com.google.code.geocoder.model.GeocoderResult;
import com.google.code.geocoder.model.GeocoderAddressComponent;

import java.util.List;

public class ReverseGeocoder implements ReverseGeocoderInterface {
	
	public Address reverseGeocode(Position p) {
		if (p != null) {
			try {
				GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyDni-ZQemF7eA1P-A76acHMF2tREyFM3HI");
				LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
				GeocodingResult[] results = GeocodingApi.newRequest(context).latlng(latLng).await();
				System.out.println(results[0].formattedAddress);			
				return new Address(results[0].addressComponents);	
			}
			catch (Exception ex) {
				System.out.println("Caught exception while reverse geocode "+ex.toString());
				ex.printStackTrace();
			}	
		}
		return null;
	}

	public Address getAddress(GeocoderResult result) {
//		List<GeocoderAddressComponent> components = result.getAddressComponents();
//		return new Address(components);
		return null;
	}

}
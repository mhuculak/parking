package parking.map;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder; 
import com.google.code.geocoder.model.GeocodeResponse; 
import com.google.code.geocoder.model.GeocoderRequest; 
import com.google.code.geocoder.model.GeocoderResult;

import java.util.List;

public class MyGeocoder implements GeocoderInterface {

	public Position geocode(String place, Position orig, double maxDistKm) {
		Geocoder geocoder = new Geocoder();
    	GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(place).getGeocoderRequest();
    	GeocodeResponse geocodeResponse = geocoder.geocode(geocoderRequest);
    	List<GeocoderResult> results = geocodeResponse.getResults();
    	if(results.size() >= 1) {
            int nearest = getNearest(results, orig);
        	double lat = results.get(nearest).getGeometry().getLocation().getLat().doubleValue();
        	double lng = results.get(nearest).getGeometry().getLocation().getLng().doubleValue();
        	Position p = new Position( lat, lng);
            if (maxDistKm == 0 || orig == null && Position.getDistanceKm(p,orig) < maxDistKm) {
                return p;
            }
    	}
    	return null;
    }

    public Place findPlace(String place, Position orig, double maxDistKm) {        
/*
        Geocoder geocoder = new Geocoder();
        GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(place).getGeocoderRequest();
        GeocodeResponse geocodeResponse = geocoder.geocode(geocoderRequest);
        List<GeocoderResult> results = geocodeResponse.getResults();
        if(results.size() >= 1) {
            int nearest = getNearest(results, orig);
            double lat = results.get(nearest).getGeometry().getLocation().getLat().doubleValue();
            double lng = results.get(nearest).getGeometry().getLocation().getLng().doubleValue();
            Position p = new Position( lat, lng);
            if (maxDistKm == 0 || orig == null || Position.getDistanceKm(p,orig) < maxDistKm) {
                Address a = ReverseGeocoder.getAddress(results.get(nearest));
                return new Place( a, p);
            }
        }       
*/
        return null;        
    }

    private int getNearest(List<GeocoderResult> results, Position orig) {
        if (orig == null) {
            return 0;
        }
        int nearest = 0;
        double minDist = -1;
        for ( int i=0 ; i<results.size() ; i++ ) {
            GeocoderResult result = results.get(i);
            double lat = result.getGeometry().getLocation().getLat().doubleValue();
            double lng = result.getGeometry().getLocation().getLng().doubleValue();
            Position p = new Position( lat, lng);
            double dist = Position.getDistanceKm(p, orig);
            if (minDist < 0 || dist < minDist) {
                minDist = dist;
                nearest = i;
            }
        }
        
        return nearest;
    }

}
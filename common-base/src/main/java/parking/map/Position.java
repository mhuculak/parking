package parking.map;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

public class Position implements Serializable {

	private double latitude;
	private double longitude;
	private double accuracy;          // radius in meters where actual position is 68% of the time
	private static GeocoderInterface geocoder;

	public Position() {

	}

	public Position(String value) {
		String[] latlng = value.split("_");
		latitude = latlng[0].equals("null") ? 0.0 : Double.parseDouble(latlng[0]);
		longitude = latlng[1].equals("null") ? 0.0 : Double.parseDouble(latlng[1]);
	}

	public Position(double lat, double lng) {
		latitude = lat;
		longitude = lng;
	}

	public Position(double lat, double lng, double accuracy) {
		latitude = lat;
		longitude = lng;
		this.accuracy = accuracy;
	}

	public Position(String latitude, String longitude) {
		this.latitude = convertToDecimal(latitude);
		this.longitude = convertToDecimal(longitude);
	}

	public Position(JSONObject jObj) throws JSONException {
		if (!jObj.isNull("latitude")) {
			latitude = jObj.getDouble("latitude");
		}
		if (!jObj.isNull("longitude")) {
			longitude = jObj.getDouble("longitude");
		}
		if (!jObj.isNull("accuracy")) {
			accuracy = jObj.getDouble("accuracy");
		}
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		jObj.put("latitude", latitude);
		jObj.put("longitude", longitude);
		jObj.put("accuracy", accuracy);
		return jObj;
	}

	public void setLatitude(String latitude) {
		this.latitude = convertToDecimal(latitude);
	}

	public void setLongitude(String longitude) {
		this.longitude = convertToDecimal(longitude);
	}

	public static void setGeocoder(GeocoderInterface gc) {
		geocoder = gc;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public String toString() {
		return Double.toString(latitude) + "_" + Double.toString(longitude);
	}

	public String shortString() {
		int lat = (int)getLatitude();
		int lng = (int) getLongitude();
		return lat+"_"+lng;
	}

	public List<String> findNearbyStreets(double angle) {
		double lat = getLatitude();
		double lng = getLongitude();
		Address here = Address.reverseGeocode(this);
		if (here == null) {
			return null;
		}
		List<Position> posList = new ArrayList<Position>(); 
		posList.add(new Position(lat+angle, lng));
		posList.add(new Position(lat-angle, lng));
		posList.add(new Position(lat, lng-angle));
		posList.add(new Position(lat, lng+angle));
		Map<String, Boolean> nearby = new HashMap<String, Boolean>();
		for ( Position p : posList) {
			String nearestStreet = findNearest(p, this, here.getStreetName());
			if (nearestStreet != null && nearby.get(nearestStreet) == null) {
				nearby.put( nearestStreet, true);
			}
		}
		return new ArrayList<String>(nearby.keySet());

	}

	private String findNearest(Position p, Position dest, String fromStreet) {
		final double step = 0.2;
		String street = Address.reverseGeocode(p).getStreetName();
		String nearest = street;
		while (!street.equals(fromStreet)) {
			p = p.moveTowards(dest, step);
			nearest = street;
			street = Address.reverseGeocode(p).getStreetName();			
		}
		if (nearest.equals(fromStreet)) {
			return null;
		}
		else {
			return nearest;
		}
	}

	private Position moveTowards(Position dest, double step) {
		double dlat= dest.getLatitude() - this.getLatitude();
		double dlong = dest.getLongitude() - this.getLongitude();
		return new Position( this.getLatitude() + dlat*step, this.getLongitude() + dlong*step)	;	
	}

	// Longitude -73.0° 38.0' 58.600000000008095"
	// Latitude 45.0° 26.0' 54.52999999999918"
	private static double convertToDecimal(String val) {
		final String DEGREE  = "\u00b0";
//		System.out.println("convert " + val);
		if (val.contains(DEGREE)) {
			String[] dms = val.split(" ");
			double dval;
			String deg = dms[0].replaceAll(DEGREE,"");
			dval = Double.parseDouble(deg);
			String min = dms[1].replaceAll("'","");
			String sec = dms[2].replaceAll("\"","");
			double sval = Double.parseDouble(sec);
			double mval = Double.parseDouble(min);
			mval += sval/60;
			if (dval > 0.0) {
				dval += mval/60;
			}
			else {
				dval -= mval/60;
			}
			return dval;
		}
		else {
//			System.out.println("return original " + val);
			return Double.parseDouble(val);
		}
	}

	public static Position getLocation(String place, Position orig, double maxDist) {

		if (place != null && geocoder != null) {
			return geocoder.geocode(place, orig, maxDist);
		}
		return null;
	}
	/*
	     FIXME: haversine formula more accurate but still uses mean radius

	     var R = 6371e3; // metres
    var φ1 = lat1.toRadians();
    var φ2 = lat2.toRadians();
    var Δφ = (lat2-lat1).toRadians();
    var Δλ = (lon2-lon1).toRadians();

    var a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
            Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ/2) * Math.sin(Δλ/2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

    var d = R * c;


    According to the post at http://gis.stackexchange.com/questions/20200/how-do-you-compute-the-earths-radius-at-a-given-geodetic-latitude 
    the formula you are looking for is: 

     Rt= SQRT((((a^2cos(t))^2)+((b^2sin(t))^2))/(  ((acos(t))^2)+((b(sin(t))^2)))                 
                 
     Where                 
           Rt = radius of earth at latitude t                 
           a = semi major radius of earth         = 6,378,137 meters 
           b= semi minor radius of earth         = 6,356,752.31420 meters 

    */

	public static double getDistanceKm(Position p1, Position p2) {
//		final double radiusOfEarthKm = 6371;
		double radiusOfEarthKm = (getEarthRadiusMeters(p1)+getEarthRadiusMeters(p2))/2000;
		double lat1 = p1.getLatitude()*Math.PI/180;
		double lat2 = p2.getLatitude()*Math.PI/180;
		double dLat = lat2 - lat1;
		double dLng = (p2.getLongitude() - p1.getLongitude())*Math.PI/180;
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng/2) * Math.sin(dLng/2);
		double angleRadians = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
//		double dlat = Math.abs( p1.getLatitude() - p2.getLatitude());
//		double dlng = Math.abs( p1.getLongitude() - p2.getLongitude());
//		double angleDegrees = Math.sqrt(dlat*dlat + dlng*dlng);
//		double angleRadians = angleDegrees*Math.PI/180;
		return angleRadians * radiusOfEarthKm;
	}

	public static double getEarthRadiusMeters(Position p) {
		final double semiMajorRadiusMeters = 6378137;
		final double semiMinorRadiusMeters = 6356752;
		double latRadians = p.getLatitude()*Math.PI/180;
		double a = semiMajorRadiusMeters;
		double b = semiMinorRadiusMeters;
		double acos = a*Math.cos(latRadians);
		double bsin = b*Math.sin(latRadians);
		return Math.sqrt((a*a*acos*acos + b*b*bsin*bsin)/(acos*acos + bsin*bsin));
	}

	// p2 = p1
	public static Position delta(Position p2, Position p1) {
		double lng1 = p1.getLongitude() < 0 ? 360 + p1.getLongitude() : p1.getLongitude();
		double lng2 = p2.getLongitude() < 0 ? 360 + p2.getLongitude() : p2.getLongitude();
		return new Position( p2.getLatitude() - p1.getLatitude(), lng2 - lng1);
	}

}
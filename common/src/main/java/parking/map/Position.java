package parking.map;

import parking.util.Logger;
import parking.util.LoggingTag;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Position {

	private String latitude;
	private String longitude;

	public Position() {

	}

	public Position(String value) {
		String[] latlng = value.split("_");
		latitude = latlng[0].equals("null") ? null : latlng[0];
		longitude = latlng[1].equals("null") ? null : latlng[1];
	}

	public Position(double lat, double lng) {
		latitude = Double.toString(lat);
		longitude = Double.toString(lng);
	}

	public Position(String latitude, String longitude) {
		this.latitude = convertToDecimal(latitude);
		this.longitude = convertToDecimal(longitude);
	}

	public void setLatitude(String latitude) {
		this.latitude = convertToDecimal(latitude);
	}

	public void setLongitude(String longitude) {
		this.longitude = convertToDecimal(longitude);
	}

	public String getLatitudeAsString() {
		return latitude;
	}

	public String getLongitudeAsString() {
		return longitude;
	}

	public double getLatitude() {
		if (latitude == null) {
			return 0.0;
		}
		return Double.parseDouble(latitude);
	}

	public double getLongitude() {
		if (longitude == null) {
			return 0.0;
		}
		return Double.parseDouble(longitude);
	}

	public boolean isValid() {
		return latitude != null && longitude != null;
	}


	public String toString() {
		return latitude + "_" + longitude;
	}

	public boolean isDefined() {
		if (latitude != null || longitude != null) {
			return true;
		}
		return false;
	}

	public List<String> findNearbyStreets(double angle, Logger logger) {
		double lat = getLatitude();
		double lng = getLongitude();
		Address here = Sign.reverseGeocode(this, logger);
		List<Position> posList = new ArrayList<Position>(); 
		posList.add(new Position(lat+angle, lng));
		posList.add(new Position(lat-angle, lng));
		posList.add(new Position(lat, lng-angle));
		posList.add(new Position(lat, lng+angle));
		Map<String, Boolean> nearby = new HashMap<String, Boolean>();
		for ( Position p : posList) {
			String nearestStreet = findNearest(p, this, here.getStreetName(), logger);
			if (nearestStreet != null && nearby.get(nearestStreet) == null) {
				nearby.put( nearestStreet, true);
			}
		}
		return new ArrayList<String>(nearby.keySet());

	}

	private String findNearest(Position p, Position dest, String fromStreet, Logger logger) {
		final double step = 0.2;
		String street = Sign.reverseGeocode(p, logger).getStreetName();
		String nearest = street;
		while (!street.equals(fromStreet)) {
			p = p.moveTowards(dest, step);
			nearest = street;
			street = Sign.reverseGeocode(p, logger).getStreetName();			
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
	private static String convertToDecimal(String val) {
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
			return Double.toString(dval);
		}
		else {
//			System.out.println("return original " + val);
			return val;
		}
	}
}
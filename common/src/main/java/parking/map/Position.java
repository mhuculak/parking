package parking.map;

public class Position {

	private String latitude;
	private String longitude;

	public Position() {

	}

	public Position(String value) {
		String[] latlng = value.split("_");
		latitude = latlng[0];
		longitude = latlng[1];
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

	public double getLatitude() {
		return Double.parseDouble(latitude);
	}

	public double getLongitude() {
		return Double.parseDouble(longitude);
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
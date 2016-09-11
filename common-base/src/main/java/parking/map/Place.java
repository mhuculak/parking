package parking.map;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import parking.util.Utils;
import java.io.Serializable;

public class Place implements Serializable {
	private Address address;
	private Position position;
	private String streetNumber; // FIXME: why does the goolge geocode appear to drop the street number?
	private static GeocoderInterface geocoder;
	private static ReverseGeocoderInterface reverseGeocoder;

	public Place(Address address, Position position) {
		if (address == null && reverseGeocoder != null) {
			address = reverseGeocoder.reverseGeocode(position);
		}
		this.address = address;
		this.position = position;
	}

	public Place(JSONObject jObj) throws JSONException {
		if (!jObj.isNull("address")) {
			address = new Address(jObj.getJSONObject("address"));
		}
		if (!jObj.isNull("position")) {
			position = new Position(jObj.getJSONObject("position"));
		}
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		if (address != null) {
			jObj.put("address", address.serialize());
		}
		if (position != null) {
			jObj.put("position", position.serialize());
		}
		if (streetNumber != null) {
			jObj.put("streetNumber", streetNumber);
		}
		return jObj;
	}

	public Position getPosition() {
		return position;
	}

	public Address getAddress() {
		return address;
	}

	public String shortAddress() {		
		StringBuilder sb = new StringBuilder(10);
		if (address != null) {
			if (address.getStreetNumber() != null) {
				sb.append(address.getStreetNumber());
			}
			else if (streetNumber != null) {
				sb.append(streetNumber);
			}
			if (address.getStreetName() != null) {
				sb.append(" "+address.getStreetName());
			}
			if (address.getTown() != null) {
				sb.append(" "+address.getTown());
			}
			else if (address.getCity() != null) {
				sb.append(" "+address.getCity());
			}
			else if (address.getProvinceState() != null) {
				sb.append(" "+address.getProvinceState());
			}
			else if (address.getCountry() != null) {
				sb.append(" "+address.getCountry());
			}
			return sb.toString();
		}
		return "";
	}

	public double getLatitude() {
		if (position != null) {
			return position.getLatitude();
		}
		return 0.0;
	}

	public double getLongitude() {
		if (position != null) {
			return position.getLongitude();
		}
		return 0.0;
	}

	public static void setGeocoder(GeocoderInterface gc) {
		geocoder = gc;
	}

	public static void setReverseGeocoder(ReverseGeocoderInterface gc) {
		reverseGeocoder = gc;
	}

	public void setPosition(Position position) {
		this.position = position;
		if (reverseGeocoder != null) {
			address = reverseGeocoder.reverseGeocode(position);
		}
		else {
			address = null;
		}
	}

	public void setStreetNumber(String streetNumber) {
		String[] comp = streetNumber.split(" ");
		if (comp.length > 0) {
			String value = comp[0];
			String numValue = value.replaceAll("[^0-9]","");
			if (Utils.isNotBlank(numValue)) {
				this.streetNumber = value;
			}
		}
	}

	public static Place findPlace(String placeString, Position orig, double maxDist) {
		if (geocoder != null) {
			return geocoder.findPlace(placeString, orig, maxDist);
		}
		return null;
	}
	
}
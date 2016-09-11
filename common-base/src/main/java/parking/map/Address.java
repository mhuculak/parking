package parking.map;

import parking.util.Utils;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType; 

import java.util.List;
import java.io.Serializable;

public class Address implements Serializable {

	private String streetName; // Avenue de Mount Vernon
	private String streetNumber; // 150
	private String town;   // Lachine
	private String admin;  // Communauté-Urbaine-de-Montréal
	private String city;   // Montréal
	private String stateProv;
	private String postalCodeZip;
	private String country;
	private static  parking.map.ReverseGeocoderInterface reverseGeocoder;

	public Address() {

	}

/*

    FIXME: GeocoderAddressComponent foolishly represents the types
           with String i.e.  GeocoderAddressComponent.getTypes() returns List<String> 
           so we would need to reverse engineer it

	public Address(List<<GeocoderAddressComponent> addressComponents) {
		for (<GeocoderAddressComponent ac : addressComponents) {
			setComponent2(ac);
		}
	}

	private void setComponent2(GeocoderAddressComponent ac)	{	
			String type = ac.types[0];
			String value = ac.longName;
			String abbrev = ac.shortName;
	}
*/	
	public Address(AddressComponent[] addressComponents) {
		for ( int i=0 ; i<addressComponents.length ; i++) {
			setComponent(addressComponents[i]);
		}
	}

	private void setComponent(AddressComponent ac)	{	
			AddressComponentType type = ac.types[0];
			String value = ac.longName;
			String abbrev = ac.shortName;
			switch (type) {
				case ROUTE:
					streetName = value;
					break;
				case STREET_NUMBER:
					streetNumber = value;
					break;
				case COUNTRY:
					country = value;
					break;
				case POLITICAL:
					town = value;
					break;
				case LOCALITY:
					city = value;
					break;
				case POSTAL_CODE:
					postalCodeZip = value;
					break;
				case ADMINISTRATIVE_AREA_LEVEL_1:
					stateProv = abbrev;
					break;
				case ADMINISTRATIVE_AREA_LEVEL_2:
					admin = abbrev;
					break;
				case INTERSECTION:		
				case NEIGHBORHOOD:				
				case PARKING:
				
				default:
					System.out.println("Ignoring " + type + " = " + value);

			}	
	}

	public Address(String value) {
		String[] a = value.split(":");
		streetNumber = a[0];
		streetName = a[1];
		city = a[2];
		town = a[3];
		admin = a[4];
		stateProv = a[5];
		postalCodeZip = a[6];
		country = a[7];
	}

	public Address(JSONObject jObj) throws JSONException {
		if (!jObj.isNull("streetNumber")) {
			streetNumber = jObj.getString("streetNumber");
		}
		if (!jObj.isNull("streetName")) {
			streetName = jObj.getString("streetName");
		}
		if (!jObj.isNull("city")) {
			city = jObj.getString("city");
		}		
		if (!jObj.isNull("town")) {
			town = jObj.getString("town");
		}
		if (!jObj.isNull("admin")) {
			admin = jObj.getString("admin");
		}
		if (!jObj.isNull("stateProv")) {
			stateProv = jObj.getString("stateProv");
		}
		if (!jObj.isNull("postalCodeZip")) {
			postalCodeZip = jObj.getString("postalCodeZip");
		}
		if (!jObj.isNull("country")) {
			country = jObj.getString("country");
		}
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		if (streetNumber != null) {
			jObj.put("streetNumber", streetNumber);
		}
		if (streetName != null) {
			jObj.put("streetName", streetName);
		}
		if (city != null) {
			jObj.put("city", city);
		}
		if (town != null) {
			jObj.put("town", town);
		}
		if (admin != null) {
			jObj.put("admin", admin);
		}
		if (stateProv != null) {
			jObj.put("stateProv", stateProv);
		}
		if (postalCodeZip != null) {
			jObj.put("postalCodeZip", postalCodeZip);
		}
		if (country != null) {
			jObj.put("country", country);
		}
		return jObj;
	}

	public String getStreetNumber() {
		return streetNumber;
	}

	public String getStreetName() {
		return streetName;
	}

	public String getTown() {
		return town;
	}

	public String getCity() {
		return city;
	}

	public String getAdminRegion() {
		return admin;
	}

	public String getPostalCodeZIP() {
		return postalCodeZip;
	}

	public String getProvinceState() {
		return stateProv;
	}

	public String getShortAddress() {
		return streetNumber + " " + streetName;
	}

	public String getCountry() {
		return country;
	}

	public void setStreet(String street) {
		String[] comp = street.split(" ");
		int num = Utils.parseInt(comp[0]);		
		if (num == 0) {
			streetNumber = null;
			streetName = street;
		}
		else {
			streetNumber = comp[0];
			StringBuilder sb = new StringBuilder(10);
			for ( int i=1 ; i<comp.length ; i++) {
				if ( i == comp.length-1) {
					sb.append(comp[i]);
				}
				else {
					sb.append(comp[i]+" ");
				}
			}
			streetName = sb.toString();
		}
		
	}

	public void setStreetNumber(String streetNumber) {
		this.streetNumber = streetNumber;
	}

	public void setStreetName(String streetName) {
		this.streetName  = streetName;
	}

	public void setTown(String town) {
		this.town = town;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setAdminRegion(String adminRegion) {
		this.admin = adminRegion;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public void setPostalCodeZIP(String postalCodeZip) {
		this.postalCodeZip = postalCodeZip;
	}

	public void setStateProv(String stateProv) {
		this.stateProv = stateProv;
	}

	public void setCounttry(String country) {
		this.country = country;
	}

	public static void setReverseGeocoder(ReverseGeocoderInterface rg) {
		reverseGeocoder = rg;
	}

	public static  Address reverseGeocode( Position p) {
		if (reverseGeocoder != null) {
			return reverseGeocoder.reverseGeocode(p);
		}
		return null;
	}

	public String getTownOrCity() {
		if (town != null) {
			return town;
		}
		if (city != null) {
			return city;
		}
		return "";
	}

	public String displayText() {
		return streetNumber+" "+streetName+"<br>";
	}

	public String fullString() {
		return streetNumber+" "+streetName+" town:"+town+" city:"+city+" admin:"+admin+" state:"+stateProv+" code:"+postalCodeZip+" country: "+country;
	}

	public String toString() {
		return streetNumber + ":" + streetName + ":" + city + ":" + town + ":" + admin + ":" + stateProv + ":" + postalCodeZip + ":" + country;
	}
}
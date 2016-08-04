package parking.map;

import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;

// 150 Avenue de Mount Vernon, Lachine, QC H8R 1K1, Canada

public class Address {

	private String streetName; // Avenue de Mount Vernon
	private String streetNumber; // 150
	private String town;   // Lachine
	private String admin;  // Communauté-Urbaine-de-Montréal
	private String city;   // Montréal
	private String stateProv;
	private String postalCodeZip;
	private String country;

	public Address(AddressComponent[] addressComponents) {
		for ( int i=0 ; i<addressComponents.length ; i++) {
			AddressComponent ac = addressComponents[i];
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
/*		
		if (address.contains("Canada")) {
			String[] parts = address.split(",");
			String[] num_street = parts[0].split(" ");
			number = num_street[0];
			street = parts[0].replace(number, "");
			city = parts[1];
			String[] provPC = parts[2].split(" ");
			stateProv = provPC[0];
			postalCodeZip = parts[2].replace(stateProv, "");
			country = parts[3];
		}
*/		
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

	public String toString() {
		return streetNumber + ":" + streetName + ":" + city + ":" + town + ":" + admin + ":" + stateProv + ":" + postalCodeZip + ":" + country;
	}
}
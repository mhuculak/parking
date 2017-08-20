package parking.map;

import java.util.Map;
import java.util.HashMap;

//
//  FOLLOW: https://en.wikipedia.org/wiki/ISO_3166-1
//
public enum Country {
	CAN,
	USA;	

	public static final Map<Country, String> countryMap = new HashMap<Country, String>();
	public static final Map<String, Country> countryReverseMap = new HashMap<String, Country>();

	static {
		countryMap.put(CAN, "Canada");
		countryMap.put(USA, "United States");
	}

	static {
		countryReverseMap.put("Canada", CAN);
		countryReverseMap.put("United States", USA);
	}
}


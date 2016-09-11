package parking.schedule;

import parking.util.Utils;

import org.json.JSONObject;
import org.json.JSONException;
//import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;
import java.util.HashMap;

import java.lang.IllegalArgumentException;

import java.io.Serializable;

enum Month {
	January,
	February,
	March,
	April,
	May,
	June,
	July,
	August,
	September,
	October,
	November,
	December
}

public class SimpleDate implements Serializable {
	
	private Month month;
	private int day;

	private static final Map<Month, String> abbrev = new HashMap<Month, String>();
	private static final Map<String, Month> revAbbrev = new HashMap<String, Month>();

	static {
		abbrev.put( Month.January, "Ja");
		abbrev.put( Month.February, "F");
		abbrev.put( Month.March, "M");
		abbrev.put( Month.April, "A");
		abbrev.put( Month.May, "My");
		abbrev.put( Month.June, "Jn");
		abbrev.put( Month.July, "Jy");
		abbrev.put( Month.August, "Au");
		abbrev.put( Month.September, "S");
		abbrev.put( Month.October, "O");
		abbrev.put( Month.November, "N");
		abbrev.put( Month.December, "D");

		revAbbrev.put("Ja", Month.January);
		revAbbrev.put("F", Month.February);
		revAbbrev.put("M", Month.March);
		revAbbrev.put("A", Month.April);
		revAbbrev.put("My", Month.May);
		revAbbrev.put("Jn", Month.June);
		revAbbrev.put("Jy", Month.July);
		revAbbrev.put("Au", Month.August);
		revAbbrev.put("S", Month.September);
		revAbbrev.put("O", Month.October);
		revAbbrev.put("N", Month.November);
		revAbbrev.put("D", Month.December);
	}

	public SimpleDate(Month month, int day) {
		this.month = month;
		this.day = day;
	}

	public SimpleDate(String value) {
		String[] data = value.split(" ");
		month = revAbbrev.get(data[0]);
		day = Utils.parseInt(data[1]);
	}

	public SimpleDate(String monthName, int day) {
		try {
			month = Month.valueOf(monthName);
		}
		catch (IllegalArgumentException ex) {
			month = bestMatch(monthName);
		}
		this.day = day;
	}

	public SimpleDate(String monthName, String day) {
		try {
			month = Month.valueOf(monthName);
		}
		catch (IllegalArgumentException ex) {
			month = bestMatch(monthName);
		}
		this.day = Utils.parseInt(day);
	}

	public SimpleDate(JSONObject jObj) throws JSONException {
		month = Month.valueOf(jObj.getString("month"));
		day = jObj.getInt("day");
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		if (month != null) {
			jObj.put("month", month.toString());
		}		
		jObj.put("day", day);		
		return jObj;
	}

	public String getMonth() {
		return month.toString();
	}

	public int getMonthNum() {
		return month.ordinal();
	}

	public int getDayNum() {
		return day;
	}

	public boolean isValid() {
		return month != null && day>0 && day<32;
	}

	public String getDay() {
		return Integer.toString(day);
	}

	public String displayText() {
		return month+" "+day;
	}
	public String toString() {
		return abbrev.get(month)+" "+day;
	}

	public static String getMonth(int index) {
		return Month.values()[index].toString();
	}

	//
	// FIXME: algorithm doesn't account for insertion and deletion errors
	//        e.g. October is the best match for Deceber
	//
	private Month bestMatch(String value) {
		int min = -1;
		Month best = null; 
		for ( Month m : Month.values() ) {
	    	int cost = doMatch(m.toString(), value);
	    	System.out.println("Got cost "+cost+" for "+m+" value "+value);
	    	if (min <0 || cost < min) {
				min = cost;
				best = m;
	    	}
		}		
		return best;
    }

	private int doMatch(String s1, String s2) {
		char[] a1 = s1.toCharArray();
		char[] a2 = s2.toCharArray();		
		int extra = Math.abs( s1.length() - s2.length());
		int min = s1.length() < s2.length() ? s1.length() : s2.length();
		int i;
		int cost = 0;
		for ( i=0 ; i<min ; i++ ) {
	    	if (Character.toLowerCase(a1[i]) != Character.toLowerCase(a2[i])) { // case independant
				cost++;
	    	}
		}
//		System.out.println("Got cost "+cost+" length diff = "+extra);
		return cost+extra;	
    }
}
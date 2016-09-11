package parking.schedule;

import org.json.JSONObject;
import org.json.JSONException;
//import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;
import java.util.HashMap;

public class DateRange {

//	@JsonProperty("start")
	private SimpleDate start;

//	@JsonProperty("end")
	private SimpleDate end;

	public DateRange(SimpleDate start, SimpleDate end) {
		this.start = start;
		this.end = end;
	}

	public DateRange(String value) {
		String[] data = value.split(",");
		start = new SimpleDate(data[0]);
		end = new SimpleDate(data[1]);
	}

	public DateRange(JSONObject jObj) throws JSONException {
		if (!jObj.isNull("start") && !jObj.isNull("end")) {
			start = new SimpleDate(jObj.getJSONObject("start"));
			end = new SimpleDate(jObj.getJSONObject("end"));
		}
	}

	public DateRange(SimpleDateRangeForJson simple) {
		start = simple.start;
		end = simple.end;
	}

	public SimpleDate getStart() {
		return start;
	}

	public SimpleDate getEnd() {
		return end;
	}

	public boolean isValid() {
		return start.isValid() && end.isValid();
	}

	public Map<ParkingElement, String> getMap() {
		Map<ParkingElement, String> map = new HashMap<ParkingElement, String>();
		map.put( ParkingElement.StartDay, start.getDay() );
		map.put( ParkingElement.StartMonth, start.getMonth() );
		map.put( ParkingElement.EndDay, end.getDay() );
		map.put( ParkingElement.EndMonth, end.getMonth() );
		return map;
	}

	public String displayText() {
		return start.displayText() + " to " + end.displayText();
	}
	
	public String toString() {
		return start+","+end;
	}
}
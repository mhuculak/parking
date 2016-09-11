package parking.schedule;

import org.json.JSONObject;
import org.json.JSONException;
//import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;
import java.util.HashMap;

public class TimeRange {

	private SimpleTime start;
	private SimpleTime end;

	public TimeRange(SimpleTime start, SimpleTime end) {
		this.start = start;
		this.end = end;
	}

	public TimeRange(String value) {
		String[] data = value.split(",");
		start = new SimpleTime(data[0]);
		end = new SimpleTime(data[1]);
	}

	public TimeRange(JSONObject jObj) throws JSONException {
		if (!jObj.isNull("start") && !jObj.isNull("end")) {
			start = new SimpleTime(jObj.getJSONObject("start"));
			end = new SimpleTime(jObj.getJSONObject("end"));
		}
	}

	public TimeRange(SimpleTimeRangeForJson simple) {
		start = simple.start;
		end = simple.end;
	}

	public SimpleTime getStart() {
		return start;
	}

	public SimpleTime getEnd() {
		return end;
	}

	public boolean isValid() {
		return start.isValid() && end.isValid();
	}

	public Map<ParkingElement, String> getMap() {
		Map<ParkingElement, String> map = new HashMap<ParkingElement, String>();
		map.put( ParkingElement.StartHour, start.getHourString() );
		map.put( ParkingElement.StartMin, start.getMinuteString() );
		map.put( ParkingElement.EndHour, end.getHourString() );
		map.put( ParkingElement.EndMin, end.getMinuteString() );
		return map;
	}

	public String displayText() {
		return start.displayText() + " to " + end.displayText();
	}
	
	public String toString() {
		return start+","+end;
	}	
}
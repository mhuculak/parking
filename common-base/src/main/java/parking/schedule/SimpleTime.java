package parking.schedule;

import parking.util.Utils;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.Serializable;

public class SimpleTime implements Serializable {

	private int hour;
	private int minute;

	public SimpleTime(String value) {
		String[] data = value.split(":");
		hour = Utils.parseInt(data[0]);
		minute = Utils.parseInt(data[1]);
	}

	public SimpleTime(String hr, String min) {	
		hour = Utils.parseInt(hr);
		minute = Utils.parseInt(min);
	}

	public SimpleTime(int hour) {
		this.hour = hour;
		minute = 0;
	}

	public SimpleTime(int hour, int min) {
		this.hour = hour;
		this.minute = min;
	}

	public SimpleTime(JSONObject jObj) throws JSONException {
		hour = jObj.getInt("hour");
		minute = jObj.getInt("minute");
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		jObj.put("hour", hour);
		jObj.put("minute", minute);		
		return jObj;
	}

	public int getHour() {
		return hour;
	}

	public String getHourString() {
		return hour < 10 ? "0"+Integer.toString(hour) : Integer.toString(hour);
	}

	public int getMinute() {
		return minute;
	}

	public boolean isValid() {
		return hour > 0 && hour < 24 && minute >=0 && minute <= 60;
	}

	public String getMinuteString() {
		return minute < 10 ? "0"+Integer.toString(minute) : Integer.toString(minute);
	}

	public String displayText() {
		return getHourString()+":"+getMinuteString();
	}

	public String toString() {
		return getHourString()+":"+getMinuteString();
	}
	
}
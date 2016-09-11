package parking.schedule;

import parking.map.Place;
import parking.map.Position;
import parking.map.Address;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;


public class SignSchedule implements Serializable {
	public SimpleScheduleForJson schedule;
	public String place;
	public String id;

	public SignSchedule (ParkingSchedule sched, String place, String id) {
		if (sched != null) {
			this.schedule = new SimpleScheduleForJson(sched);
		}
		this.place = place;
		this.id = id;		
	}

	public SignSchedule(JSONObject jObj) throws JSONException {

		if (!jObj.isNull("schedule")) {
			schedule = new SimpleScheduleForJson(jObj.getJSONObject("schedule"));
		}
		if (!jObj.isNull("place")) {
			place = jObj.getString("place");
		}
		if (!jObj.isNull("id")) {
			id = jObj.getString("id");
		}
		
		
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		if (schedule != null) {
			jObj.put("schedule", schedule.serialize());
		}
		if (place != null) {
			jObj.put("place", place);
		}
		if (id != null) {
			jObj.put("id", id);
		}
		
		return jObj;
	}
}
	
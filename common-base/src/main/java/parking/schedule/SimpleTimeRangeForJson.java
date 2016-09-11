package parking.schedule;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class SimpleTimeRangeForJson implements Serializable {
	public SimpleTime start;
	public SimpleTime end;

	public SimpleTimeRangeForJson(TimeRange range) {
		this.start = range.getStart();
		this.end = range.getEnd();
	}

	public SimpleTimeRangeForJson(JSONObject jObj) throws JSONException {
		if (!jObj.isNull("start") && !jObj.isNull("end")) {
			start = new SimpleTime(jObj.getJSONObject("start"));
			end = new SimpleTime(jObj.getJSONObject("end"));
		}
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		if (start != null) {
			jObj.put("start", start.serialize());
		}
		if (end != null) {
			jObj.put("end", end.serialize());
		}
		return jObj;
	}
}

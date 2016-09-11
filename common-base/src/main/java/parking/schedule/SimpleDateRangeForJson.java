package parking.schedule;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class SimpleDateRangeForJson implements Serializable {
	public SimpleDate start;
	public SimpleDate end;

	public SimpleDateRangeForJson(DateRange range) {
		this.start = range.getStart();
		this.end = range.getEnd();
	}

	public SimpleDateRangeForJson(JSONObject jObj) throws JSONException {
		if (!jObj.isNull("start") && !jObj.isNull("end")) {
			start = new SimpleDate(jObj.getJSONObject("start"));
			end = new SimpleDate(jObj.getJSONObject("end"));
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

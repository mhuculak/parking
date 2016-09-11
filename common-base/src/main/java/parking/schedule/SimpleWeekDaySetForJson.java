package parking.schedule;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class SimpleWeekDaySetForJson implements Serializable {
	
	public List<WeekDay> weekDaySet;
	public boolean isContiguous;

	public SimpleWeekDaySetForJson(WeekDaySet wks) {
		this.isContiguous = wks.isContiguous();
        this.weekDaySet = wks.getSet();        
	}

	public SimpleWeekDaySetForJson(JSONObject jObj) throws JSONException {
		JSONArray jsonArray = jObj.optJSONArray("weekDaySet");
		if (jsonArray != null) {
			weekDaySet = new ArrayList<WeekDay>();
			for ( int i=0 ; i<jsonArray.length() ; i++ ) {
				weekDaySet.add(WeekDay.valueOf(jsonArray.getString(i)));
			}
		}
		if (!jObj.isNull("isContiguous")) {
			isContiguous = jObj.getBoolean("isContiguous");
		}
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		if (weekDaySet != null) {
			JSONArray jsonArray = new JSONArray();
			jObj.put("weekDaySet", jsonArray);
			for (WeekDay day: weekDaySet) {
				jObj.accumulate("weekDaySet", day.toString());
			}
		}
		jObj.put("isContiguous", isContiguous);
		return jObj;
	}
	
}

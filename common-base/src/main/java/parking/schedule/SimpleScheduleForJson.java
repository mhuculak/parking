package parking.schedule;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class SimpleScheduleForJson implements Serializable {
	
	public boolean isRestriction;
	public int timeLimitMinutes;
	public SimpleTimeRangeForJson timeRange;
	public SimpleWeekDaySetForJson weekDays;
	public SimpleDateRangeForJson dateRange;

	public SimpleScheduleForJson() {
		isRestriction = true;
		timeLimitMinutes = 0;
	}

	public SimpleScheduleForJson(ParkingSchedule sched) {
		this.isRestriction = sched.isRestricted();
        this.timeLimitMinutes = sched.getTimeLimitMinutes();
        this.timeRange = sched.getTimeRange() == null ? null : new SimpleTimeRangeForJson(sched.getTimeRange());
        this.weekDays = sched.getWeekDays() == null ? null : new SimpleWeekDaySetForJson(sched.getWeekDays());
        this.dateRange = sched.getDateRange() == null ? null : new SimpleDateRangeForJson(sched.getDateRange());
	}

	public SimpleScheduleForJson(JSONObject jObj) throws JSONException {
		if (!jObj.isNull("isRestriction")) {
			isRestriction = jObj.getBoolean("isRestriction");
		}
		else {
			return; // invalid data
		}
		if (!jObj.isNull("timeLimitMinutes")) {
			timeLimitMinutes = jObj.getInt("timeLimitMinutes");
		}
		
		if (!jObj.isNull("timeRange")) {
			timeRange = new SimpleTimeRangeForJson(jObj.getJSONObject("timeRange"));
		}
		if (!jObj.isNull("weekDays")) {
			weekDays = new SimpleWeekDaySetForJson(jObj.getJSONObject("weekDays"));
		}
		if (!jObj.isNull("dateRange")) {
			dateRange = new SimpleDateRangeForJson(jObj.getJSONObject("dateRange"));
		}
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		
		jObj.put("isRestriction", isRestriction);
		jObj.put("timeLimitMinutes", timeLimitMinutes);		
		if (timeRange != null) {
			jObj.put("timeRange", timeRange.serialize());
		}
		if (weekDays != null) {
			jObj.put("weekDays", weekDays.serialize());
		}
		if (dateRange != null) {
			jObj.put("dateRange", dateRange.serialize());
		}
		return jObj;
	}


	public boolean hasTimeRange() {
		return timeRange != null;
	}

	public String timeStart() {
		return timeRange.start.toString();
	}

	public String timeEnd() {
		return timeRange.end.toString();
	}

	public boolean hasDateRange() {
		return dateRange != null;
	}


	public String dateStart() {
		return dateRange.start.displayText();
	}

	public String dateEnd() {
		return dateRange.end.displayText();
	}

	public boolean hasWeekDays() {
		return weekDays != null;
	}

	public List<WeekDay> weekDaySet() {
		return weekDays.weekDaySet;
	}
}

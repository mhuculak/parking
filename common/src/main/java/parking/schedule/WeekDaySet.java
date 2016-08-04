package parking.schedule;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

enum WeekDay {
	Sunday,
	Monday,
	Tuesday,
	Wednesday,
	Thursday,
	Friday,
	Saturday;
}

public class WeekDaySet {

	private List<WeekDay> weekDaySet;
	private boolean isContiguous;

	public static final List<String> allDays = new ArrayList<String>();

	static {
		for (WeekDay day : WeekDay.values()) {
			allDays.add(day.toString());
		}
	}
	
	public WeekDaySet() {

	}

	public WeekDaySet(String wkdSet) {
		if (wkdSet.contains(" to ")) {
			String[] days = wkdSet.split(" to ");
			createWeekDaySet(days[0],days[1]);
		}
		else {
			String[] days = wkdSet.split(",");
			weekDaySet = new ArrayList<WeekDay>();
			for ( int i=0 ; i<days.length ; i++) {
				weekDaySet.add(WeekDay.valueOf(days[i]));
			}
			checkContinuity();
		}
	}

	public WeekDaySet(String start, String end) {
		createWeekDaySet(start,end);
	}

	private void createWeekDaySet(String start, String end) {
		WeekDay swd = start.length()>0 ? WeekDay.valueOf(start) : null;
		WeekDay ewd = end.length()>0 ? WeekDay.valueOf(end) : null;
		weekDaySet = new ArrayList<WeekDay>();
		if (swd != null && ewd != null) {
			for ( int i=swd.ordinal() ; i<= ewd.ordinal() ; i++) {
				weekDaySet.add(WeekDay.values()[i]);
			}
			isContiguous = true;
		}
		else if (swd != null) {
			weekDaySet.add(swd);
		}
		else if (ewd != null) {
			weekDaySet.add(ewd);
		}
	}

	public void add(WeekDay day) {
		if (weekDaySet == null) {
			weekDaySet = new ArrayList<WeekDay>();
		}
		weekDaySet.add(day);
		checkContinuity();
	}

	public void add(String day) {
		if (weekDaySet == null) {
			weekDaySet = new ArrayList<WeekDay>();
		}
		weekDaySet.add(WeekDay.valueOf(day));
		checkContinuity();
	}

	public List<WeekDay> getSet() {
		return weekDaySet;
	}

	public int size() {
		if (weekDaySet != null) {
			return weekDaySet.size();
		}
		return 0;
	}

	public String get(int index) {
		return weekDaySet.get(index).toString();
	}

	public String getStart() {
		return weekDaySet.get(0).toString();
	}

	public String getEnd() {
		return weekDaySet.get(weekDaySet.size()-1).toString();
	}

	public Map<String, Boolean> getMap() {
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		for (WeekDay day : weekDaySet) {
			map.put(day.toString(), true);
		}
		for (String day : allDays) {
			if (map.get(day) == null) {
				map.put(day, false);
			}
		}
		return map;
	}

	public boolean isContiguous() {
		return isContiguous;
	}

	private void checkContinuity() {
		WeekDay first = weekDaySet.get(0);
		WeekDay last = weekDaySet.get(weekDaySet.size()-1);
		isContiguous = true;
		int i,k;
		for ( k=0, i=first.ordinal() ; i<= last.ordinal() && k<weekDaySet.size() ; i++, k++ ) {
			WeekDay next = WeekDay.values()[i];
			if (next != weekDaySet.get(k)) {
				isContiguous = false;
			}
		}
	}

	public String toString() {
		
		if (isContiguous && weekDaySet.size() > 1) {
			return weekDaySet.get(0)+" to "+weekDaySet.get(weekDaySet.size()-1);
		}
		else {
			if (weekDaySet == null) {
				return "";
			}
			StringBuilder sb = new StringBuilder(10);
			for (WeekDay day : weekDaySet) {
				sb.append(day+",");
			}
			return sb.toString();
		}
	}
}
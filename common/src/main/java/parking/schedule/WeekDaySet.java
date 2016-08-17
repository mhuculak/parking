package parking.schedule;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

enum WeekDay {
	Sunday,
	Monday,
	Tuesday,
	Wednesday,
	Thursday,
	Friday,
	Saturday
}

public class WeekDaySet {

	private List<WeekDay> weekDaySet;
	private boolean isContiguous;

	public static final List<String> allDays = new ArrayList<String>();
	private static final Map<WeekDay, String> abbrev = new HashMap<WeekDay, String>();
	private static final Map<String, WeekDay> revAbbrev = new HashMap<String, WeekDay>();

	static {
		for (WeekDay day : WeekDay.values()) {
			allDays.add(day.toString());
		}
		abbrev.put(WeekDay.Sunday, "Su");
		abbrev.put(WeekDay.Monday, "M");
		abbrev.put(WeekDay.Tuesday, "Tu");
		abbrev.put(WeekDay.Wednesday, "W");
		abbrev.put(WeekDay.Thursday, "Th");
		abbrev.put(WeekDay.Friday, "F");
		abbrev.put(WeekDay.Saturday, "Sa");

		revAbbrev.put("Su", WeekDay.Sunday);
		revAbbrev.put("M", WeekDay.Monday);
		revAbbrev.put("Tu", WeekDay.Tuesday);
		revAbbrev.put( "W", WeekDay.Wednesday);
		revAbbrev.put("Th", WeekDay.Thursday);
		revAbbrev.put("F", WeekDay.Friday);
		revAbbrev.put("Sa", WeekDay.Saturday);
	}
	
	public WeekDaySet() {

	}

	public WeekDaySet(String wkdSet) {
		if (wkdSet.contains(":")) {
			String[] days = wkdSet.split(":");
			createWeekDaySet(revAbbrev.get(days[0]),revAbbrev.get(days[1]));
		}
		else {
			String[] days = wkdSet.split(",");
			weekDaySet = new ArrayList<WeekDay>();
			for ( int i=0 ; i<days.length ; i++) {
				weekDaySet.add(revAbbrev.get(days[i]));
			}
			checkContinuity();
		}
	}

	public WeekDaySet(String start, String end) {
		if (start != null && end != null) {
			WeekDay swd = WeekDay.valueOf(start);
			WeekDay ewd = WeekDay.valueOf(end);
			if (swd != null && ewd != null) {
				createWeekDaySet(swd, ewd);
			}
		}
	}

	private void createWeekDaySet(WeekDay start, WeekDay end) {
		weekDaySet = new ArrayList<WeekDay>();
		for ( int i=start.ordinal() ; i<= end.ordinal() ; i++) {
			weekDaySet.add(WeekDay.values()[i]);
		}
		isContiguous = true;
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
		Collections.sort(weekDaySet);
		int i,k;
/*		
		for ( k=0 ; k<weekDaySet.size() ; k++) {
			System.out.println(k+" "+weekDaySet.get(k));
		}
*/		
		WeekDay first = weekDaySet.get(0);
		WeekDay last = weekDaySet.get(weekDaySet.size()-1);
		isContiguous = true;
		
//		System.out.println("Check continuity from "+first+" "+first.ordinal()+" to "+last+" "+last.ordinal());
		for ( k=0, i=first.ordinal() ; i<= last.ordinal() && k<weekDaySet.size() ; i++, k++ ) {
			WeekDay next = WeekDay.values()[i];
//			System.out.println(" day "+i+" is "+next+" should be "+weekDaySet.get(k));
			if (next != weekDaySet.get(k)) {
				isContiguous = false;
			}
		}
	}

	public String displayText() {
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

	public String toString() {
		
		if (isContiguous && weekDaySet.size() > 1) {
			return abbrev.get(weekDaySet.get(0))+":"+abbrev.get(weekDaySet.get(weekDaySet.size()-1));
		}
		else {
			if (weekDaySet == null) {
				return "";
			}
			StringBuilder sb = new StringBuilder(10);
			for (WeekDay day : weekDaySet) {
				sb.append(abbrev.get(day)+",");
			}
			return sb.toString();
		}
	}
}
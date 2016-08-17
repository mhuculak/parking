package parking.schedule;

public class SimpleTime {

	private int hour;
	private int minute;
/*
	public SimpleTime(String timeString) {

		char[] cvals = timeString.toCharArray();
		if (cvals.length == 4) {
			StringBuilder h = new StringBuilder(2);
			h.append(cvals[0]);
			h.append(cvals[1]);			
			hour = Integer.parseInt(h.toString());
			StringBuilder m = new StringBuilder(2);
			m.append(cvals[2]);
			m.append(cvals[3]);
			minute = Integer.parseInt(m.toString());
			if (hour > 24 || minute > 60) {
				System.out.println("ERROR: out of range values found in " + timeString);
			}
		}
		else {
			System.out.println("ERROR: unable to parse time string " + timeString);
		}

	}
*/
	public SimpleTime(String value) {
		String[] data = value.split(":");
		hour = Integer.parseInt(data[0]);
		minute = Integer.parseInt(data[1]);
	}
	
	public SimpleTime(String hr, String min) {
		hr.replaceAll("[^0-9]","");
		hr.replaceAll("[^0-9]","");		
		hour = hr == null ? 0 : Integer.parseInt(hr);
		minute = min == null ? 0 : Integer.parseInt(min);
	}

	public SimpleTime(int hour) {
		this.hour = hour;
		minute = 0;
	}

	public SimpleTime(int hour, int min) {
		this.hour = hour;
		this.minute = min;
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
package parking.schedule;

enum Month {
	January,
	February,
	March,
	April,
	May,
	June,
	July,
	August,
	September,
	October,
	November,
	December;
}

public class SimpleDate {
	
	private Month month;
	private int day;

	public SimpleDate(Month month, int day) {
		this.month = month;
		this.day = day;
	}

	public SimpleDate(String value) {
		String[] data = value.split(" ");
		month = Month.valueOf(data[0]);
		day = Integer.parseInt(data[1]);
	}

	public SimpleDate(String monthName, int day) {
		month = Month.valueOf(monthName);
		this.day = day;
	}

	public String getMonth() {
		return month.toString();
	}

	public String getDay() {
		return Integer.toString(day);
	}

	public String toString() {
		return month+" "+day;
	}
}
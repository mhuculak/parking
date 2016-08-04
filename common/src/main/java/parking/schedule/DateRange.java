package parking.schedule;

public class DateRange {

	private SimpleDate start;
	private SimpleDate end;

	public DateRange(SimpleDate start, SimpleDate end) {
		this.start = start;
		this.end = end;
	}

	public DateRange(String value) {
		String[] data = value.split(",");
		start = new SimpleDate(data[0]);
		end = new SimpleDate(data[1]);
	}

	public SimpleDate getStart() {
		return start;
	}

	public SimpleDate getEnd() {
		return end;
	}

	public String toString() {
		return start+","+end;
	}
}
package parking.schedule;

public class TimeRange {

	private SimpleTime start;
	private SimpleTime end;

	public TimeRange(SimpleTime start, SimpleTime end) {
		this.start = start;
		this.end = end;
	}

	public TimeRange(String value) {
		String[] data = value.split(",");
		start = new SimpleTime(data[0]);
		end = new SimpleTime(data[1]);
	}

	public SimpleTime getStart() {
		return start;
	}

	public SimpleTime getEnd() {
		return end;
	}

	public String displayText() {
		return start.displayText() + " to " + end.displayText();
	}
	
	public String toString() {
		return start+","+end;
	}	
}
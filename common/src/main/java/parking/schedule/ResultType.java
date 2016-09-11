package parking.schedule;

import java.util.Map;
import java.util.HashMap;

public enum ResultType {
	CorrectAccept,
	FalseAcceptIn,
	FalseAcceptOut,
	FalseReject;

	public static final Map<ResultType, String> abbrev = new HashMap<ResultType, String>();
	static{

		abbrev.put(CorrectAccept, "CA");
		abbrev.put(FalseAcceptIn, "FAin");
		abbrev.put(FalseAcceptOut, "FAout");
		abbrev.put(FalseReject, "FR");
	}
}
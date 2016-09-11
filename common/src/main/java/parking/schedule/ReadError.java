package parking.schedule;

import java.util.Map;
import java.util.HashMap;

public class ReadError {
	
	private Map<ParkingElement, ResultType> errorMap;
	private Map<ParkingElement, String> correctMap;
	private Map<ParkingElement, String> resultMap;
	private boolean isCorrect;

	public ReadError( ParkingSchedule correct, ParkingSchedule result) {
		correctMap = correct.getMap();
		resultMap = result.getMap();
		createErrorMap();
	}

	public Map<ParkingElement, ResultType> getErr() {
		return errorMap;
	}

	private void createErrorMap() {
		isCorrect = true;
		errorMap = new HashMap<ParkingElement, ResultType>();
		for ( ParkingElement element : ParkingElement.values()) {
			String correctVal = correctMap.get(element);
			String resultVal = resultMap.get(element);
			if (correctVal != null && resultVal != null ) {
				if (correctVal.equals(resultVal)) {
					errorMap.put(element, ResultType.CorrectAccept);
				}
				else {
					errorMap.put(element, ResultType.FalseAcceptIn);
					isCorrect = false;
				}
			}
			else if (correctVal != null && resultVal == null) {
				errorMap.put(element, ResultType.FalseReject);
				isCorrect = false;
			}
			else if (correctVal == null && resultVal != null) {
				errorMap.put(element, ResultType.FalseAcceptOut);
				isCorrect = false;
			}			
		}
		if (isCorrect) {
			errorMap.put(ParkingElement.All, ResultType.CorrectAccept);
		}
		else {
			errorMap.put(ParkingElement.All, ResultType.FalseAcceptIn);
		}
	}

	public boolean isCorrect() {
		return isCorrect;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(10);
		for (ParkingElement element : errorMap.keySet()) {
			sb.append(element.toString()+"="+errorMap.get(element).toString()+" ");
		}
		return sb.toString();
	}
}
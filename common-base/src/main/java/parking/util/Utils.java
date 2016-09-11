package parking.util;

public class Utils {

	public static int parseInt(String value) {
		if (value != null) {
			value = value.replaceAll("[^0-9]","");
			if (isNotBlank(value)) {
				return Integer.parseInt(value);
			}
		}
		return 0;
	}

	public static int round(double val) {
		return (int)(val +0.5);
	}

	public static boolean isNotBlank(String value) {
		return value != null && value.length() > 0;
	}
}
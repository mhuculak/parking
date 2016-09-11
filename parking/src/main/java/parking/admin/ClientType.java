package parking.admin;

public enum ClientType {
	BROWSER,
	APP;

	static ClientType getClientType(String userAgent) {
		if (userAgent.contains("okhttp")) {
			return APP;
		}
		else {
			return BROWSER;
		}
	}

	static String getContentType(ClientType type) {
		switch(type) {
			case BROWSER:
				return "text/html";
			case APP:
				return "text/plain";
			default:
				return "text/plain";
		}
	}
}
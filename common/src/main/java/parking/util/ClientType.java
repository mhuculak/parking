package parking.util;

public enum ClientType {
	BROWSER,
	IMPORT,
	APP;

	static public ClientType getClientType(String userAgent) {
		if (userAgent.contains("okhttp")) {
			return APP;
		}
		else if (userAgent.contains("import-tool")) {
			return IMPORT;
		}
		else {
			return BROWSER;
		}
	}

	static public String getContentType(ClientType type) {
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
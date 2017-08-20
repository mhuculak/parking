package parking.security;

//import parking.util.Logger;
//import parking.util.LoggingTag;

import org.json.JSONObject;
import org.json.JSONException;

public class User {

	private String userName;
	private char[] password;
	private String phoneNumber;
	private String email;
	private String homeAddress;
	private Permission permission;

	private static final int minPasswordLen = 8;
	private static final int minPhoneLen = 10;

	public enum Permission { user, admin, root }

	public User() {

	}

	public User( String userName) {
		this.userName = userName;
	}

	public User(String userName, char[] password) {
		this.userName = userName;
		this.password = password;
	}

	public User(String userName, char[] password, String phoneNumber, String email, String address) {
		this.userName = userName;
		this.password = password;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.homeAddress = address;
	}

	public User(JSONObject jObj) throws JSONException {
		if (!jObj.isNull("userName")) {
			userName = jObj.getString("userName");
		}
		if (!jObj.isNull("password")) {
			password = jObj.getString("password").toCharArray();
		}
		if (!jObj.isNull("phoneNumber")) {
			phoneNumber = jObj.getString("phoneNumber");
		}
		if (!jObj.isNull("email")) {
			email = jObj.getString("email");
		}
		if (!jObj.isNull("homeAddress")) {
			homeAddress = jObj.getString("homeAddress");
		}
	}

	public String getUserName() {
		return userName;
	}

/*
	public char[] getPassword() {
		return password;
	}
*/
	public String getPassword() {
		if (password != null) {
			StringBuilder sb = new StringBuilder(10);
			sb.append(password);
			return sb.toString();
		}
		return "";
	}

	public String getPasswordProblems() {
		StringBuilder problems = new StringBuilder(10);
		if (password.length < minPasswordLen) {
			problems.append("min password len "+minPasswordLen);
		}
		String result = problems.toString();
		if (result.length() > 0) {
			return result;
		}
		return null;
	}

	public boolean hasValidPhoneNumber() {
		if (phoneNumber == null) {
			return false;
		}
		String numOnly = phoneNumber.replaceAll("[^0-9]","");
		if (numOnly.length() < minPhoneLen) {
			return false;
		}
		return true;
	}

	public String getPhone() {
		return phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public String getAddress() {
		return homeAddress;
	}

	public Permission getPermission() {
		return permission;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public void setPermission(Permission permission) {
		this.permission = permission;
	}

	public void setPhone(String phone) {
		phoneNumber = phone;
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jObj = new JSONObject();
		if (userName != null) {
			jObj.put("userName", userName);
		}
		if (password != null) {
			jObj.put("password", new String(password));
		}
		if (phoneNumber != null) {
			jObj.put("phoneNumber", phoneNumber);
		}
		if (email != null) {
			jObj.put("email", email);
		}
		if (homeAddress != null) {
			jObj.put("homeAddress", homeAddress);
		}

		return jObj;
	}

	public static boolean grantPermission(Permission userPermission, Permission requiredPermission) {
//		Logger logger = new Logger(LoggingTag.Security, "User", "grantPermission");
		if (userPermission.compareTo(requiredPermission) >= 0) {
//			logger.log("Permission required is " + requiredPermission);
			return true;
		}
		else {
//			logger.log("Permission is not granted for " + userPermission + " required is " + requiredPermission);
			return false;
		}
	}

}
package parking.security;

public class User {
	private String userName;
	private char[] password;
	private Permission permission;

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

	public String getUserName() {
		return userName;
	}
/*
	public char[] getPassword() {
		return password;
	}
*/
	public String getPassword() {
		StringBuilder sb = new StringBuilder(10);
		sb.append(password);
		return sb.toString();
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

	public static boolean grantPermission(Permission userPermission, Permission requiredPermission) {
		if (userPermission.compareTo(requiredPermission) >= 0) {
			System.out.println("Permission is granted for " + userPermission + " required is " + requiredPermission);
			return true;
		}
		else {
			System.out.println("Permission is not granted for " + userPermission + " required is " + requiredPermission);
			return false;
		}
	}

}
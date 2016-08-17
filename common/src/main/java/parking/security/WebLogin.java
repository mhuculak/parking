package parking.security;

import parking.database.MongoInterface;
import parking.security.User.Permission;
import parking.security.User;

import parking.util.Logger;
import parking.util.LoggingTag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URLDecoder;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import java.util.Map;
import java.util.HashMap;

public class WebLogin {
	
	private String userName;
	private String uri;
	private boolean isLoggedIn;

	public WebLogin(String uri) {
		this.uri = uri;
		isLoggedIn = false;
	}

	public static WebLogin getLogin(HttpServletRequest request) {
		HttpSession session = request.getSession();
		return (WebLogin)session.getAttribute("login-session");
	}

	public static boolean checkLogin(HttpServletRequest request) {
		HttpSession session = request.getSession();
		WebLogin login = (WebLogin)session.getAttribute("login-session");
		return login.isLoggedIn();
	}

	public static String getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession();
		WebLogin login = (WebLogin)session.getAttribute("login-session");
		return login.getUserName();
	}

	public static WebLogin Authenticate(HttpServletRequest request, HttpServletResponse response, Permission requiredPermission) {
		Map<String,String> postData = getBody(request);
		return Authenticate(request, response, requiredPermission, postData);
	}

	public static WebLogin Authenticate(HttpServletRequest request, HttpServletResponse response, Permission requiredPermission, Map<String,String> postData ) {
		Logger logger = new Logger(request.getSession(),LoggingTag.Security, "WebLogin", "Authenticate");
		HttpSession session = request.getSession();
		WebLogin login = (WebLogin)session.getAttribute("login-session");
		if (login == null) {
			logger.log("login-session not found");
			login = new WebLogin(request.getRequestURI());			
		}
		else {
			logger.log("UserName is " + login.getUserName());
		}
		String method = request.getMethod();
		String query = request.getQueryString();
		if (method.equals("POST") && query!=null && query.contains("action") && query.contains("login")) {						
			String userName = postData.get("userName");
			if (userName != null) {
				String db = request.getParameter("db");
				MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
				logger.log("Check username " + userName + " and password " + postData.get("password"));
				if (mongo.getUserDB().checkPassword(userName, postData.get("password"))) {
					logger.log("Check required permission " + requiredPermission);
					Permission userPermission = mongo.getUserDB().getPermission(userName);
					if (User.grantPermission(userPermission,requiredPermission)) {
						login.setLoggedIn(userName);
						try {
							logger.log("Redirecting back to " + request.getRequestURI());
							response.sendRedirect(response.encodeRedirectURL(request.getRequestURI()));
						}
						catch (IOException ex) {
	    					ex.printStackTrace();
						}
					}
					else {
						logger.log("Check permission failed. User permission is " + userPermission);
					}
				}
			}
		}
		session.setAttribute("login-session", login);
		return login;

	}

	public void setLoggedIn(String userName) {
		isLoggedIn = true;
		this.userName = userName;
	}

	public boolean isLoggedIn() {
		return isLoggedIn;
	}

	public String getUserName() {
		return userName;
	}

	public String getLoginForm() {
		String loginHTML = "<form action=\"" + uri + "?action=login\" method=\"POST\">" +
				"User Name: <input type=\"text\" name=\"userName\"/><br>" +
				"Password: <input type=\"password\" name=\"password\"/><br>" +
				"<input type=\"submit\" name=\"submit\" value=\"Submit\" /></form>";
		return loginHTML;
	}

	public String toString() {
		return userName+" "+uri+" logged on ="+isLoggedIn;
	}

	public static Map<String,String> getBody(HttpServletRequest request) {
		String body = null;
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;
		Map<String, String> map = new HashMap<String, String>();
		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
		    		stringBuilder.append(charBuffer, 0, bytesRead);
				}
	    	} else {
				stringBuilder.append("");
	    	}
	    	body = stringBuilder.toString();
//	    	System.out.println("Request Body: " + body);
	    	String[] info = body.split("&");
	    	for ( int i=0 ; i<info.length ; i++ ) {
	    		String[] keyval = info[i].split("=");
	    		if ( keyval.length == 2) {
					String key = keyval[0];		
					String value = URLDecoder.decode(keyval[1],"UTF-8");
					map.put(key, value);
				}
			}
		} catch (IOException ex) {
	    	ex.printStackTrace();
		} finally {
	    	if (bufferedReader != null) {
				try {
		    		bufferedReader.close();
				} catch (IOException ex) {
		   			 ex.printStackTrace();
				}
	    	}
		}
		return map;		
	}
}
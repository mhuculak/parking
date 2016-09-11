package parking.security;

import parking.database.MongoInterface;
import parking.security.User.Permission;
import parking.security.User;

import parking.util.Logger;
import parking.util.LoggingTag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;
import java.net.URLDecoder;

import java.util.Base64;
import java.util.Enumeration;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import java.util.Map;
import java.util.HashMap;

public class WebLogin {
	
	private String userName;
	private String uri;
	private String serverRealm;
	private boolean isLoggedIn;

	public WebLogin(HttpServletRequest request) {
		serverRealm = getServerRealm(request);
		uri = request.getRequestURI();
		isLoggedIn = false;
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

	public String getServerRealm() {
		return serverRealm;
	}

	private String getServerRealm(HttpServletRequest request) {
		int port = request.getServerPort();
		String server = request.getServerName();
		String serverRealm = null;
		if (port == 8081) {
			serverRealm = server+"_demo";
		}
		else if (port == 8082) {
			serverRealm = server+"_test";
		}
		else {
			serverRealm = server;
		}
		return serverRealm;
	}

	public void setResponseNotAuthorized(HttpServletResponse response) {
		response.setHeader("WWW-Authenticate","Basic realm=\""+getServerRealm()+"\"");       
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
    }

    public static void logHeaders(HttpServletRequest request) {
    	Logger logger = new Logger(request.getSession(), LoggingTag.Security, "WebLogin", "headers");	
    	Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			String headerValue = request.getHeader(headerName);
			logger.log("Header "+headerName+" = "+headerValue);			
		}
    }       

	public static WebLogin authenticate(HttpServletRequest request) {
		Logger logger = new Logger(request.getSession(), LoggingTag.Security, "WebLogin", "Authenticate");		
		WebLogin login = new WebLogin(request);
		String authorizationHeader = request.getHeader("Authorization");
  		if(authorizationHeader!=null) {
  			logger.log("Found Authorization header "+authorizationHeader);
  			String encodedStuff = authorizationHeader.substring(6);  			
   			byte[] usernpass=Base64.getDecoder().decode(encodedStuff); // Note: header is "Basic "+usernpass
   			String decodedStuff = new String(usernpass);
   			logger.log("decoded "+encodedStuff+" to "+decodedStuff);   			
   			String user2=decodedStuff.substring(0,decodedStuff.indexOf(":"));
   			String password2=decodedStuff.substring(decodedStuff.indexOf(":")+1);
   			logger.log("extracted user = "+user2+" passwd = "+password2);   			
   			String db = request.getParameter("db");
   			MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
   			if (mongo.getUserDB().checkPassword(user2, password2)) {
   				logger.log("password check successfull");
   				login.setLoggedIn(user2);
   			}
   			else {
   				logger.log("password check failed");
   			}
  		}
  		else {
  			logger.log("No Authorization header found");
  		}
  		return login;  
	}

	public static WebLogin getLogin(HttpServletRequest request) {
		
		Cookie[] cookies = request.getCookies();
		
		String userCookieName = request.getRequestURI()+":userName";
		String loginCookieName = request.getRequestURI()+":loggedIn";
		String userName = null;
		boolean loggedIn = false;
		if (cookies != null) {
			System.out.println("found "+cookies.length+" cookies");
			for ( int i=0 ; i<cookies.length ; i++) {
				Cookie cookie = cookies[i];
				System.out.println("found cookie "+cookie.getName()+" value = "+cookie.getValue());
				if (cookie.getName().equals(userCookieName)) {
					userName = cookie.getValue();
				}
				else if (cookie.getName().equals(loginCookieName)) {
					loggedIn = Boolean.parseBoolean(cookie.getValue());
				}
			}
		}
		else {
			System.out.println("No cookies found");
		}
		WebLogin login = new WebLogin(request);
		if (loggedIn && userName != null) {
			login.setLoggedIn(userName);
			return login;
		}
		else {
			HttpSession session = request.getSession();	
			return (WebLogin)session.getAttribute("login-session");
		}
		
		
	}


	public static WebLogin Authenticate(HttpServletRequest request, HttpServletResponse response, Permission requiredPermission) {
		Map<String,String> postData = getBody(request);
		return Authenticate(request, response, requiredPermission, postData);
	}

	public static WebLogin Authenticate(HttpServletRequest request, HttpServletResponse response, Permission requiredPermission, Map<String,String> postData ) {
		Logger logger = new Logger(request.getSession(),LoggingTag.Security, "WebLogin", "Authenticate");
		HttpSession session = request.getSession();
		WebLogin login = getLogin(request);
		if (login == null) {
			logger.log("login-session not found");
			login = new WebLogin(request);			
		}
		else {
			logger.log("UserName is " + login.getUserName());
		}
		String method = request.getMethod();
		String query = request.getQueryString();
		logger.log("method = "+method+" query = "+query);
		if (method.equals("POST") && query!=null && query.contains("action") && query.contains("login")) {						
			String userName = postData.get("userName");
			logger.log("user name = "+userName);
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
							login.setResponse(request, response);
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
			else {
				logger.log("failed with user name = "+userName);
			}			
		}
		else {
			logger.log("IGNORING method = "+method+" query = "+query);
		}
		session.setAttribute("login-session", login);
		return login;

	}

	public void setResponse(HttpServletRequest request, HttpServletResponse response) {
		String userCookieName = request.getRequestURI()+":userName";
		String loginCookieName = request.getRequestURI()+":loggedIn";
		response.addCookie(new Cookie(userCookieName, userName));
		response.addCookie(new Cookie(loginCookieName, Boolean.toString(isLoggedIn)));
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
//					System.out.println("Add key/value: " + key + " = \""+value+"\"");
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
/*		
		for (String key: map.keySet()) {
			System.out.println("returning: key = "+key+" value = "+map.get(key));
		}
*/		
		return map;		
	}
}
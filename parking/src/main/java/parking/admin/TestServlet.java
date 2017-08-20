package parking.admin;

import parking.security.WebLogin;
import parking.database.MongoInterface;
import parking.security.Password;
import parking.security.User.Permission;

import parking.map.Sign;
import parking.schedule.ParkingSchedule;
import parking.util.ClientType;
import parking.util.Logger;
import parking.util.LoggingTag;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class TestServlet extends HttpServlet {					 

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Logger logger = new Logger(request.getSession(), LoggingTag.Servlet, this, "service");
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");      	
      
      	ClientType client = ClientType.getClientType(userAgent);
      	logger.log("request from "+userAgent+" client "+client);
      	Map<String, String> postData = null;
      	if (method.equals("POST")) {
      		postData = WebLogin.getBody(request);
    	}
      	WebLogin login = WebLogin.authenticate(request);
		if (login.isLoggedIn() == false) {        
         	login.setResponseNotAuthorized(response); 
         	logger.log("return not authorized");        
         	return;
      	}
      	logger.log("return ok");
      	response.setContentType(ClientType.getContentType(client));
      	response.setStatus(HttpServletResponse.SC_OK );
	}
}
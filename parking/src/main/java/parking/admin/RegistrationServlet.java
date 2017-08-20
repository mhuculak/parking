package parking.admin;

import parking.database.MongoInterface;
import parking.map.Trajectory;
import parking.map.MapInit;
import parking.security.WebLogin;
import parking.security.User;
import parking.util.ClientType;
import parking.util.Logger;
import parking.util.LoggingTag;

import org.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.io.IOException;
import java.io.PrintWriter;

public class RegistrationServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Logger logger = new Logger(request.getSession(), LoggingTag.Servlet, this, "service");
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
      	logger.log("Enter "+request.getRequestURI()+" method = "+method+" user agent = "+userAgent);
      	MapInit mapInit = new MapInit();
      
      	ClientType client = ClientType.getClientType(userAgent);
      	logger.log("request from "+userAgent+" client "+client);
      	Map<String, String> postData = null;
      	if (method.equals("POST")) {
      		postData = WebLogin.getBody(request);
      		logger.log("got "+postData.size()+" keys from form");
    	}
      	 
      	response.setContentType(ClientType.getContentType(client));

      	String db = request.getParameter("db");
    	MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
        PrintWriter out = response.getWriter();
        response.setContentType("text/plain");

      	if (postData.get("register") != null) {
      		if (client == ClientType.APP) {
                try {
                    String json = postData.get("register");
                	JSONObject jObj = new JSONObject(json);
                    User user = new User(jObj);
                    String passwordProblems = user.getPasswordProblems();
                    if (passwordProblems != null) {
                        logger.log("rejecting password because "+passwordProblems);
                        out.println(passwordProblems);
                    }
                    else if (mongo.getUserDB().userNameExists(user.getUserName())) {
                        logger.log("rejecting user name "+user.getUserName()+" because it already exists");
                        out.println("user name already exists");
                    }
                    else if (!user.hasValidPhoneNumber()) {
                        logger.log("phone number "+user.getPhone()+" is not valid");
                        out.println("phone number is not valid");
                    }
                    else {
                        logger.log("adding new user "+user.getUserName()+" to DB");
                        mongo.getUserDB().addUser(user);
                    }
              		return;
                }  
                catch (Exception ex) {
                	logger.error("Got exception "+ex);
              		ex.printStackTrace();
                }
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                
            }
      	}
    
    }
}

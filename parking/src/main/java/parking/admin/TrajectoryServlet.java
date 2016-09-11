package parking.admin;

import parking.database.MongoInterface;
import parking.map.Trajectory;
import parking.map.MapInit;
import parking.security.WebLogin;

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

public class TrajectoryServlet extends HttpServlet {

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
      	WebLogin login = WebLogin.authenticate(request);  

      	response.setContentType(ClientType.getContentType(client));

      	if (login.isLoggedIn() == false) {        
         	login.setResponseNotAuthorized(response);         
         	return;
      	}
            
      	logger.log("Got login = "+login.toString());      
      	String user = login.getUserName();

      	String db = request.getParameter("db");
    	MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);

      	if (postData.get("signID") != null) {
      		if (client == ClientType.APP) {
                try {
                	double startTime = Double.parseDouble(postData.get("start"));
                	double endTime = Double.parseDouble(postData.get("end"));
                	String signID = postData.get("signID");
                	String json = postData.get("trajectory");
              		JSONObject jObj = new JSONObject(json);
              		Trajectory trajectory = new Trajectory(jObj);
              		mongo.getTrajectoryDB().addTrajectory(signID, trajectory, startTime, endTime);
              		response.setStatus(HttpServletResponse.SC_OK );
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
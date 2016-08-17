package parking.admin;

import parking.schedule.ParkingSchedule;
import parking.database.MongoInterface;
import parking.security.WebLogin;
import parking.security.User.Permission;
import parking.map.Sign;
import parking.map.Address;
import parking.map.Position;

import parking.util.Logger;
import parking.util.LoggingTag;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;

public class VerifyServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Logger logger = new Logger(request.getSession(), LoggingTag.Servlet, this, "service");
		String method = request.getMethod();
		int port = request.getServerPort();
      	logger.log("Method = "+method);
      	WebLogin login = WebLogin.getLogin(request);
      	if (login == null || login.isLoggedIn() == false) {
         	if (method.equals("POST")) {
            	logger.log("Authenticating...");
            	login =  WebLogin.Authenticate(request, response, Permission.user);
         	}
         	else if (login == null) {
            	logger.log("create dummy login...");
            	login = new WebLogin(request.getRequestURI());
         	}
      	}	
      	else {
         	logger.log("logged in as"+login.getUserName());
      	}

      	response.setContentType("text/html");
      	PrintWriter out = response.getWriter();

      	if (login.isLoggedIn() == false) {
         	logger.log("display login form...");
         	out.println(simplePage(login.getLoginForm()));
         	return;
      	}
            
      	logger.log("Got login session "+login);      
     	String user = WebLogin.getLoginUser(request);

     	Map<String, String> postData = WebLogin.getBody(request);
     	String db = request.getParameter("db");
        MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);

        logger.log("Get sign using signID"+postData.get("signID"));
        if (postData.get("signID") != null) {
        	Sign sign = mongo.getSignDB().getSign(postData.get("signID"));
            ParkingSchedule verifiedSchedule = new ParkingSchedule(postData, logger);
            sign.setParkingSchedule(verifiedSchedule);
            if (postData.get("street") != null && postData.get("streetNumber") != null) {
            	logger.log("position before address change "+sign.getPosition());
            	if (sign.updatePosition(postData)) {
            		logger.log("position after address change "+sign.getPosition());
            	}
            	else {
                StringBuilder sb = new StringBuilder(10);
                sb.append("failed to get new position from new street = "+postData.get("street")+" "+postData.get("streetNumber"));
                if (postData.get("city") != null) {
                  sb.append(" city = "+postData.get("city"));
                }
                Address a = sign.getAddress();
                if (a != null) {
                  sb.append(" orig address = "+a.toString());
                }
            		logger.error(sb.toString());
            	}
            }
            if (sign.getPosition() == null) {
              mongo.getSignDB().removeSign(sign);
            }
            else {
              mongo.getSignDB().updateSign(sign, login.getUserName());
            }
            
            logger.log("Verified schedule is "+verifiedSchedule);
            String returnUrl = postData.get("returnUrl");
            boolean auto = postData.get("auto") == null ? true : Boolean.parseBoolean(postData.get("auto"));
            if ( auto ) { // show map for autogen sshecule           
              out.println(getGoogleMap(returnUrl, port, user, sign));
              logger.log("Displayed goolge map");
            }
            else {
              response.sendRedirect(response.encodeRedirectURL(returnUrl));
            }
        }
        else {
            logger.error("could not take action with postData:");
            for ( String key : postData.keySet()) {
               logger.error(key+" = "+postData.get(key));
            }
        }
	}

	private String getGoogleMap(String url, int port, String userName, Sign sign) {
      StringBuilder sb = new StringBuilder(100);
      sb.append("<!DOCTYPE html>");
      sb.append("<html>");
      sb.append("<head>");
      sb.append("<style type=\"text/css\"> #map{ width:700px; height: 500px; }</style>");
      sb.append("<script type=\"text/javascript\" src=\"https://maps.googleapis.com/maps/api/js?key=AIzaSyDni-ZQemF7eA1P-A76acHMF2tREyFM3HI\"></script>");      
      sb.append("<body>");
      sb.append("<h2>If required, reposition the sign by dragging it on the map</h2>");
      if (sign.getPosition() == null) {
        sb.append("<h2>ERROR: cannot locate the sign on the map because no address or gps position was provided. Please retry operation</h2>");
      }
      sb.append("<form action=\"" + url + "\" method=\"POST\" enctype=\"application/x-www-form-urlencoded\">");
      sb.append("<input type=\"hidden\" name=\"port\" id=\"port\" value=\""+port+"\"><br>");
      sb.append("<input type=\"hidden\" name=\"user\" id=\"user\" value=\""+userName+"\"><br>");
      if (sign.getPosition() != null) {
        sb.append("<input type=\"hidden\" name=\"lat\" id=\"cenLat\" value=\""+sign.getPosition().getLatitude()+"\"><br>");
        sb.append("<input type=\"hidden\" name=\"lng\" id=\"cenLng\" value=\""+sign.getPosition().getLongitude()+"\"><br>");
      }
      sb.append("<input type=\"hidden\" name=\"signNum\" id=\"signNum\" value=\""+sign.getID()+"\"><br>");
      sb.append("<input type=\"hidden\" name=\"ignoreFile\" value=\"true\"><br>");
      sb.append("<input type=\"submit\" name=\"submit\" value=\"Done\" />");
      sb.append("<div id=\"map\"></div>");
      sb.append("<script type=\"text/javascript\" src=\"../js/map.js\"></script>");
      sb.append("</body>");
      sb.append("</html>");
      return sb.toString();
   }

   private String simplePage(String content) {
      StringBuilder sb = new StringBuilder(100);
      sb.append("<html>");
      sb.append("<head>");
      sb.append("<title>Servlet upload</title>");  
      sb.append("</head>");
      sb.append("<body>");
      sb.append("<p>"+content+"</p>"); 
      sb.append("</body>");
      sb.append("</html>");
      return sb.toString();
   }

}
package parking.admin;

import parking.schedule.ParkingSchedule;
import parking.schedule.SignSchedule;
import parking.database.MongoInterface;
import parking.security.WebLogin;
import parking.security.User.Permission;
import parking.map.Sign;
import parking.map.Address;
import parking.map.Trajectory;
import parking.map.Position;
import parking.map.MapInit;
import parking.util.ClientType;
import parking.util.Logger;
import parking.util.LoggingTag;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;

public class VerifyServlet extends HttpServlet {

  private MongoInterface mongo;
  private WebLogin login;
  private PrintWriter out;
  private Logger logger;
  private int port;

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger = new Logger(request.getSession(), LoggingTag.Servlet, this, "service");
    MapInit mapInit = new MapInit();
		String method = request.getMethod();
		port = request.getServerPort();
    String userAgent = request.getHeader("User-Agent");
    ClientType client = ClientType.getClientType(userAgent);
    logger.log("Method = "+method);
    Map<String, String> postData = null;
    if (method.equals("POST")) {
      postData = WebLogin.getBody(request);
      logger.log("got "+postData.size()+" keys from form");
    }
    login = WebLogin.authenticate(request);
      	
    response.setContentType("text/html");
    out = response.getWriter();

    if (login.isLoggedIn() == false) {
        login.setResponseNotAuthorized(response);         
        return;
    }
            
    logger.log("Got login session "+login);      
    

//    Map<String, String> postData = WebLogin.getBody(request);
    String db = request.getParameter("db");
    mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);

    logger.log("Get sign using signID "+postData.get("signID"));
    if (postData.get("signID") != null) {
        Sign sign = mongo.getSignDB().getSign(postData.get("signID"));
        ParkingSchedule verifiedSchedule = null;
        try {
            if (client == ClientType.BROWSER) {
                verifiedSchedule = addVerifiedScheduleFromBrowser(sign, postData, response);  
                
            }
            else {
                verifiedSchedule = addVerifiedScheduleFromApp(sign, postData);          
            }
         }
        catch (Exception ex) {
              logger.error("Got exception "+ex);
              ex.printStackTrace();
        } 
        if (verifiedSchedule != null) {          
              logger.log("Verified schedule is "+verifiedSchedule);
              response.setStatus(HttpServletResponse.SC_OK );
        }
        else {
              logger.error("Verified schedule is not found");
              response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
        }
    }
    else {
            logger.error("could not take action with postData:");
            for ( String key : postData.keySet()) {
               logger.error(key+" = "+postData.get(key));
            }
    }
	}

  private ParkingSchedule addVerifiedScheduleFromApp(Sign sign, Map<String, String> postData) throws JSONException {
    ParkingSchedule verifiedSchedule = null;
       
              String json = postData.get("verified");
              JSONObject jObj = new JSONObject(json);
              SignSchedule verified = new SignSchedule(jObj);
              if (verified.schedule != null) {
                  verifiedSchedule = new ParkingSchedule(verified.schedule);
                  sign.setParkingSchedule(verifiedSchedule);
              }
              if (verified.place != null) {
                  Trajectory trajectory = mongo.getTrajectoryDB().getTrajectory(sign.getID());
                  double startTime = mongo.getTrajectoryDB().getStart(sign.getID());
                  double endTime = mongo.getTrajectoryDB().getEnd(sign.getID());
                  Position signPosition = Sign.resolvePosition(verified.place, trajectory, startTime, endTime);                 
                  sign.setPosition(signPosition);
              }
              mongo.getSignDB().updateSign(sign, login.getUserName());
              
              return verifiedSchedule;
       
  }

  private ParkingSchedule addVerifiedScheduleFromBrowser(Sign sign, Map<String, String> postData, HttpServletResponse response) throws IOException {
      for (String key : postData.keySet()) {
          logger.log(key+" = "+postData.get(key));
      }
      ParkingSchedule verifiedSchedule = new ParkingSchedule(postData);
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
      mongo.getSignDB().updateSign(sign, login.getUserName());  
      String returnUrl = postData.get("returnUrl");
      boolean auto = postData.get("auto") == null ? true : Boolean.parseBoolean(postData.get("auto"));
      if ( auto ) { // show map for autogen sshecule           
          out.println(getGoogleMap(returnUrl, port, login.getUserName(), sign));
          logger.log("Displayed goolge map");
      }
      else {
          response.sendRedirect(response.encodeRedirectURL(returnUrl));
      }
      return verifiedSchedule;
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
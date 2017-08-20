package parking.admin;

import parking.security.WebLogin;
import parking.database.MongoInterface;
import parking.security.Password;
import parking.security.User.Permission;

import parking.map.Sign;
import parking.map.MapInit;
import parking.schedule.ParkingSchedule;

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

public class MapServlet extends HttpServlet {


	private final String script = "function initialize() {\n" +
										"var mapProp = {\n" +
											"center:new google.maps.LatLng(45.46, 286.37),\n" +
											" zoom:16,\n" +
											"mapTypeId:google.maps.MapTypeId.ROADMAP\n" +
										"};\n" +
										"var map=new google.maps.Map(document.getElementById(\"googleMap\"), mapProp);\n" +
									"}\n" +
									"google.maps.event.addDomListener(window, 'load', initialize);\n";
						 

	private MongoInterface mongo;

	private String getForm(String processUri, String verifyUri, int port, String userName) {
		String formHTML = "<form action=\"" + processUri + "\" method=\"POST\" enctype=\"multipart/form-data\">" +
				"Sign Picture: <input type=\"file\" id=\"pic\" name=\"pic\" accept=\"image/*\" /><br>" +
				"<input type=\"text\" name=\"lat\" id=\"lat\" readonly=\"yes\"><br>" +
				"<input type=\"text\" name=\"lng\" id=\"lng\" readonly=\"yes\"><br>" +
				"<input type=\"hidden\" name=\"verify\" value=\""+verifyUri+"\"><br>" +	
				"<input type=\"hidden\" name=\"port\" id=\"port\" value=\""+port+"\"><br>" +
				"<input type=\"hidden\" name=\"user\" id=\"user\" value=\""+userName+"\"><br>" +						
				"<input type=\"submit\" name=\"submit\" value=\"Upload\" />" +
				"</form>";
		return formHTML;
	}

	private void showGoogleMap(PrintWriter out, String processUri, String verifyUri, int port, String userName) {
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<style type=\"text/css\"> #map{ width:1000px; height: 800px; }</style>");
		out.println("<script type=\"text/javascript\" src=\"https://maps.googleapis.com/maps/api/js?key=AIzaSyDni-ZQemF7eA1P-A76acHMF2tREyFM3HI\"></script>");
		
		out.println("<body>");
		out.println("<h1>Upload a picture of a sign to the database OR click on a sign on the map to edit it</h1>");
		out.println("<div id=\"editForm\"></div>");
		out.println("<div id=\"map\"></div>");
		out.println("<h2>Step 1 - click the location of the sign on the map</h2>");	
		out.println("<h2>Step 2 - choose the image file to upload</h2>");
		out.println(getForm(processUri, verifyUri, port, userName));			
		out.println("<h2>Important Note: be a as precise as possible selecting the sign location on the map. Zoom in if necessary</h2>");	
		out.println("<script type=\"text/javascript\" src=\"js/map.js\"></script>");
		out.println("<div id=\"verificationForm\"></div>");
		out.println("<img id=\"image\">");
		out.println("</body>");
		out.println("</html>");
	}

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Logger logger = new Logger(request.getSession(), LoggingTag.Servlet, this, "service");
		MapInit mapInit = new MapInit();
		String method = request.getMethod();
		String userAgent = request.getHeader("User-Agent");
      	logger.log("request from "+userAgent);
      	
		int port = request.getServerPort();
		Map<String,String> postData = null;
		if (method.equals("POST")) {
			postData = WebLogin.getBody(request);
		}
		WebLogin login = WebLogin.authenticate(request);
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
/*		
		WebLogin login = null;
		if (postData == null) {
			logger.log( "Authenticating...");
			login =  WebLogin.Authenticate(request, response, Permission.user);
		}
		else {
			 login = WebLogin.Authenticate(request, response, Permission.user, postData);
		}
*/		
		if (login.isLoggedIn() == false) {        
         	login.setResponseNotAuthorized(response);         
         	return;
      	}
		logger.log("Got login session "+login);
		String db = request.getParameter("db");
/*		
		if (login.isLoggedIn() == false) {
			out.println("<!DOCTYPE html>");
			out.println("<html>");
			out.println("<body>");
			out.println(login.getLoginForm());
			out.println("</body>");
			out.println("</html>");
			return;
		}
*/		
		mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
		if (postData != null) {
			String signID = postData.get("signID");
			logger.log("processing post data for sign "+signID);
			if (signID != null) {
				Sign sign = mongo.getSignDB().getSign(signID);
				if (sign != null) {
//					ParkingSchedule verifiedSchedule = new ParkingSchedule(postData, logger);
					ParkingSchedule verifiedSchedule = new ParkingSchedule(postData);
					sign.setParkingSchedule(verifiedSchedule);
					mongo.getSignDB().updateSign(sign, login.getUserName());
					logger.log(verifiedSchedule.toString());
				}
				else {
					logger.error("could not find sign with signID "+signID);
				}
			}
			else {
				logger.error("no signID found in post");
			}
		}
		logger.log("MapServer displaying map...");
		String uploadSignUri = request.getRequestURI() + "/upload";		
		showGoogleMap(out, uploadSignUri, request.getRequestURI(), port, login.getUserName());
	}
}
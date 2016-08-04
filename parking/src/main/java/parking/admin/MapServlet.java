package parking.admin;

import parking.security.WebLogin;
import parking.database.MongoInterface;
import parking.security.Password;
import parking.security.User.Permission;

import parking.map.Sign;
import parking.schedule.ParkingSchedule;

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

	private String getForm(String processUri, String verifyUri, int port) {
		String formHTML = "<form action=\"" + processUri + "\" method=\"POST\" enctype=\"multipart/form-data\">" +
				"Sign Picture: <input type=\"file\" id=\"pic\" name=\"pic\" accept=\"image/*\" /><br>" +
				"<input type=\"text\" name=\"lat\" id=\"lat\" readonly=\"yes\"><br>" +
				"<input type=\"text\" name=\"lng\" id=\"lng\" readonly=\"yes\"><br>" +
				"<input type=\"hidden\" name=\"verify\" value=\""+verifyUri+"\"><br>" +	
				"<input type=\"hidden\" name=\"port\" id=\"port\" value=\""+port+"\"><br>" +						
				"<input type=\"submit\" name=\"submit\" value=\"Upload\" />" +
				"</form>";
		return formHTML;
	}

	private void showGoogleMap(PrintWriter out, String processUri, String verifyUri, int port) {
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<style type=\"text/css\"> #map{ width:700px; height: 500px; }</style>");
		out.println("<script type=\"text/javascript\" src=\"https://maps.googleapis.com/maps/api/js?key=AIzaSyDni-ZQemF7eA1P-A76acHMF2tREyFM3HI\"></script>");
		
		out.println("<body>");
		out.println("<h1>Upload a picture of a sign to the database</h1>");
		out.println("<div id=\"map\"></div>");
		out.println("<h2>Step 1 - click the location of the sign on the map</h2>");	
		out.println("<h2>Step 2 - choose the image file to upload</h2>");
		out.println(getForm(processUri, verifyUri, port));			
		out.println("<h2>Important Note: be a as precise as possible selecting the sign location on the map. Zoom in if necessary</h2>");	
		out.println("<script type=\"text/javascript\" src=\"js/map.js\"></script>");
		out.println("<div id=\"verificationForm\"></div>");
		out.println("</body>");
		out.println("</html>");
	}

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String method = request.getMethod();
		int port = request.getServerPort();
		Map<String,String> postData = null;
		if (method.equals("POST")) {
			postData = WebLogin.getBody(request);
		}
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		WebLogin login = null;
		if (postData == null) {
			login =  WebLogin.Authenticate(request, response, Permission.user);
		}
		else {
			 login = WebLogin.Authenticate(request, response, Permission.user, postData);
		}
		String db = request.getParameter("db");
		mongo = MongoInterface.getInstance(db, request.getServerPort());
		if (postData != null) {
			System.out.println("MapServer processing post data...");
			String tag = postData.get("tag");
			if (tag != null) {
				Sign sign = mongo.getSignDB().getSignFromTag(tag);
				if (sign != null) {
					ParkingSchedule verifiedSchedule = new ParkingSchedule(postData);
					sign.setParkingSchedule(verifiedSchedule);
					mongo.getSignDB().updateSign(sign);
					System.out.println(verifiedSchedule);
				}
				else {
					System.out.println("ERROR: could not find sign with tag "+tag);
				}
			}
			else {
				System.out.println("ERROR: no tag found in post");
			}
		}
		System.out.println("MapServer displaying map...");
		String uploadSignUri = request.getRequestURI() + "/upload";		
		showGoogleMap(out, uploadSignUri, request.getRequestURI(), port);
	}
}
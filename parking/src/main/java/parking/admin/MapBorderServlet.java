package parking.admin;

import parking.security.WebLogin;
import parking.database.MongoInterface;
import parking.security.Password;
import parking.security.User.Permission;

import parking.map.Position;
import parking.map.MapInit;

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

public class MapBorderServlet extends HttpServlet {


	private final String script = "function initialize() {\n" +
										"var mapProp = {\n" +
											"center:new google.maps.LatLng(45.4337, -73.6905),\n" +
											" zoom:16,\n" +
											"mapTypeId:google.maps.MapTypeId.ROADMAP\n" +
										"};\n" +
										"var map=new google.maps.Map(document.getElementById(\"googleMap\"), mapProp);\n" +
									"}\n" +
									"google.maps.event.addDomListener(window, 'load', initialize);\n";
						 

	private MongoInterface mongo;

	private String getForm(String processUri, int port, String userName, String region) {
		StringBuilder sb = new StringBuilder(10);
		sb.append("<form action=\"" + processUri + "\" method=\"POST\">");
		sb.append("<input type=\"hidden\" name=\"port\" id=\"port\" value=\""+port+"\">");
		sb.append("<input type=\"hidden\" name=\"user\" id=\"user\" value=\""+userName+"\">");
		sb.append("<input type=\"hidden\" name=\"BorderId\" id=\"BorderId\">");
        sb.append("<input type=\"button\" onclick=\"setAddPointMode()\" id=\"AddPoint\" value=\"AddPoint\">");
        sb.append("<input type=\"button\" onclick=\"setInsertPointMode()\" id=\"InsertPoint\" value=\"InsertPoint\">");
        sb.append("<input type=\"button\" onclick=\"setViewMode()\" id=\"View\" value=\"View\">");
        sb.append("<input type=\"button\" onclick=\"setSelectMode()\" id=\"Select\" value=\"Select\">");
        sb.append("<input type=\"button\" onclick=\"getAddress()\" id=\"Address\" value=\"Address\" /><br>");
        sb.append("<input type=\"button\" onclick=\"undo()\" id=\"Undo\" value=\"Undo\">");
        sb.append("<input type=\"button\" onclick=\"saveBorder()\" id=\"Save\" value=\"Save\" /><br>");
        if (region == null) {
        	sb.append("Region Name: <input type=\"text\" name=\"RegionName\" id=\"RegionName\"><br>");
        	sb.append("<input type=\"submit\" name=\"SetRegion\" value=\"SetRegion\" />");
        }
        else {
        	sb.append("Region Name: <input type=\"text\" name=\"RegionName\" id=\"RegionName\" value=\""+region+"\"><br>");
        	sb.append("<input type=\"submit\" name=\"Publish\" value=\"Publish\" />");
        }							
		sb.append("</form>");
		return sb.toString();
	}

	private void showGoogleMap(PrintWriter out, String processUri, int port, String userName, String region) {
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<style type=\"text/css\"> #map{ width:1000px; height: 800px; }</style>");
		out.println("<script type=\"text/javascript\" src=\"https://maps.googleapis.com/maps/api/js?key=AIzaSyDni-ZQemF7eA1P-A76acHMF2tREyFM3HI\"></script>");
		
		out.println("<body>");
		out.println("<div id=\"topForm\"></div>");
		out.println("<div id=\"map\"></div>");
		out.println("<div id=\"statusMessage\"></div>");
		out.println(getForm(processUri, port, userName, region));				
		out.println("<script type=\"text/javascript\" src=\"js/editBorder.js\"></script>");
		out.println("<div id=\"editForm\"></div>");
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
		
		if (login.isLoggedIn() == false) {        
         	login.setResponseNotAuthorized(response);         
         	return;
      	}
		logger.log("Got login session "+login);
		String db = request.getParameter("db");	
		mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
		String user = login.getUserName();
		String region = null;
		if (postData != null) {
			region = postData.get("RegionName");
			String id = postData.get("BorderId");
			if ( region != null && id != null ) {
				if(!mongo.getMapEditDB().publishBorder(id, region)) {
					logger.error("Failed to publish border "+id+" for "+region);
				}

			}	
		}
		showGoogleMap(out, request.getRequestURI(), port, user, region);
	}
}
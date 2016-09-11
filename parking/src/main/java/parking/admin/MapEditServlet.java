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

public class MapEditServlet extends HttpServlet {


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

	private String getForm(String processUri, int port, String userName, String selectedSegmentId, Position mapCenter) {
		StringBuilder sb = new StringBuilder(10);
		sb.append("<form action=\"" + processUri + "\" method=\"POST\">");
		sb.append("<input type=\"hidden\" name=\"port\" id=\"port\" value=\""+port+"\">");
		sb.append("<input type=\"hidden\" name=\"selectedSegmentId\" id=\"selectedSegmentId\" value=\""+selectedSegmentId+"\">");
		sb.append("<input type=\"hidden\" name=\"user\" id=\"user\" value=\""+userName+"\">");
		sb.append("<input type=\"hidden\" name=\"lat\" id=\"cenLat\" value=\""+mapCenter.getLatitude()+"\">");
        sb.append("<input type=\"hidden\" name=\"lng\" id=\"cenLng\" value=\""+mapCenter.getLongitude()+"\">");
        sb.append("<input type=\"button\" onclick=\"setSelectMode()\" id=\"SelectSegment\" value=\"SelectSegment\">");
        sb.append("<input type=\"button\" onclick=\"setViewMode()\" id=\"ViewSegment\" value=\"ViewSegment\">");
        sb.append("<input type=\"button\" onclick=\"setAddPointMode()\" id=\"AddPoint\" value=\"AddPoint\">");
        sb.append("<input type=\"button\" onclick=\"setAddCornerMode()\" id=\"AddCorner\" value=\"AddCorner\">");
// not required        sb.append("<input type=\"button\" onclick=\"saveSegment()\" id=\"Save\" value=\"Save\" /><br>");
// not required        sb.append("<input type=\"button\" onclick=\"undoUnsavedEdits()\" id=\"UndoEdits\" value=\"UndoEdits\" />");
        sb.append("<input type=\"button\" onclick=\"clearSegment()\" id=\"ClearSegment\" value=\"ClearSegment\" /><br>");
//        sb.append("<input type=\"submit\" name=\"PublishSaved\" value=\"PublishSaved\" />");
        sb.append("<input type=\"submit\" name=\"PublishSelected\" value=\"PublishSelected\" />");							
		sb.append("</form>");
		return sb.toString();
	}

	private void showGoogleMap(PrintWriter out, String processUri, int port, String userName, String selectedSegmentId, Position mapCenter) {
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<style type=\"text/css\"> #map{ width:1000px; height: 800px; }</style>");
		out.println("<script type=\"text/javascript\" src=\"https://maps.googleapis.com/maps/api/js?key=AIzaSyDni-ZQemF7eA1P-A76acHMF2tREyFM3HI\"></script>");
		
		out.println("<body>");
		out.println("<div id=\"topForm\"></div>");
		out.println("<div id=\"map\"></div>");
		out.println("<div id=\"statusMessage\"></div>");
		out.println(getForm(processUri, port, userName, selectedSegmentId, mapCenter));				
		out.println("<script type=\"text/javascript\" src=\"js/editMap.js\"></script>");
		out.println("<div id=\"editForm\"></div>");
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
	
		if (login.isLoggedIn() == false) {        
         	login.setResponseNotAuthorized(response);         
         	return;
      	}
		logger.log("Got login session "+login);
		String db = request.getParameter("db");	
		mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
		String user = login.getUserName();
		Position mapCenter = null;
		String selectedSegmentId = "";
		boolean published = false;
		if (postData != null) {
			for (String key : postData.keySet()) {
				logger.log("got post entry "+key+" = "+postData.get(key));
			}			
			if (postData.get("lat") != null && postData.get("lng") != null) {
				double lat = Double.parseDouble(postData.get("lat"));		
				double lng = Double.parseDouble(postData.get("lng"));
				mapCenter = new Position(lat, lng);
				logger.log("got position = "+mapCenter+" from POST");
			}
			if (postData.get("selectedSegmentId") != null) {
				selectedSegmentId = postData.get("selectedSegmentId");
				logger.log("got selected segment id "+selectedSegmentId);
			}
			if (postData.get("PublishSelected") != null) {
				if (selectedSegmentId!= null && !selectedSegmentId.equals("")) {
					if(!mongo.getMapEditDB().publishSegment(selectedSegmentId)) {
						logger.error("Failed to publish segment "+selectedSegmentId);
					}
				}
			}
			else if (postData.get("PublishSaved") != null) {
				if (!mongo.getMapEditDB().publishSavedSegments()) {
					logger.error("Failed to publish saved segments ");
				}
			}
		}
		if (published) {
			logger.log("Data was published");
		}
        Position workPosition = mongo.getMapEditDB().findWork(user);
        logger.log("got position = "+workPosition+" from edit DB");
        if (workPosition == null) {
        	workPosition = mongo.getSignDB().findWork();
        	logger.log("got position = "+workPosition+" from sign DB");
        }
        mapCenter = workPosition == null ? mapCenter : workPosition;        	
		if (mapCenter == null) {
			mapCenter = new Position(45.501691, -73.567256); // Montreal
		}
		logger.log("MapServer displaying map...");	
		showGoogleMap(out, request.getRequestURI(), port, user, selectedSegmentId, mapCenter);
	}
}
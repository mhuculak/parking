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

public class TestServlet extends HttpServlet {


	private final String script = "function initialize() {\n" +
										"var mapProp = {\n" +
											"center:new google.maps.LatLng(45.46, 286.37),\n" +
											" zoom:16,\n" +
											"mapTypeId:google.maps.MapTypeId.ROADMAP\n" +
										"};\n" +
										"var map=new google.maps.Map(document.getElementById(\"googleMap\"), mapProp);\n" +
									"}\n" +
									"google.maps.event.addDomListener(window, 'load', initialize);\n";
						 

	private void showGoogleMap(PrintWriter out) {
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<style type=\"text/css\"> #map{ width:700px; height: 500px; }</style>");
		out.println("<script type=\"text/javascript\" src=\"https://maps.googleapis.com/maps/api/js?key=AIzaSyDni-ZQemF7eA1P-A76acHMF2tREyFM3HI\"></script>");
		
		out.println("<body>");
		out.println("<h1>Test Google Maps</h1>");
		out.println("<div id=\"map\"></div>");
		out.println("<script type=\"text/javascript\" src=\"js/testmap.js\"></script>");
		out.println("</body>");
		out.println("</html>");
	}

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String method = request.getMethod();
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();	
		showGoogleMap(out);
	}
}
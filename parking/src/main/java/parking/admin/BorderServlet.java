package parking.admin;

import parking.database.MongoInterface;
import parking.map.Position;
import parking.map.Address;
import parking.map.MapBorder;
import parking.map.MapBounds;
import parking.map.MapInit;
import parking.security.WebLogin;

import parking.util.Logger;
import parking.util.LoggingTag;

//import org.codehaus.jackson.map.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class BorderServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Logger logger = new Logger(request.getSession(), LoggingTag.Servlet, this, "service");
		MapInit mapInit = new MapInit();
		String method = request.getMethod();
		Map<String,String> postData = null;
		if (method.equals("POST")) {
			postData = WebLogin.getBody(request);
		}
		WebLogin login = WebLogin.authenticate(request);
		if (login.isLoggedIn() == false) {        
         	login.setResponseNotAuthorized(response);         
         	return;
      	}
		String db = request.getParameter("db");				
		PrintWriter out = response.getWriter();
		response.setContentType("text/plain");
		
		MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
		
		if (method.equals("POST")) {
			String action = postData.get("action");
			String lat = postData.get("lat");
			String lng = postData.get("lng");
			String olat = postData.get("olat");
			String olng = postData.get("olng");
			String formUser = postData.get("user");
			String id = postData.get("id");
			if (formUser != null && !formUser.equals(login.getUserName())) {
				logger.error("ERROR: inconsistant user from form "+formUser+" and login "+login.getUserName());
			}
			if (action != null) {
				logger.log("action = "+action);
				Position p = null;
				if (lat != null && lng != null) {
					p = new Position(lat, lng);
				}
				Position orig = null;
				if (olat != null && olng != null) {
					orig = new Position(olat, olng);
				}
				if (action.equals("MovePoint")) {
					if (p != null && orig != null && id != null) {
						logger.log("Border "+id+" move point "+orig+" to "+p);
						if (!mongo.getMapEditDB().movePoint(id, orig, p)) {
							logger.error("ERROR: unable to move point "+p.toString()+" for border id "+id);
						}
					}
					else {
						logger.error("ERROR: invalid data for action "+action);
					}
				}
				else if (action.equals("SaveBorder")) {
					String region = postData.get("region");
					logger.log("region = "+region);
					if (postData.get("points") !=null && region != null) {
						logger.log("parse points = "+postData.get("points"));					
						List<Position> points = SegmentServlet.pListFromString(postData.get("points"));
						logger.log("found "+points.size()+" points");
						if (id != null) {
							logger.log("save points for id = "+id);								
							mongo.getMapEditDB().saveBorder(id, region, points);
						}
						else {
							logger.log("save points for region = "+region+" user = "+login.getUserName());	
							id = mongo.getMapEditDB().saveBorder(region, points, login.getUserName());

						}
						logger.log("sending response "+id);
						out.println(id);
					}
					else {
						logger.error("ERROR: no data found for action "+action);
					}
				}
			}
		}
		else {
			String action = request.getParameter("action");	
			logger.log("process action "+action);	
			String json = null;
			try {				
				ObjectMapper mapper = new ObjectMapper();
				if (action.equals("find")) {
					String nela = request.getParameter("nela");
					String nelg = request.getParameter("nelg");
					String swla = request.getParameter("swla");
					String swlg = request.getParameter("swlg");
					Position ne = new Position( nela, nelg );
					Position sw = new Position( swla, swlg );
					logger.log(action+" with bounds "+ne+" to "+sw);
					MapBounds bounds = new MapBounds( ne, sw);
					List<MapBorder> borders = mongo.getMapEditDB().getBorders(bounds, login.getUserName());									
					if (borders != null && borders.size() > 0) {
						logger.log("returning "+borders.size()+" to client");
						json = mapper.writeValueAsString(borders);
						out.println(json);
						return;
					}
					logger.log("no borders found");									
				}
				else if (action.equals("select")) {
					String sela = request.getParameter("sela");
					String selg = request.getParameter("selg");
					String id = request.getParameter("id");					
					MapBorder border = null;
					if (id != null) {
						logger.log(action+" with id "+id);
						border = mongo.getMapEditDB().getBorder(id);
					}
					else if ( sela != null && selg != null) {
						Position p = new Position(sela, selg);
						logger.log(action+" at pos "+p);
					 	border = mongo.getMapEditDB().selectBorder(p, login.getUserName());
					}
					else {
						logger.error("Unable to process "+action);
					}
					if (border != null) {
						ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
						json = ow.writeValueAsString(border);
//						json = mapper.writeValueAsString(border);
						logger.log("json ="+ json);
						out.println(json);
						return;
					}
					else {
						logger.error("no segment selected from DB");
					}
				}
				else if (action.equals("address")) {
					String sela = request.getParameter("sela");
					String selg = request.getParameter("selg");
					if ( sela != null && selg != null) {
						Position p = new Position(sela, selg);
						Address a = Address.reverseGeocode(p);
						out.println(a.toString());
						return;
					}
				}
				response.setStatus(HttpServletResponse.SC_NO_CONTENT );
			}
			catch (Exception ex) {
				logger.error("Caught "+ex);
				ex.printStackTrace();
			}
		}
	}
}
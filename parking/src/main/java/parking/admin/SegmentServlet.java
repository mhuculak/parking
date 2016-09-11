package parking.admin;

import parking.database.MongoInterface;
import parking.map.SignMarker;
import parking.map.Sign;
import parking.map.Position;
import parking.map.MapBounds;
import parking.map.StreetSegment;
import parking.map.MapInit;
import parking.security.WebLogin;

import parking.util.Logger;
import parking.util.LoggingTag;

import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class SegmentServlet extends HttpServlet {

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
				if (action.equals("AddPoint")) {
					if (p != null && id != null) {
						if (!mongo.getMapEditDB().addPoint(id, p, login.getUserName())) {
							logger.error("ERROR: unable to add point "+p.toString()+" for segment id "+id);
						}
					}
					else {
						logger.error("ERROR: invalid data for action "+action);
					}
				}
				else if (action.equals("AddCorner")) {
					if (p != null && id != null) {
						if (!mongo.getMapEditDB().addCorner(id, p, login.getUserName())) {
							logger.error("ERROR: unable to add corner "+p.toString()+" for segment id "+id);
						}
					}
					else {
						logger.error("ERROR: invalid data for action "+action);
					}
				}
				else if (action.equals("MovePoint")) {
					if (p != null && orig != null && id != null) {
						logger.log("Segment "+id+" move point "+orig+" to "+p);
						if (!mongo.getMapEditDB().movePoint(id, orig, p)) {
							logger.error("ERROR: unable to move point "+p.toString()+" for segment id "+id);
						}
					}
					else {
						logger.error("ERROR: invalid data for action "+action);
					}
				}
				else if (action.equals("MoveCorner")) {
					if (p != null && orig != null && id != null) {
						if (!mongo.getMapEditDB().moveCorner(id, orig, p, login.getUserName())) {
							logger.error("ERROR: unable to move corner "+p.toString()+" for segment id "+id);
						}
					}
					else {
						logger.error("ERROR: invalid data for action "+action);
					}
				}
				else if (action.equals("SaveSegment")) {
					if (postData.get("points") !=null || postData.get("corners") != null) {
						logger.log("parse "+postData.get("points"));						
						List<Position> points = pListFromString(postData.get("points"));
						logger.log("got "+points.size()+" points");
						List<Position> corners = pListFromString(postData.get("corners"));		
						logger.log("save lists in DB");			
						mongo.getMapEditDB().saveSegment(id, points, corners);
					}
					else {
						logger.error("ERROR: no data found for action "+action);
					}
				}
				else if (action.equals("ClearSegment")) {
					if (!mongo.getMapEditDB().clearSegment(id)) {
						logger.error("failed to remove any data from segment "+id);
					}
				}
				else {
					logger.error("Unsupported action "+action);
				}
			}
			else {
				logger.error("No action provided in POST");
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
					List<StreetSegment> segments = mongo.getMapEditDB().getStreetSegments(bounds, login.getUserName());
					if (segments == null) {
						logger.log("no segments found in the edit DB");
						segments = mongo.getSignDB().getStreetSegments(bounds);
						if (segments != null) {
							logger.log("adding "+segments.size()+" segments to the edit DB");
							mongo.getMapEditDB().addSegmentsAsVisible(segments, login.getUserName());
						}
						else {
							logger.log("no segments found in the sign DB");
						}								
					}					
					if (segments != null && segments.size() > 0) {
						logger.log("returning "+segments.size()+" to client");
						json = mapper.writeValueAsString(segments);
						out.println(json);
						return;
					}
					logger.log("no segments found");									
				}
				else if (action.equals("select")) {
					String sela = request.getParameter("sela");
					String selg = request.getParameter("selg");
					String id = request.getParameter("id");					
					StreetSegment segment = null;
					if (id != null) {
						logger.log(action+" with id "+id);
						segment = mongo.getMapEditDB().getSegment(id);
					}
					else if ( sela != null && selg != null) {
						Position p = new Position(sela, selg);
						logger.log(action+" at pos "+p);
					 	segment = mongo.getMapEditDB().select(p, login.getUserName());
					}
					else {
						logger.error("Unable to process "+action);
					}
					if (segment != null) {
						json = mapper.writeValueAsString(segment);
						logger.log("json ="+ json);
						out.println(json);
						return;
					}
					else {
						logger.error("no segment selected from DB");
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

	public static List<Position> pListFromString(String value) {
		if (value == null) {
			return null;
		}
		List<Position> pList = new ArrayList<Position>();
		String[] vals = value.split(":");
		for (int i=0 ; i<vals.length ; i++) {
			Position p = new Position(vals[i]);
			pList.add(p);
		}
		return pList;
	}
}
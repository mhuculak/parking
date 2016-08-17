package parking.admin;

import parking.database.MongoInterface;
import parking.map.SignMarker;
import parking.map.Sign;
import parking.map.Position;
import parking.map.MapBounds;
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

public class SignServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Logger logger = new Logger(request.getSession(), LoggingTag.Servlet, this, "service");
		String method = request.getMethod();
		Map<String,String> postData = null;
		if (method.equals("POST")) {
			postData = WebLogin.getBody(request);

		}
		String db = request.getParameter("db");
		String id = request.getParameter("id");
		
		String nela = request.getParameter("nela");
		String nelg = request.getParameter("nelg");
		String swla = request.getParameter("swla");
		String swlg = request.getParameter("swlg");

		String signNum = request.getParameter("signNum");	

		PrintWriter out = response.getWriter();
		response.setContentType("text/plain");
		
		MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
		if (method.equals("POST")) {
			Position p = new Position (postData.get("lat"), postData.get("lng"));
			String user = postData.get("user");
			logger.log("move sign id " + postData.get("id") + " to " + p.toString());			
			boolean ok = mongo.getSignDB().moveSign(postData.get("id"), p, user);
			if (ok) {
				out.println("success");
			}
			else {
				out.println("failed");
			}
		}
		else { // GET
			String json = null;
			try {
				ObjectMapper mapper = new ObjectMapper();
				
				if ( id != null) {
					Sign sign = mongo.getSignDB().getSign(id);
					String signDetails = sign.displayText();
					json = mapper.writeValueAsString(signDetails);
				}
				else if ( signNum != null) {
					List<SignMarker> signs = new ArrayList<SignMarker>();
					SignMarker marker = mongo.getSignDB().getSignMarker(signNum);
					signs.add(marker);
//					System.out.println("got list of "+signs.size()+" sign markers");
					json = mapper.writeValueAsString(signs);
				}			
				else {
					int max = 5;
//					logger.log("nela="+nela+" nelg="+nelg+" swla="+swla+" swlg = "+swlg);
					Position ne = new Position( nela, nelg );
					Position sw = new Position( swla, swlg );
					MapBounds bounds = new MapBounds( ne, sw);
					List<SignMarker> signs = mongo.getSignDB().getSignMarkers(bounds);
					json = mapper.writeValueAsString(signs);
				}
			}
			catch (Exception ex) {
				System.out.println("Caught exception "+ex);
				logger.log("Caught exception "+ex);
				ex.printStackTrace();
			}
//			System.out.println("send "+json+" to client");
			out.println(json);

		}		
		out.close();
	}
	
}
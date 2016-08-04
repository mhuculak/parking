package parking.admin;

import parking.database.MongoInterface;
import parking.map.SignMarker;
import parking.map.Sign;
import parking.map.Position;
import parking.security.WebLogin;

import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class SignServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String method = request.getMethod();
		Map<String,String> postData = null;
		if (method.equals("POST")) {
			postData = WebLogin.getBody(request);

		}
		String db = request.getParameter("db");
		String id = request.getParameter("id");		

		PrintWriter out = response.getWriter();
		response.setContentType("text/plain");
		
		MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort());
		if (method.equals("POST")) {
			Position p = new Position (postData.get("lat"), postData.get("lng"));
			System.out.println("move sign id " + postData.get("id") + " to " + p.toString());			
			boolean ok = mongo.getSignDB().moveSign(postData.get("id"), p);
			if (ok) {
				out.println("success");
			}
			else {
				out.println("failed");
			}
		}
		else { // GET
			ObjectMapper mapper = new ObjectMapper();
			String json = null;
			if ( id != null) {
				try {
					Sign sign = mongo.getSignDB().getSign(id);
					String signDetails = sign.getParkingSchedule().displayText();
					json = mapper.writeValueAsString(signDetails);
				}
				catch (Exception ex) {
					System.out.println("Caught exception "+ex);
					ex.printStackTrace();
				}
			}			
			else {
				int max = 5;
				try {
					List<SignMarker> signs = mongo.getSignDB().getSignMarkers(max);
					json = mapper.writeValueAsString(signs);
				}
				catch (Exception ex) {
					System.out.println("Caught exception "+ex);
					ex.printStackTrace();
				}
			}

			out.println(json);

		}		
		out.close();
	}
	
}
package parking.admin;

import parking.database.MongoInterface;
import parking.map.SignMarker;
import parking.map.Sign;
import parking.map.Position;
import parking.map.MapBounds;
import parking.map.MapInit;
import parking.security.WebLogin;
import parking.display.ThumbNail;
import parking.util.Utils;
import parking.util.Logger;
import parking.util.LoggingTag;

import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.mongodb.gridfs.GridFSDBFile;

public class SignServlet extends HttpServlet {

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
		String id = request.getParameter("id");
		
		String nela = request.getParameter("nela");
		String nelg = request.getParameter("nelg");
		String swla = request.getParameter("swla");
		String swlg = request.getParameter("swlg");

		String signNum = request.getParameter("signNum");	

		
		response.setContentType("text/plain");
		
		MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
		if (method.equals("POST")) {
			PrintWriter out = response.getWriter();			
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
			out.close();
		}
		else { // GET
			String json = null;
			try {
				ObjectMapper mapper = new ObjectMapper();				
				if ( id != null) {
					Sign sign = mongo.getSignDB().getSign(id);
					if (sign != null) {
						String action = request.getParameter("action");
						if (action != null) {
							if (action.equals("thumbnail")) {
								String size = request.getParameter("size");
								logger.log("get image "+sign.getImageName());
								GridFSDBFile image = mongo.getPictureDB().getPicture(sign.getImageName());
								if (image != null) {
									BufferedImage signImage = ImageIO.read(image.getInputStream());
									ThumbNail thumb = null;
									logger.log("create thumb ");
									if (size == null) {
									 	thumb = new ThumbNail(signImage, null);
									}
									else {
										thumb = new ThumbNail(signImage, null, Utils.parseInt(size));
									}
									ByteArrayOutputStream os = new ByteArrayOutputStream();
									ImageIO.write(thumb.getImage(),"jpg", os); 
									InputStream inStream = new ByteArrayInputStream(os.toByteArray());
									OutputStream outStream = response.getOutputStream();
									response.setContentType("image/jpeg");
        							response.setContentLength(os.toByteArray().length);
        							logger.log("send data... ");
									sendImage(inStream, outStream, logger);
									return;																
								}
							}
						}
						else {						
							String signDetails = sign.displayText();
							json = mapper.writeValueAsString(signDetails);
						}
					}
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
			PrintWriter out = response.getWriter();
			out.println(json);
			out.close();
		}		
		
	}
	private void sendImage( InputStream is, OutputStream outStream, Logger logger) throws IOException {
		byte[] buffer = new byte[4096];
        int bytesRead = -1;
         
        while ((bytesRead = is.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
            logger.log("send " + bytesRead + " bytes from input stream to output stream");
        }
         
        is.close();
        outStream.close();  

	}
}
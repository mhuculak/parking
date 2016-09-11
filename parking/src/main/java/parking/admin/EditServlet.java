package parking.admin;

import parking.security.WebLogin;
import parking.security.User.Permission;
import parking.database.MongoInterface;
import parking.map.Sign;
import parking.map.MapInit;

import parking.util.Logger;
import parking.util.LoggingTag;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;

public class EditServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Logger logger = new Logger(request.getSession(), LoggingTag.Servlet, this, "service");
      MapInit mapInit = new MapInit();
      Map<String, String> postData = WebLogin.getBody(request);
		String method = request.getMethod();
		int port = request.getServerPort();
      logger.log("Method = "+method);
      WebLogin login = WebLogin.getLogin(request);
      if (login == null || login.isLoggedIn() == false) {
         if (method.equals("POST")) {
            logger.log("Authenticating...");
            login =  WebLogin.Authenticate(request, response, Permission.user);
         }
         else if (login == null) {
            logger.log("create dummy login...");
            login = new WebLogin(request);
         }
      }	
      else {
         logger.log("logged in as"+login.getUserName());
      }

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      String db = request.getParameter("db");
      MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
      
      if (postData != null) {
         logger.log("processing post data...");
         String action = postData.get("action");
         String id = postData.get("id");
         String returnUrl = postData.get("returnUrl");
         logger.log("Got id = "+id+" action = "+action+" returnUrl = "+returnUrl);
         for ( String key : postData.keySet()) {
            logger.log("key = "+key+" value = "+postData.get(key));
         }
         Sign sign = mongo.getSignDB().getSign(id);
         if (sign != null) {
            if (action.equals("edit")) {           
               out.println(AdminHtml.verificationFormAddress(request.getRequestURI()+"/verify", returnUrl, sign, false, logger));
            }
            else if (action.equals("delete")) {
               boolean deleted = mongo.getSignDB().removeSign(sign);
               if (deleted) {
                  logger.log("Sign "+id+" deleted");
               }
               else {
                  logger.error("Failed to delete sign "+id);
               }
               response.sendRedirect(response.encodeRedirectURL(returnUrl));
            }
            else {
               logger.error("unknown action "+action);
            }
         }
         else {
            logger.error("sign with id"+id+" does not exist");
         }
      }
   }
}

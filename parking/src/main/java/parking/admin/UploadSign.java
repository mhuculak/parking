package parking.admin;

import parking.map.Position;
import parking.map.Direction;
import parking.map.Address;
import parking.map.Sign;
import parking.map.SignMarker;
import parking.database.MongoInterface;
import parking.display.SignBuilder;
import parking.schedule.ParkingSchedule;
import parking.schedule.TimeRange;
import parking.schedule.WeekDaySet;
import parking.schedule.DateRange;
import parking.security.WebLogin;
import parking.security.User.Permission;

import parking.util.Logger;
import parking.util.LoggingTag;

import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
 
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.output.*;

import org.codehaus.jackson.map.ObjectMapper;

public class UploadSign extends HttpServlet {
   
   private boolean isMultipart;
   private DiskFileItemFactory factory;
   private ServletFileUpload upload;
   private String filePath;
   private String fileName;
   private int maxFileSize = 50 * 1024 * 1024;
   private int maxMemSize = 4 * 1024 * 1024;
   private File file ;
   private Map<String, String> postData;
   private Logger logger;

   public void init() {
      // Get the file location where it would be stored.
      filePath = 
             getServletContext().getInitParameter("file-upload"); 
   }

   public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
      logger = new Logger(request.getSession(), LoggingTag.Servlet, this, "service");
      logger.log("Enter "+request.getRequestURI());
      String method = request.getMethod();
      WebLogin login = WebLogin.getLogin(request);
      if (login == null || login.isLoggedIn() == false) {
         if (method.equals("POST")) {
            logger.log("Authenticating...");
            login =  WebLogin.Authenticate(request, response, Permission.user);
         }
         else if (login == null) {
            logger.log("create dummy login...");
            login = new WebLogin(request.getRequestURI());
         }
      }
      else {
         logger.log("logged in as"+login.getUserName());
      }

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      if (login.isLoggedIn() == false) {
         logger.log("display login form...");
         out.println(AdminHtml.simplePage(login.getLoginForm()));
         return;
      }
            
      logger.log("Got login session "+login.toString());      
      String user = WebLogin.getLoginUser(request);


      if (method.equals("GET")) {
         logger.log("GET: display new form...");
         out.println(AdminHtml.simplePage(AdminHtml.getUploadForm()));
         return;
      }
      else {
         isMultipart = ServletFileUpload.isMultipartContent(request);
         if (!isMultipart) {
            logger.log("POST: No file data to process");
            out.println(AdminHtml.simplePage(AdminHtml.getUploadForm()));
            return;
         }
         else {
            logger.log("Got multipart POST...");
         }
      }

      // Check that we have a file upload request
      
      postData = new HashMap<String, String>();
      try {           
         if( isMultipart ) {
            logger.log("Process file upload");
            try {
               processFileUpload(request);
            }
            catch (FileNotFoundException ex) {
               logger.error("got exception "+ex);
               out.println("File not found. Please retry...");
               out.println(AdminHtml.simplePage(AdminHtml.getUploadForm()));
               return;
            }
            String ignoreFile = postData.get("ignoreFile");
            if (ignoreFile != null) {
               logger.log("Ignoring posted data...redirecting");
               response.sendRedirect(request.getRequestURI());
               return;
            }
         }
      
         String db = request.getParameter("db");
         MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort(), logger);
         Sign sign = null;
         if (file != null) {
            if (postData.get("lat") != null && postData.get("lng") != null) {
               sign = new Sign(file, new Position(postData.get("lat"), postData.get("lng")), logger);
            }     
            else {
               sign = new Sign(file, logger); // use position from Exif header if available
               if (sign.getPosition() == null) {
                  logger.log("Failed to get position from uploaded image");
               }
            }
            SignBuilder builder = null;
            ParkingSchedule schedule = null;
            try {      
               builder = new SignBuilder(file, logger);
               schedule = builder.readSign(0);
            }
            catch (OutOfMemoryError err) {
               logger.error("Got "+err+" total memory is "+ Runtime.getRuntime().totalMemory() / (1024 * 1024));
               out.println("Got "+err+" Please retry...");
               out.println(AdminHtml.simplePage(AdminHtml.getUploadForm()));
            }
            if (schedule != null) {
               sign.setAutoSchedule(schedule);
               sign = mongo.getSignDB().addSign(sign, file, user);
               logger.log("Auto sched: "+schedule);
            }
            else {
               String msg = "No sign info found";
               logger.log(msg);
               out.println(AdminHtml.simplePage(msg));
               return;
            }
         
            if (postData.get("verify") == null) {
               out.println(AdminHtml.verificationFormAddress(request.getRequestURI()+"/verify", request.getRequestURI(), sign, true, logger));
            }
            else {
                out.println(AdminHtml.verificationForm1(postData.get("verify"), schedule, sign.getID()));
            }            
         }
     
      } catch(Exception ex) {
         System.out.println(ex);
         ex.printStackTrace();
         out.println(ex);
      }

//      out.println("</body>");
//      out.println("</html>");
   }
   
   private void processFileUpload(HttpServletRequest request) throws Exception {
      factory = new DiskFileItemFactory();
      factory.setSizeThreshold(maxMemSize);
      factory.setRepository(new File("c:\\temp"));
      upload = new ServletFileUpload(factory);
      upload.setSizeMax( maxFileSize );
      List fileItems = upload.parseRequest(request);
      Iterator i = fileItems.iterator();         
      while ( i.hasNext () ) {
         FileItem fi = (FileItem)i.next();
         if ( fi.isFormField () )   {
            postData.put(fi.getFieldName(), fi.getString());              
         }
         else {
            if (filePath == null) {
               filePath = "";
            }
               // Get the uploaded file parameters
            String fieldName = fi.getFieldName();
            fileName = fi.getName();
            logger.log("uploading file named "+ fileName + " file path = "+filePath);
            String contentType = fi.getContentType();
            boolean isInMemory = fi.isInMemory();
            long sizeInBytes = fi.getSize();
            // Write the file
            if( fileName.lastIndexOf("\\") >= 0 ){
               file = new File( filePath + 
               fileName.substring( fileName.lastIndexOf("\\"))) ;
            } else{
               file = new File( filePath + 
               fileName.substring(fileName.lastIndexOf("\\")+1)) ;
            }
            fi.write( file ) ;
//            logger.log(LoggingTag.Servlet, "UploadSign", "service","Uploaded Filename: " + fileName);              
         }
      }    
   }

   private void processPost(HttpServletRequest request) {
      postData = WebLogin.getBody(request);
   }

   

}


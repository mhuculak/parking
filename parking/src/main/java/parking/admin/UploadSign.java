package parking.admin;

import parking.map.Position;
import parking.map.Direction;
import parking.map.Address;
import parking.map.Sign;
import parking.map.Place;
import parking.map.SignMarker;
import parking.map.MapInit;
import parking.database.MongoInterface;
import parking.opencv.SignRecognizer;
import parking.schedule.ParkingSchedule;
import parking.schedule.SignSchedule;
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
      String method = request.getMethod();
      String userAgent = request.getHeader("User-Agent");
      logger.log("Enter "+request.getRequestURI()+" method = "+method+" user agent = "+userAgent);
      MapInit mapInit = new MapInit();      
      ClientType client = ClientType.getClientType(userAgent);
      logger.log("request from "+userAgent+" client "+client);
      WebLogin.logHeaders(request);      
      WebLogin login = WebLogin.authenticate(request);  
      if (login.isLoggedIn() == false) {        
         login.setResponseNotAuthorized(response);         
         return;
      }
      response.setContentType(ClientType.getContentType(client));
      PrintWriter out = response.getWriter();            
      logger.log("Got login = "+login.toString());      
      String user = login.getUserName();
//      String user = "android";
      if (method.equals("GET")) {
         if (client == ClientType.BROWSER) {
            logger.log("GET: display new form...");
            out.println(AdminHtml.simplePage(AdminHtml.getUploadForm()));
         }
         else {
            logger.log("GET: from non-browser...nothing to do");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT );
         }
         return;
      }
      else {
         isMultipart = ServletFileUpload.isMultipartContent(request);
         if (!isMultipart) {
            logger.log("POST: No file data to process");
            if (client == ClientType.BROWSER) {
               logger.log("display upload form for browser");
               out.println(AdminHtml.simplePage(AdminHtml.getUploadForm()));
            }
            else {
               logger.log("nothing to display for app...");
            }
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
            sign = mongo.getSignDB().addSign(sign, file, user);
            SignRecognizer recognizer = null;
            ParkingSchedule schedule = null;
            try {      
               logger.log("recognizing...");
               recognizer = new SignRecognizer(file, logger);
               schedule = recognizer.readSign(0);
            }
            catch (OutOfMemoryError err) {
               logger.error("Got "+err+" total memory is "+ Runtime.getRuntime().totalMemory() / (1024 * 1024)+" max is "+Runtime.getRuntime().maxMemory() / (1024 * 1024));
               out.println("Got "+err+" Ask Mike to increase the maximum memory size on the server. Currently is "+Runtime.getRuntime().maxMemory() / (1024 * 1024));
               out.println(AdminHtml.simplePage(AdminHtml.getUploadForm()));
            }
            catch (Exception ex) {
               
               if (client == ClientType.BROWSER) { 
                  out.println("Got "+ex+" while recognizing sign");              
                  out.println(AdminHtml.simplePage(AdminHtml.getUploadForm()));
               }
               else {
                  logger.error("Caught exception "+ex+" while attempting to recognize sign");
                  ObjectMapper mapper = new ObjectMapper(); 
//                  Place signPlace = new Place(sign.getAddress(), sign.getPosition());
                  SignSchedule ret = new SignSchedule(null, null, sign.getID() );               
                  String json = mapper.writeValueAsString(ret);
                  logger.log("got json result "+json);
                  out.println(json);
               }
               ex.printStackTrace();
            }
            if (schedule != null) {
               sign.setAutoSchedule(schedule);
               mongo.getSignDB().updateSign(sign, user);
               logger.log("Auto sched: "+schedule);
            }
            else {
               String msg = "No sign info found";
               logger.log(msg);
               if (client == ClientType.BROWSER) {
                  out.println(AdminHtml.simplePage(msg));
               }
               return;
            }
            
            if (client == ClientType.BROWSER) {
               if (postData.get("verify") == null) {
                  out.println(AdminHtml.verificationFormAddress(request.getRequestURI()+"/verify", request.getRequestURI(), sign, true, logger));
               }
               else {
                  out.println(AdminHtml.verificationForm1(postData.get("verify"), schedule, sign.getID()));
               }
            }
            else {
               ObjectMapper mapper = new ObjectMapper();                             
//               ParkingSchedule test = new ParkingSchedule("P;L=100;T=9:30,10:30;W=M:F;D=M 18,N 18");
//               logger.log("convert "+test.toString()+" to json");
//               Place signPlace = new Place(sign.getAddress(), sign.getPosition());
               SignSchedule signSchedule = new SignSchedule(schedule, null, sign.getID() );               
               String json = mapper.writeValueAsString(signSchedule);
               logger.log("got json result "+json);
               out.println(json);
                  
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


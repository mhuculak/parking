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

import java.io.*;
import java.util.*;
 
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
   private String filePath;
   private String fileName;
   private int maxFileSize = 50 * 1024 * 1024;
   private int maxMemSize = 4 * 1024 * 1024;
   private File file ;

   public void init() {
      // Get the file location where it would be stored.
      filePath = 
             getServletContext().getInitParameter("file-upload"); 
   }

   public void doPost(HttpServletRequest request, 
               HttpServletResponse response)
              throws ServletException, java.io.IOException {
      // Check that we have a file upload request
      isMultipart = ServletFileUpload.isMultipartContent(request);
      response.setContentType("text/html");
//      response.setContentType("text/plain");
      java.io.PrintWriter out = response.getWriter( );
      if( !isMultipart ){
         out.println("<html>");
         out.println("<head>");
         out.println("<title>Servlet upload</title>");  
         out.println("</head>");
         out.println("<body>");
         out.println("<p>No file uploaded</p>"); 
         out.println("</body>");
         out.println("</html>");
         return;
      }
      DiskFileItemFactory factory = new DiskFileItemFactory();
      // maximum size that will be stored in memory
      factory.setSizeThreshold(maxMemSize);
      // Location to save data that is larger than maxMemSize.
      factory.setRepository(new File("c:\\temp"));

      // Create a new file upload handler
      ServletFileUpload upload = new ServletFileUpload(factory);
      // maximum file size to be uploaded.
      upload.setSizeMax( maxFileSize );

      Position p0 = new Position();   // position of the camera
      Position p1 = new Position();   // map position on opposite side of street
      Position p2 = new Position();   // map position on sign side of street

      String verifyUrl = null;
/*
      out.println("<html>");
      out.println("<head>");
      out.println("<title>Servlet upload</title>");  
      out.println("</head>");
      out.println("<body>");
*/
      try { 
         // Parse the request to get file items.
         List fileItems = upload.parseRequest(request);
	
         // Process the uploaded file items
         Iterator i = fileItems.iterator();

         
         while ( i.hasNext () ) {
            FileItem fi = (FileItem)i.next();
            if ( fi.isFormField () )	{
               System.out.println("Found form field " +  fi.getFieldName() + " value " + fi.getString());
               String fieldName = fi.getFieldName();
               String fieldValue = fi.getString();
               if (fieldName.equals("lat1")) {
                  p1.setLatitude(fieldValue);
               }
               else if (fieldName.equals("lat2")) {
                  p2.setLatitude(fieldValue);
               }
               else if (fieldName.equals("lng1")) {
                  p1.setLongitude(fieldValue);
               }
               else if (fieldName.equals("lng2")) {
                  p2.setLongitude(fieldValue);
               }
               else if (fieldName.equals("lat")) {
                  p0.setLatitude(fieldValue);
               }
               else if (fieldName.equals("lng")) {
                  p0.setLongitude(fieldValue);
               }
               else if (fieldName.equals("verify")) {
                  verifyUrl = fieldValue;
               }
            }
            else {
               // Get the uploaded file parameters
               String fieldName = fi.getFieldName();
               fileName = fi.getName();
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
               System.out.println("Uploaded Filename: " + fileName);
               
            }
         }      
         

         String db = request.getParameter("db");
         MongoInterface mongo = MongoInterface.getInstance(db, request.getServerPort());
         Sign sign = null;
         if (p0.isDefined()) {
            System.out.println("create new Sign with file and position " + p0.toString());
            sign = new Sign(file, p0);            
         }
/*         
         else if (p1.isDefined()) {
            Direction dir = new Direction(p1,p2);
            sign = new Sign(file, dir);
         }
*/         
         else {
            System.out.println("ERROR: no position info uploaded with picture");
         }
         
/*         
         ObjectMapper mapper = new ObjectMapper();
         String json = mapper.writeValueAsString(signMarker);
         out.println(json);
*/        
         SignBuilder builder = new SignBuilder(file, 0.25);
         ParkingSchedule schedule = builder.readSign(0);
         if (schedule != null) {
            sign.setAutoSchedule(schedule);
            SignMarker signMarker = mongo.getSignDB().addSign(sign, file);
            System.out.println(schedule);
         }
         else {
            System.out.println("No sign info found");
         }
         
         try {
            out.println(getForm(verifyUrl, schedule, sign.getPictureTag()));
         }
         catch (Exception ex) {
            ex.printStackTrace();
            out.println("Exception "+ex+"occured while processing picture data");
         }


      } catch(Exception ex) {
         System.out.println(ex);
         ex.printStackTrace();
         out.println(ex);
      }

//      out.println("</body>");
//      out.println("</html>");
   }
   public void doGet(HttpServletRequest request, 
                       HttpServletResponse response)
        throws ServletException, java.io.IOException {
        
        throw new ServletException("GET method used with " +
                getClass( ).getName( )+": POST method required.");
   } 

      private String getForm(String uri, ParkingSchedule schedule, String tag) {
         StringBuilder sb = new StringBuilder(10);
         sb.append("<h2>Step 3 - make any necessary corrections and submit</h2>");
         sb.append("<form action=\"" + uri + "\" method=\"POST\">");
         sb.append("<input type=\"hidden\" name=\"tag\" value=\""+tag+"\">");
         if (schedule.isRestricted()) {
            sb.append("<input type=\"radio\" name=\"restricted\" value=\"true\" checked> No Parking<br>");
            sb.append("<input type=\"radio\" name=\"restricted\" value=\"false\"> Parking<br>");
         }
         else {
            sb.append("<input type=\"radio\" name=\"restricted\" value=\"true\"> No Parking<br>");
            sb.append("<input type=\"radio\" name=\"restricted\" value=\"false\" checked> Parking<br>");
            if (schedule.getTimeLimitMinutes() > 0) {
               sb.append("Time Period (in minutes) Read From Sign:<br>");
               sb.append("<input type=\"text\" style=\"width:50px;\" name=\"timePeriod\" value=\""+schedule.getTimeLimitMinutes()+"\"><br>");
            }
            else {
               sb.append("No Time Period Found  (Please add time period in minutes if required) :<br>");
               sb.append("<input type=\"text\" style=\"width:50px;\" name=\"timePeriod\" value=\"\"><br>");
            }
         }


            TimeRange range = schedule.getTimeRange();
            if (range != null) {
               sb.append("Time Range Read From Sign:<br>");
               sb.append("<input type=\"text\" style=\"width:20px;\" name=\"startHour\" value=\""+range.getStart().getHourString()+"\">");
               sb.append(":<input type=\"text\" style=\"width:20px;\" name=\"startMinute\" value=\""+range.getStart().getMinuteString()+"\">");
               sb.append(" to <input type=\"text\" style=\"width:20px;\" name=\"endHour\" value=\""+range.getEnd().getHourString()+"\">");
               sb.append(":<input type=\"text\" style=\"width:20px;\" name=\"endMinute\" value=\""+range.getEnd().getMinuteString()+"\"><br>");
            }
            else {
               sb.append("No Time Range Found (Please add if required):<br>");
               sb.append("<input type=\"text\" style=\"width:20px;\" name=\"startHour\" value=\"\">");
               sb.append(":<input type=\"text\" style=\"width:20px;\" name=\"startMinute\" value=\"\">");
               sb.append(" to <input type=\"text\" style=\"width:20px;\" name=\"endHour\" value=\"\">");
               sb.append(":<input type=\"text\" style=\"width:20px;\" name=\"endMinute\" value=\"\"><br>");
            }
            WeekDaySet weekDays = schedule.getWeekDays();
            if (weekDays != null) {
               Map<String, Boolean> weekDayMap = weekDays.getMap();
               sb.append("Week Day Range Read From Sign:<br>");
               int i=0;
               for ( String day : WeekDaySet.allDays) {
                  Boolean gotIt = weekDayMap.get(day);
                  String name = "day"+i;
                  if (gotIt) {
                     sb.append("<input type=\"checkbox\" name=\""+name+"\" value=\""+day+"\" checked>"+day+"<br>");
                  }
                  else {
                     sb.append("<input type=\"checkbox\" name=\""+name+"\" value=\"\">"+day+"<br>");
                  }
                  i++;
               }
/*               
               if (weekDays.isContiguous() && weekDays.size() > 1) {
                  sb.append("Week Day Range Read From Sign:<br>");
                  sb.append("<input type=\"text\" style=\"width:50px;\" name=\"startDay\" value=\""+weekDays.getStart()+"\">");
                  sb.append(" to <input type=\"text\" style=\"width:50px;\" name=\"endDay\" value=\""+weekDays.getEnd()+"\"><br>");
               }
               else {
                  sb.append("Week Days Read From Sign:<br>");
                  for ( int i=0 ; i<weekDays.size() ; i++) {
                     String name = "day"+i;
                     sb.append("<input type=\"text\" style=\"width:50px;\" name=\""+name+"\" value=\""+weekDays.get(i).toString()+"\"><br>");
                  }
               }
*/               
            }
            else {
               sb.append("No Week Days Found (Please add if required):<br>");
               for ( String day : WeekDaySet.allDays) {
                  sb.append("<input type=\"checkbox\" name=\""+day+"\" value=\""+day+"\">"+day+"<br>");
               }
            }
            DateRange dateRange = schedule.getDateRange();
            if (dateRange != null) {
               sb.append("Date Range Read From Sign:<br>");
               sb.append("<input type=\"text\" style=\"width:20px;\" name=\"startMonthDay\" value=\""+dateRange.getStart().getDay()+"\">");
               sb.append(" <input type=\"text\" style=\"width:60px;\" name=\"startMonth\" value=\""+dateRange.getStart().getMonth()+"\">");
               sb.append(" to <input type=\"text\" style=\"width:20px;\" name=\"endMonthDay\" value=\""+dateRange.getEnd().getDay()+"\">");
               sb.append(" <input type=\"text\" style=\"width:60px;\" name=\"endMonth\" value=\""+dateRange.getEnd().getMonth()+"\"><br>");
            }
            else {
               sb.append("No Date Range Found (Please add if required):<br>");
               sb.append("<input type=\"text\" style=\"width:20px;\" name=\"startMonthDay\" value=\"\">");
               sb.append(" <input type=\"text\" style=\"width:60px;\" name=\"startMonth\" value=\"\">");
               sb.append(" to <input type=\"text\" style=\"width:20px;\" name=\"endMonthDay\" value=\"\">");
               sb.append(" <input type=\"text\" style=\"width:60px;\" name=\"endMonth\" value=\"\"><br>");
            }         
         sb.append("<input type=\"submit\" value=\"Submit\"></form>");
          
      return sb.toString();
   }

}
package parking.admin;

import parking.map.Sign;
import parking.map.Address;
import parking.schedule.ParkingSchedule;
import parking.schedule.TimeRange;
import parking.schedule.WeekDaySet;
import parking.schedule.DateRange;

import parking.util.Logger;
import parking.util.LoggingTag;

import java.util.List;
import java.util.Map;

public class AdminHtml {
	public static String verificationFormAddress(String processUrl, String returnUrl, Sign sign, boolean auto, Logger logger) {
      StringBuilder sb = new StringBuilder(10);
      sb.append("<h2>Step 3 - make any necessary corrections and submit.</h2>");
      sb.append("<p>Important: be sure to use a street number from the <b>same side</b> of the street as the sign.</p>");
      sb.append("<form action=\"" + processUrl +"\" method=\"POST\"  enctype=\"application/x-www-form-urlencoded\">");
      sb.append("<input type=\"hidden\" name=\"signID\" value=\""+sign.getID()+"\">");
      sb.append("<input type=\"hidden\" name=\"returnUrl\" value=\""+returnUrl+"\">");
      if (auto || sign.getParkingSchedule() == null) {
         if (sign.getAutoSchedule() != null) {
            addSchedule(sb, sign.getAutoSchedule());
            sb.append("<input type=\"hidden\" name=\"auto\" value=\"true\">");
         }
      }
      else {
         addSchedule(sb, sign.getParkingSchedule());
         sb.append("<input type=\"hidden\" name=\"auto\" value=\"false\">");
      }
      addAddress(sb, sign, logger);
      sb.append("<input type=\"submit\" value=\"Submit\"></form>");          
      return sb.toString();
   }

   public static String verificationForm1(String uri, ParkingSchedule schedule, String signID) {
      StringBuilder sb = new StringBuilder(10);
      sb.append("<h2>Step 3 - make any necessary corrections and submit</h2>");
      sb.append("<form action=\"" + uri + "\" method=\"POST\"  enctype=\"application/x-www-form-urlencoded\">");
      sb.append("<input type=\"hidden\" name=\"signID\" value=\""+signID+"\">");
      addSchedule(sb, schedule);
      sb.append("<input type=\"submit\" value=\"Submit\"></form>");          
      return sb.toString();
   }

   public static void addAddress(StringBuilder sb, Sign sign, Logger logger) {
      double maxStreetSearchAngle = 0.0005;
      if (sign.getPosition() == null) {
         sb.append("Street Number: <input type=\"text\" style=\"width:50px;\" name=\"streetNumber\" value=\"\"><br>");
         sb.append("Street Name:<input type=\"text\" style=\"width:100px;\" name=\"street\" value=\"\"><br>");
         sb.append("City:<input type=\"text\" style=\"width:100px;\" name=\"city\" value=\"\"><br>");
      }
      else {
         Address address = sign.getAddress();
         List<String> nearby = sign.getPosition().findNearbyStreets(maxStreetSearchAngle);
         String signStreet = address.getStreetName();
         sb.append("<input type=\"hidden\" name=\"city\" value=\""+address.getCity()+"\">");
         sb.append("<input type=\"hidden\" name=\"zip\" value=\""+address.getPostalCodeZIP()+"\">");
         sb.append("Street Number: <input type=\"text\" style=\"width:50px;\" name=\"streetNumber\" value=\""+address.getStreetNumber()+"\"><br>");
         sb.append("Street Name:<input type=\"checkbox\" name=\"street\" value=\""+signStreet+"\" checked>"+signStreet+"<br>");
         if (nearby != null) {
            sb.append("Nearby streets:<br>");
            for ( String street : nearby) {
               sb.append("<input type=\"checkbox\" name=\"street\" value=\""+street+"\">"+street+"<br>");
            }
         }
      }  
   }

   public static void addSchedule(StringBuilder sb, ParkingSchedule schedule) {
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
         Map<String, Boolean> weekDayMap = weekDays.getWeekDayMap();
         sb.append("Week Day Range Read From Sign:<br>");
         int i=0;
         for ( String day : WeekDaySet.allDays) {
            Boolean gotIt = weekDayMap.get(day);
            String name = "day"+i;
            if (gotIt) {
               sb.append("<input type=\"checkbox\" name=\""+name+"\" value=\""+day+"\" checked>"+day+"<br>");
            }
            else {
               sb.append("<input type=\"checkbox\" name=\""+name+"\" value=\""+day+"\">"+day+"<br>");
            }
            i++;
         }             
      }
      else {
         sb.append("No Week Days Found (Please add if required):<br>");
         int i=0;
         for ( String day : WeekDaySet.allDays) {
            String name = "day"+i;
            sb.append("<input type=\"checkbox\" name=\""+name+"\" value=\""+day+"\">"+day+"<br>");
            i++;
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
   }

   public static String getGoogleMap() {
      StringBuilder sb = new StringBuilder(100);
      sb.append("<!DOCTYPE html>");
      sb.append("<html>");
      sb.append("<head>");
      sb.append("<style type=\"text/css\"> #map{ width:700px; height: 500px; }</style>");
      sb.append("<script type=\"text/javascript\" src=\"https://maps.googleapis.com/maps/api/js?key=AIzaSyDni-ZQemF7eA1P-A76acHMF2tREyFM3HI\"></script>");      
      sb.append("<body>");
      sb.append("<script type=\"text/javascript\" src=\"js/map.js\"></script>");
      sb.append("</body>");
      sb.append("</html>");
      return sb.toString();
   }

   public static String getUploadForm() {
      StringBuilder sb = new StringBuilder(100);      
//      sb.append("<form action=\"" + processUri + "\" method=\"POST\" enctype=\"multipart/form-data\">");
      sb.append("<form method=\"POST\" enctype=\"multipart/form-data\">");
      sb.append("Sign Picture: <input type=\"file\" id=\"pic\" name=\"pic\" accept=\"image/*\" /><br>");
      sb.append("<input type=\"submit\" name=\"submit\" value=\"Upload\" />");
      return sb.toString(); 
   }

   public static String simplePage(String content) {
      StringBuilder sb = new StringBuilder(100);
      sb.append("<html>");
      sb.append("<head>");
      sb.append("<title>Servlet upload</title>");  
      sb.append("</head>");
      sb.append("<body>");
      sb.append("<p>"+content+"</p>"); 
      sb.append("</body>");
      sb.append("</html>");
      return sb.toString();
   }

   public static String simplePage(String content, String userAgent) {
      StringBuilder sb = new StringBuilder(100);
      sb.append("<html>");
      sb.append("<head>");
      sb.append(getStyle(userAgent));
      sb.append("<title>Servlet upload</title>");  
      sb.append("</head>");
      sb.append("<body>");
      sb.append("<p>"+content+"</p>"); 
      sb.append("</body>");
      sb.append("</html>");
      return sb.toString();
   }


   //
   // FIXME: when I try this with my Android phone, the web pages still use a tiny font size that needs to be zoomed to read
   //
   private static String getStyle(String userAgent) {
      if (userAgent.contains("Android")) {
         return "<style> body {font size: 5 em;} p {font size: 5 em;}h1 {font size: 7 em;} h2 {font size: 6 em;} </style>";
      }
      else {
         return "<style> body {font size: 2 em;} p {font size: 2 em;}h1 {font size: 4 em;} h2 {font size: 3 em;} </style>";
      }
   }
}
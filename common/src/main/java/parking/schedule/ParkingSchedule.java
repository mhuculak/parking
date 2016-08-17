package parking.schedule;

import parking.util.Logger;
import parking.util.LoggingTag;

import java.util.Map;
import java.util.List;

public class ParkingSchedule {

	private boolean isRestriction;
	private boolean permitHolderExcepted;
	private boolean allHours;
	private boolean allWeekDays;
	private boolean allDaysOfYear;

	private TimeRange timeRange;
	private WeekDaySet weekDays;
	private DateRange dateRange;

	private double hourlyRateDollars;
	private int timeLimitMinutes;
	private String permitNumber;
	private Logger m_logger;

	public ParkingSchedule(ParkingSignType signType, Logger logger) {
		m_logger = new Logger(logger, this, LoggingTag.Schedule);
		switch (signType) {
			case PARKINGMETER:
			case PARKING:
				isRestriction = false;
				break;							
			case NOPARKING:
			case NOSTOPPING:
				isRestriction = true;
				break;
			default:
				m_logger.error("unknown sign type "+signType);
		}
	}

	public ParkingSchedule(String sched, Logger logger) {
		m_logger = new Logger(logger, this, LoggingTag.Schedule);
		String[] data = sched.split(";");
		if (data[0].equals("NP")) {
			isRestriction = true;
		}
		else {
			isRestriction = false;
		}
		for ( int i=1 ; i<data.length ; i++) {
			String[] kv = data[i].split("=");
			String key = kv[0];
			String value = kv.length == 2 ? kv[1] : null;
			if (key.equals("T")) {
				if (value == null || value.equals("any")) {
					allHours = true;
				}
				else {
					allHours = false;
					timeRange = new TimeRange(value);
				}
			}
			else if (key.equals("W")) {
				if (value == null || value.equals("any")) {
					allWeekDays = true;
				}
				else {
					allWeekDays = false;
					weekDays = new WeekDaySet(value);
				}

			}
			else if (key.equals("D")) {
				if (value == null || value.equals("any")) {
					allDaysOfYear = true;
				}
				else {
					allDaysOfYear = false;
					dateRange = new DateRange(value);
				}
			}
			else if(key.equals("L")) {
				if (value != null) {
					timeLimitMinutes = Integer.parseInt(value);
				}
			}

		}
		setFlags();
	}

	public ParkingSchedule(Map<String,String> postData, Logger logger) {
		m_logger = new Logger(logger, this, LoggingTag.Schedule);
		String restricted = postData.get("restricted");
		isRestriction = restricted.equals("true") ? true : false;
		if (isRestriction == false) {
			String timePeriod = postData.get("timePeriod");
			if (timePeriod != null) {
				timePeriod.replaceAll("[^0-9]","");
				timeLimitMinutes = timePeriod.length() > 0 ? Integer.parseInt(timePeriod) : 0;
				m_logger.log("Got timelimit "+timeLimitMinutes);
			}
		}
		String startHour = postData.get("startHour");
		String startMinute = postData.get("startMinute");
		String endHour = postData.get("endHour");
		String endMinute = postData.get("endMinute");
		if (startHour != null && endHour != null) {			
			m_logger.log("Got time range: "+startHour+" to "+endHour);
			if (startHour.length() > 0 && endHour.length() > 0) {
				timeRange = new TimeRange( new SimpleTime(startHour, startMinute), new SimpleTime(endHour, endMinute));
			}
		}
		String startWeekDay = postData.get("startDay");
		String endWeekDay = postData.get("endDay");
		if (startWeekDay != null && endWeekDay != null) {
			if (startWeekDay.length() > 0 || endWeekDay.length() > 0) {
				weekDays = new WeekDaySet( startWeekDay, endWeekDay);
				m_logger.log("Got week days "+weekDays.toString());
			}
		}
		else {
			List<String> days = WeekDaySet.allDays;
			weekDays = new WeekDaySet();
			for ( int i=0 ; i<days.size() ; i++) {
                String name = "day"+i;
                String day = postData.get(name);
                if (day != null) {
                	m_logger.log("Add "+name+" = "+day);
                	weekDays.add(day);
                }
            }
            if (weekDays.size() == 0) {
            	weekDays = null;
            }
		}
		
		String startMonthDay = postData.get("startMonthDay");
		String startMonth = postData.get("startMonth");
		String endMonthDay = postData.get("endMonthDay");
		String endMonth = postData.get("endMonth");
		if (startMonthDay != null && startMonth != null && endMonthDay != null && endMonth != null) {
			if (startMonthDay.length() > 0 && startMonth.length() > 0 && endMonthDay.length() > 0 && endMonth.length() > 0) {
				startMonthDay.replaceAll("[^0-9]","");
				endMonthDay.replaceAll("[^0-9]","");
				m_logger.log("create Date Range with "+startMonthDay+" "+startMonth+" "+endMonthDay+" "+endMonth);
				dateRange = new DateRange( new SimpleDate(startMonth, Integer.parseInt(startMonthDay)), new SimpleDate(endMonth, Integer.parseInt(endMonthDay)));
				m_logger.log("Got dateRange "+dateRange.toString());
			}
		}
		setFlags();
	}

	public void setTimeLimitMinutes(int timeLimitMinutes) {
		this.timeLimitMinutes = timeLimitMinutes;
		isRestriction = false;
	}

	public void setParkingRate(double rate) {
		hourlyRateDollars = rate;
		isRestriction = false;
	}

	public void setRestriction(TimeRange timeRange, WeekDaySet weekDaySet, DateRange dateRange) {
		isRestriction = true;
		this.timeRange = timeRange;
		this.weekDays = weekDaySet;
		this.dateRange = dateRange;		
		setFlags();
	}

	public void setTimeRange(TimeRange timeRange) {
		this.timeRange = timeRange;
	}

	public void setWeekDays(WeekDaySet weekDays) {
		this.weekDays = weekDays;
	}

	public void setDateRange(DateRange dateRange) {
		this.dateRange = dateRange;
	}

	public void setIsPermission(double rate, int timeLimit) {
		isRestriction = false;
		hourlyRateDollars = rate;
		timeLimitMinutes = timeLimit;
	}

	public void setPermitException(String permitNumber) {
		permitHolderExcepted = true;
		this.permitNumber = permitNumber;
	}

	public void setFlags() {
		if (timeRange == null) {
			allHours = true;
		}
		else {
			allHours = false;
		}
		if (weekDays == null) {
			allWeekDays = true;
		}
		else {
			allWeekDays = false;
		}
		if (dateRange == null) {
			allDaysOfYear = true;
		}
		else {
			allDaysOfYear = false;
		}
	}

	public boolean isRestricted() {
		return isRestriction;
	}

	public TimeRange getTimeRange() {
		return timeRange;
	}

	public WeekDaySet getWeekDays() {
		return weekDays;
	}

	public DateRange getDateRange() {
		return dateRange;
	}

	public int getTimeLimitMinutes() {
		return timeLimitMinutes;
	}

	public String displayText() {
		StringBuilder sb = new StringBuilder(10);
		if (isRestriction) {
			sb.append("No Parking<br>");
		}
		else {
			sb.append("Parking Permitted<br>");
			if (timeLimitMinutes > 0) {
				sb.append("Time Limit: "+timeLimitMinutes+" minutes<br>");
			}
		}
		if ( allHours) {
			sb.append("any time<br>");
		}
		else {
			sb.append(timeRange+"<br>");
		}
		if ( allWeekDays ) {
			sb.append("any day of the week<br>");
		}
		else {
			sb.append(weekDays.displayText()+"<br>");
		}
		if ( allDaysOfYear ) {
			sb.append("every day of the year<br>");
		}
		else {
			sb.append(dateRange.displayText()+"<br>");
		}
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(10);
		if (isRestriction) {
			sb.append("NP");
		}
		else {
			sb.append("P");
			if (timeLimitMinutes > 0) {
				sb.append(";L="+timeLimitMinutes);
			}
		}
		if ( allHours) {
			sb.append(";T=any");
		}
		else {
			sb.append(";T="+timeRange);
		}
		if ( allWeekDays ) {
			sb.append(";W=any");
		}
		else {
			sb.append(";W="+weekDays);
		}
		if ( allDaysOfYear ) {
			sb.append(";D=any");
		}
		else {
			sb.append(";D="+dateRange);
		}
		return sb.toString();
	}
}
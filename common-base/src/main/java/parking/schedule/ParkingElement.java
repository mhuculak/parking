package parking.schedule;

public enum ParkingElement {
	All,
	SignType,
	TimeLimit,
	TimeRange,
	WeekDays,
	DateRange,
	StartHour,
	StartMin,
	EndHour,
	EndMin,
	WeekDayStart,
	WeekDayEnd,
	WeekDay1,
	WeekDay2,
	WeekDay3,
	StartDay,
	StartMonth,
	EndDay,
	EndMonth;

	public static final ParkingElement[] weekday = { WeekDay1, WeekDay2, WeekDay3};
	public static final String longest;
   	static{
        String longestYet= "";
        for (ParkingElement value : ParkingElement.values()){
           if (longestYet.length()< value.name().length()) {
            longestYet= value.name();   
           }                
        }
        longest = longestYet;
    }  
}
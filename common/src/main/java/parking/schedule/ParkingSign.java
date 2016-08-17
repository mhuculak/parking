package parking.schedule;

import parking.opencv.TextGroup;
import parking.opencv.TextShape;
import parking.opencv.TextChoice;

import parking.util.Logger;
import parking.util.LoggingTag;

import java.util.Collections;
import java.util.Comparator;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

enum ScheduleComponent {
	TIMEPERIOD,
	TIMERANGE,
	WEEKDAYS,
	DATES,
	PERMIT,
	RATE,
	MISC
}

class ComponentChoiceComparator implements Comparator<ComponentChoice> {
	@Override
	public int compare(ComponentChoice c1, ComponentChoice c2) {
		
		if (getDiff(c1.getScore(), c2.getScore()) < 0.1) {
			if (getDiff(c1.getCoverage(), c2.getCoverage()) < 0.1) {

				if (c1.getMisses() < c2.getMisses()) { // chose fewer misses
					return -1;
				}
				else if(c1.getMisses() > c2.getMisses()) {
					return 1;
				}
				else {
					return 0;
				}
			}
			else { // choose the coverage closer to 1.0
				double d1 = Math.abs( 1.0 - c1.getCoverage());
				double d2 = Math.abs( 1.0 - c2.getCoverage());
				if ( d1 < d2) {
					return -1;
				}
				else if ( d1 > d2 ) {
					return 1;
				}
				else {
					return 0;
				}
			}
		
		}		
		else if (c1.getScore() > c2.getScore()) { // chose the higher score
			return -1;
		}
		else if (c1.getScore() < c2.getScore()) {
			return 1;
		}
		else {
			return 0;
		}
	}

	private double getDiff(double v1, double v2) {
		double diff = Math.abs(v1 - v2);
		double max = v1 > v2 ? v1 : v2;
		double norm = max > 0 ? diff/max : 0;
		return norm;
	}
}

//
//  FIXME: in order to compare score from different components such as time period vs weekdays,
//         we need to normalize the scores. To do this we can create a weighted sum ( word len * score)
//         of each element of the component so a component with fewer elements and longer words will
//         not be defeated by a component with more elements and shorter words. This will also prevent
//         components with poor coverage from being used
//
class ComponentChoice {
	private ScheduleComponent component;
	private double score;
	private double coverage;
	private int misses;
	private int numCharacters;
	private int lineLen;
	private List<Integer> ivalues;
	private List<Integer> ivalues2;
	private List<String> svalues;
	private List<WordChoice> words;
	private List<NumberChoice> numbers;

	public ComponentChoice( ScheduleComponent component, int lineLen, List<Integer> ivalues) {
		this.component = component;
		this.ivalues = ivalues;
		this.lineLen = lineLen;
	}

	public ComponentChoice( ScheduleComponent component, int lineLen) {
		this.component = component;
		this.lineLen = lineLen;
	}

	public ComponentChoice( ScheduleComponent component, int lineLen, List<Integer> ivalues, List<String> svalues) {
		this.component = component;
		this.ivalues = ivalues;
		this.svalues = svalues;
		this.lineLen = lineLen;
	}
/*

name clash: ComponentChoice(ScheduleComponent,int,List<Integer>,List<Integer>) and 
            ComponentChoice(ScheduleComponent,int,List<Integer>,List<String>) have the same erasure

	public ComponentChoice( ScheduleComponent component, int lineLen, List<Integer> ivalues, List<Integer> ivalues2) {
		this.component = component;
		this.ivalues = ivalues;
		this.ivalues2 = ivalues2;
		this.lineLen = lineLen;
	}
*/

	public void setIvalues2(List<Integer> ivalues2) {
		this.ivalues2 = ivalues2;
	}

	public void addTo(WordChoice wordChoice) {
		if (wordChoice != null) {
			score += wordChoice.getScore();
			misses += wordChoice.getMisses();
			numCharacters += wordChoice.word.length();
			coverage = numCharacters;
			coverage = coverage / lineLen;
			if (words == null) {
				words = new ArrayList<WordChoice>();
			}
			words.add(wordChoice);
		}
	}

	public void addTo(NumberChoice numChoice) {
		score += numChoice.getScore();
		numCharacters += numChoice.getNumDig();
		coverage = numCharacters;
		coverage = coverage / lineLen;
		if (numbers == null) {
			numbers = new ArrayList<NumberChoice>();
		}
		numbers.add(numChoice);
	}

	public double getScore() {
		return score;
	}

	public double getCoverage() {
		return coverage;
	}

	public int getMisses() {
		return misses;
	}

	public ScheduleComponent getComponent() {
		return component;
	}

	public List<Integer> getIntValues() {
		return ivalues;
	}

	public List<Integer> getIntValues2() {
		return ivalues2;
	}

	public List<String> getStringValues() {
		return svalues;
	}

	public List<WordChoice> getWords() {
		return words;
	}

	public String getString() {
		StringBuilder sb = new StringBuilder(10);
		for (WordChoice w : words) {
			sb.append(w.word);
		}
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(10);
		sb.append(component+" score = "+score);
		sb.append(" coverage = "+coverage);
		sb.append(" misses = "+misses);
		if (ivalues != null) {
			for (int i : ivalues) {
				sb.append(" "+i);
			}
		}
		if (ivalues2 != null) {
			for (int i : ivalues2) {
				sb.append(" "+i);
			}
		}
		if (svalues != null) {
			for (String s : svalues) {
				sb.append(" "+s);
			}
		}
		if (words != null) {
			for (WordChoice w : words) {
				sb.append(" "+w);
			}
		}
		if (numbers != null) {
			for (NumberChoice nc : numbers) {
				sb.append(" "+nc);
			}
		}
		return sb.toString();
	}
}

class WordChoiceScoreComparator implements Comparator<WordChoice> {
	@Override
	public int compare(WordChoice c1, WordChoice c2) {
		if (c1.getScore() > c2.getScore()) {
			return -1;
		}
		else if (c1.getScore() < c2.getScore()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

class WordChoiceNormScoreComparator implements Comparator<WordChoice> {
	@Override
	public int compare(WordChoice c1, WordChoice c2) {
		double diff = getDiff(c1.getNormScore(), c2.getNormScore());
		if (diff < 0.3) {
			if (c1.getScore() > c2.getScore()) {
				return -1;
			}
			else if (c1.getScore() < c2.getScore()) {
				return 1;
			}
			else {
				return 0;
			}
		}
		else {
			if (c1.getNormScore() > c2.getNormScore()) {
				return -1;
			}
			else if (c1.getNormScore() < c2.getNormScore()) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}

	private double getDiff(double v1, double v2) {
		double diff = Math.abs(v1 - v2);
		double max = v1 > v2 ? v1 : v2;
		double norm = max > 0 ? diff/max : 0;
		return norm;
	}
}

class WordChoiceIndexComparator implements Comparator<WordChoice> {
	@Override
	public int compare(WordChoice c1, WordChoice c2) {
		if (c1.getIndex() < c2.getIndex()) {
			return -1;
		}
		else if (c1.getIndex() > c2.getIndex()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
//
//  FIXME: normalized score is needed to compare scores of words of different lengths
//         but favors shorter words...a more balanced approach is needed
//
class WordChoice {
	public String word;
	public double score;
	public double normalizedScore;
	public int misses;
	public int index;
	public int cannonical;

	public WordChoice( String word, int index, double score, int misses) {
		this.word = word;
		this.index = index;
		this.score = score;
		this.misses = misses;
		normalizedScore = score / word.length();
	}

	public double getScore() {
		return score;
	}

	public double getNormScore() {
		return normalizedScore;
	}

	public int getMisses() {
		return misses;
	}

	public int getIndex() {
		return index;
	}

	public int getEnd() {
		return index + word.length();
	}

	public String toString() {
		return word+" score = "+score+" norm score = "+normalizedScore+" misses= "+misses+" index = "+index+" cannonical ="+cannonical;
	}
}

class NumberChoice {
	public int number;
	public double score;
	public int index;
	public int misses;

	public NumberChoice( int number, int index, double score, int misses) {
		this.number = number;
		this.index = index;
		this.score = score;
		this.misses = misses;
	}

	public int getNumDig() {
		double numDig = Math.log(number) + 1;
		return (int)numDig; 
	}

	public double getScore() {
		
		return score;
	}

	public String numAsString() {
		return Integer.toString(number);
	}

	public int getMisses() {
		return misses;
	}

	public String toString() {
		return number+" score = "+score+" index = "+index;
	}
}

class DateRangeComparator implements Comparator<DateRangeBuilder> {
	@Override
	public int compare(DateRangeBuilder c1, DateRangeBuilder c2) {
		if (c1.getChoice().getScore() > c2.getChoice().getScore()) {
			return -1;
		}
		else if (c1.getChoice().getScore() < c2.getChoice().getScore()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

class DateRangeBuilder {
	private WordChoice choice;
	private int monthDay1;
	private int monthDay2;
	private String month1;
	private String month2;
	public NumberChoice d1;
	public NumberChoice d2;
	public WordChoice m1;
	public WordChoice m2;

	public DateRangeBuilder(WordChoice choice) {
		this.choice = choice;
	}

	public void setDay1(int day1) {
		this.monthDay1 = day1;
	}

	public void setDay2(int day2) {
		this.monthDay2 = day2;
	}

	public void setMonth1(String month1) {
		this.month1 = month1;
	}

	public void setMonth2(String month2) {
		this.month2 = month2;
	}

	public WordChoice getChoice() {
		return choice;
	}

	public int getDay1() {
		return monthDay1;
	}

	public int getDay2() {
		return monthDay2;
	}

	public String getMonth1() {
		return month1;
	}

	public String getMonth2() {
		return month2;
	}

	public String toString() {
		return choice+" "+monthDay1+" "+month1+" "+monthDay2+" "+month2;
	}
}

class WeekDayComparator implements Comparator<WeekDayBuilder> {
	@Override
	public int compare(WeekDayBuilder c1, WeekDayBuilder c2) {
		if (c1.choice.score > c2.choice.score) {
			return -1;
		}
		else if (c1.choice.score < c2.choice.score) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

class WeekDayBuilder {
	public WordChoice choice;
	public List<String> days;
	public List<WordChoice> dList;

	public WeekDayBuilder(WordChoice choice) {
		this.choice = choice;
		days = new ArrayList<String>();
		dList = new ArrayList<WordChoice>();
	}

	public String toString() {
		return "size = "+dList.size()+" "+choice;
	}
}

public class ParkingSign {
	private static final double minScore = 0.5;
	private static final String[] hours = {"heures", "hours", "h", "hrs"};
	private static final String[] minutes = {"minutes", "min" };
	private static final String[] toList = { "to", "a", "au", "-"};
	private static final String[][] days;
	private static final String[][] months;

	static {
	
		days = new String[7][];
				
		days[0] = new String[] { "Sunday", "dimanche", "sun", "dim"};
		days[1] = new String[] { "Monday", "lundi", "lun", "mon"};
		days[2] = new String[] { "Tuesday", "mardi", "tue", "mar" };
		days[3] = new String[] { "Wednesday", "mercredi", "wed", "mer"};
		days[4] = new String[] { "Thursday", "jeudi", "jeu", "thur"};
		days[5] = new String[] { "Friday", "vendredi", "ven", "fri"};
		days[6] = new String[] { "Saturday", "samdi", "sam", "sat"};
/*
		days = new String[2][];
		days[0] = new String[] { "lun" };
		days[1] = new String[] { "ven" };
*/
		months = new String[12][];
		months[0] = new String[] { "January", "janvier", "jan"};
		months[1] = new String[] { "February", "fevrier", "feb", "fev"};
		months[2] = new String[] { "March", "mars", "mar"};
		months[3] = new String[] { "April", "avril", "apr", "avr"};
		months[4] = new String[] { "May", "mai"};
		months[5] = new String[] { "June", "juin", "jun"};
		months[6] = new String[] { "July", "juillet", "jul", "juil"};
		months[7] = new String[] { "August", "aout", "aug" };
		months[8] = new String[] { "September" , "septembre", "sep", "sept"};
		months[9] = new String[] { "October", "octobre", "oct"};
		months[10] = new String[] { "November", "novembre", "nov"};
		months[11] = new String[] { "December", "decembre", "dec"};
		
	}											
	public ParkingSign() {

	}

	public static ParkingSchedule readSchedule(ParkingSignType signType, List<TextGroup> textLines, Logger parentLogger) {
		Logger logger = new Logger( parentLogger, LoggingTag.Schedule, "ParkingSchedule", "readSchedule");
		Map<ScheduleComponent, ComponentChoice> components = new HashMap<ScheduleComponent, ComponentChoice>();
		for (TextGroup group : textLines) {
			ComponentChoice choice = classifyLine(group, logger);
			if (choice != null && choice.getScore() > minScore) {
				ComponentChoice existing = components.get(choice.getComponent());
				if ( existing == null || existing.getScore() < choice.getScore()) {
					components.put( choice.getComponent(), choice);
					logger.log("Using "+choice+" for text group size = "+group.size());
				}
				else {
					logger.log("Ignore "+choice+" for text group size = "+group.size());
				}
				
			}
		}
		return createSchedule( signType, components , logger);
	}

	private static ParkingSchedule createSchedule( ParkingSignType signType, Map<ScheduleComponent, ComponentChoice> components, Logger logger ) {
		ParkingSchedule schedule = new ParkingSchedule(signType, logger);
		for (Map.Entry<ScheduleComponent, ComponentChoice> entry : components.entrySet()) {
			ScheduleComponent component = entry.getKey();
			ComponentChoice choice = entry.getValue();
//			System.out.println("adding to schedule "+choice);
			switch (component) {
				case TIMEPERIOD:
					{					
						if ( signType == ParkingSignType.PARKING || signType == ParkingSignType.PARKINGMETER) {
							schedule.setTimeLimitMinutes(choice.getIntValues().get(0));
						}
						else {
							logger.error("dont know how process time limit for parking restriction");
						}
					}				
					break;
				case TIMERANGE:
					{	
						SimpleTime start = null;
						SimpleTime end = null;		
						if (choice.getIntValues2() == null) { // Case hr to hr
							start = new SimpleTime(choice.getIntValues().get(0));
							end = new SimpleTime(choice.getIntValues().get(1));							
						}
						else { // Case hr min to hr min
							start = new SimpleTime(choice.getIntValues().get(0), choice.getIntValues2().get(0));
							end = new SimpleTime(choice.getIntValues().get(1), choice.getIntValues2().get(1));
						}
						TimeRange timeRange = new TimeRange(start, end);
						schedule.setTimeRange(timeRange);											
					}
					break;
				case WEEKDAYS:
					{
						WeekDaySet weekDays = new WeekDaySet();
						List<Integer> dayNumList = choice.getIntValues();
						for (Integer dayNum : dayNumList) {
							weekDays.add(days[dayNum][0]);
						}
						schedule.setWeekDays(weekDays);
					}
					break;
				case DATES:
					{
						List<Integer> monthDayList = choice.getIntValues();
						List<String> monthNameList = choice.getStringValues();
						SimpleDate startDate = new SimpleDate(monthNameList.get(0), monthDayList.get(0));
						SimpleDate endDate = new SimpleDate(monthNameList.get(1), monthDayList.get(1));
						schedule.setDateRange(new DateRange(startDate, endDate));
					}
					break;
				case PERMIT:
				case MISC:
				default:
					logger.error("unable to process "+choice.getComponent());
			}
		}
		schedule.setFlags();
		return schedule;
	}

	private static ComponentChoice classifyLine(TextGroup textLine, Logger logger) {
		List<ComponentChoice> choices = new ArrayList<ComponentChoice>();
		logger.log("Classify line of size "+textLine.size()+" raw text: "+textLine.getRawText());
		logger.log("Baseline is "+ textLine.getBaseline());
		for ( TextShape shape : textLine.getShapes()) {
			logger.log(shape.getTextMatchesAsString()+" "+shape.getTopologyX()+" "+shape.getTopologyY());
		}
		doAddComponent(choices, checkForTimePeriod(textLine));
		doAddComponent(choices, checkForTimeRange(textLine));
		doAddComponent(choices, checkForWeekDays(textLine));
		doAddComponent(choices, checkForDates(textLine));
		doAddComponent(choices, checkForPermit(textLine));
		doAddComponent(choices, checkForRate(textLine));
		doAddComponent(choices, checkForMisc(textLine));
		ComponentChoiceComparator comparator = new ComponentChoiceComparator();
		Collections.sort(choices, comparator);
		for (ComponentChoice choice : choices) {
			logger.log(choice.toString());
		}
		if (choices.size() > 0) {
			return choices.get(0);
		}
		else {
			return null;
		}
	}

	private static void doAddComponent( List<ComponentChoice> choices, ComponentChoice choice) {
		if (choice != null) {
			choices.add(choice);
		}
	}

	private static ComponentChoice checkForTimePeriod(TextGroup textLine) {
		List<WordChoice> hChoices = matchWordsFrom(hours, 0, textLine);
		for (WordChoice hr : hChoices) {
			System.out.println(hr);
		}
		WordChoice hourChoice = matchBestWordListFrom(hours, 0, textLine);
		if (hourChoice != null) {
			System.out.println("Got hour = "+hourChoice);
		}
		WordChoice minuteChoice = matchBestWordListFrom(minutes, 0, textLine);
		if (minuteChoice != null) {
			System.out.println("Got minute = "+minuteChoice);
		}
		List<Integer> periodValues = new ArrayList<Integer>();
		if (hourChoice != null && (minuteChoice == null || hourChoice.getScore() > minuteChoice.getScore())) {
			NumberChoice hourVal = getDigits(textLine, 0, hourChoice.index-1);
			System.out.println("Got hour = "+hourChoice+" value = "+hourVal);
			if (hourVal != null) {
				periodValues.add(hourVal.number*60);
				ComponentChoice compChoice = new ComponentChoice( ScheduleComponent.TIMEPERIOD, textLine.size(), periodValues);
				compChoice.addTo(hourChoice);
				compChoice.addTo(hourVal);
				return compChoice;
			}
		}
		else if (minuteChoice != null) {
			NumberChoice minVal = getDigits(textLine, 0, minuteChoice.index-1);
			System.out.println("Got minute = "+minuteChoice+" value = "+minVal);
			if (minVal != null) {
				periodValues.add(minVal.number);
				ComponentChoice compChoice = new ComponentChoice( ScheduleComponent.TIMEPERIOD, textLine.size(), periodValues);
				compChoice.addTo(minuteChoice);
				compChoice.addTo(minVal);
				return compChoice;
			}
		}
		return null;
	}

	private static ComponentChoice checkForTimeRange(TextGroup textLine) {
		ComponentChoice[] choices = new ComponentChoice[2];
		choices[0] = timeRangeHrToHr(textLine);
		choices[1] = timeRangeHrMinToHrMin(textLine);
		ComponentChoice best = choices[0];
		for ( int i=1 ; i<choices.length ; i++) {
			if (best == null || (choices[i] != null && best.getScore() < choices[i].getScore())) {
				best = choices[i];
			}
		}
		return best;
	}

	private static ComponentChoice timeRangeHrMinToHrMin(TextGroup textLine) {
		WordChoice h1 = matchFirstWordFrom("h", 0, textLine);
		List<Integer> hourValues = new ArrayList<Integer>();
		List<Integer> minValues = new ArrayList<Integer>();
		if (h1 != null) {
			NumberChoice hourVal1 = getDigits(textLine, 0, h1.index-1);
			WordChoice h2 = matchFirstWordFrom("h", h1.index+3, textLine);
			NumberChoice minVal1 = getDigits(textLine, h1.index+1, h1.index+2);			
//			System.out.println("Found h1="+h1+" h2 = "+h2+" hourVal1 = "+hourVal1+" minVal1 ="+minVal1);
			if ( h2 != null && hourVal1 != null && minVal1 != null) {
				WordChoice to = matchFirstWordSynonymBetween(toList, h1.index+1, h2.index, textLine);
				NumberChoice hourVal2 = getDigits(textLine, h1.index+3, h2.index-1);
				NumberChoice minVal2 = getDigits(textLine, h2.index+1, h2.index+2);
//				System.out.println("Found to = "+to+" hourVal2 = "+hourVal2+" minVal2 ="+minVal2);
				if (hourVal2 != null && minVal2 != null) {
					hourValues.add(hourVal1.number);
					hourValues.add(hourVal2.number);
					minValues.add(minVal1.number);
					minValues.add(minVal2.number);
					ComponentChoice compChoice = new ComponentChoice( ScheduleComponent.TIMERANGE, textLine.size(), hourValues);
					compChoice.setIvalues2(minValues);
					compChoice.addTo(h1);
					compChoice.addTo(h2);
					compChoice.addTo(hourVal1);
					compChoice.addTo(hourVal2);
					compChoice.addTo(minVal1);
					compChoice.addTo(minVal2);
					compChoice.addTo(to);
					return compChoice;
				}
			}
		}
		else {
//			System.out.println("no time range found");
		}
		return null;
	}

	private static ComponentChoice timeRangeHrToHr(TextGroup textLine) {
		WordChoice h1 = matchFirstWordFrom("h", 0, textLine);
		List<Integer> rangeValues = new ArrayList<Integer>();
		if (h1 != null) {			
			NumberChoice hourVal1 = getDigits(textLine, 0, h1.index-1);
			WordChoice h2 = matchFirstWordFrom("h", h1.index+1, textLine);
//			System.out.println("Found h1="+h1+" h2 = "+h2+" hourVal1 = "+hourVal1);
			if ( h2 != null && hourVal1 != null) {
				WordChoice to = matchFirstWordSynonymBetween(toList, h1.index+1, h2.index, textLine);
				int h2start = to == null ? h1.index+1 : to.index+1;
				NumberChoice hourVal2 = getDigits(textLine, h1.index+1, h2.index-1);
//				System.out.println("Found to = "+to+" hourVal2 = "+hourVal2);
				if (hourVal2 != null) {
					rangeValues.add(hourVal1.number);
					rangeValues.add(hourVal2.number);
					ComponentChoice compChoice = new ComponentChoice( ScheduleComponent.TIMERANGE, textLine.size(), rangeValues);
					compChoice.addTo(h1);
					compChoice.addTo(h2);
					compChoice.addTo(hourVal1);
					compChoice.addTo(hourVal2);
					compChoice.addTo(to);
					return compChoice;
				}
			}
		}
		else {
//			System.out.println("no time range found");
		}
		return null;
	}

	private static ComponentChoice checkForWeekDays(TextGroup textLine) {
		ComponentChoice[] choices = new ComponentChoice[2];
		choices[0] = weekCheck1(textLine);
		System.out.println("date check1 is "+choices[0]);
		choices[1] = weekCheck2(textLine);
		System.out.println("date check2 is "+choices[1]);
		ComponentChoice best = choices[0];
		for ( int i=1 ; i<choices.length ; i++) {
			if (best == null || (choices[i] != null && best.getScore() < choices[i].getScore())) {
				best = choices[i];
			}
		}
		return best;
	}


	private static ComponentChoice weekCheck1(TextGroup textLine) {
		List<WeekDayBuilder> day1List = getWeekDaysAt(0, textLine);
		List<WeekDayBuilder> twoDaysList = addWeekDay(day1List, textLine);
		List<WeekDayBuilder> threeDaysList = addWeekDay(twoDaysList, textLine);
		WeekDayComparator comparator = new  WeekDayComparator();
		Collections.sort(threeDaysList, comparator);
/*		
		for (WeekDayBuilder wdb : threeDaysList) {
			System.out.println(wdb);
			for ( int i=0 ; i<wdb.days.size() ; i++) {
				System.out.println("  "+i+":"+wdb.days.get(i)+" "+wdb.dList.get(i));
			}
		}
*/		
		if (threeDaysList.size() > 0) {
			WeekDayBuilder top = threeDaysList.get(0);
			List<Integer> dayNumList = new ArrayList<Integer>();
			for (WordChoice c : top.dList) {
				dayNumList.add(c.cannonical);
			}
			ComponentChoice compChoice = new ComponentChoice( ScheduleComponent.WEEKDAYS, textLine.size(), dayNumList);
			compChoice.addTo(top.choice);
			return compChoice; 
		}
		return null;
	}

	//
	// FIXME: should eliminate stuff like Wednesday+Wednesday or Friday+Monday i.e. only add days that follow one at the end of the base
	// FIXME2: no point in mixing forms & language like Lun+Tuesday
	//
	private static List<WeekDayBuilder> addWeekDay(List<WeekDayBuilder> baseList, TextGroup textLine) {		
		List<WeekDayBuilder> moreDaysList = new ArrayList<WeekDayBuilder>();
		for (WeekDayBuilder wdb1 : baseList) {
			WordChoice wc1 = wdb1.choice;
			if (wc1.word.length() >= textLine.size()) {
				moreDaysList.add(wdb1);
			}
			else {
				List<WeekDayBuilder> addList = getWeekDaysAt(wc1.getEnd(), textLine);
				for ( WeekDayBuilder wdb2 : addList) {
					WordChoice wc2 = wdb2.choice;
					WordChoice wc = new WordChoice( wc1.word+wc2.word, wc1.index, wc1.score+wc2.score, wc1.misses+wc2.misses);
					WeekDayBuilder wdb = new WeekDayBuilder(wc);
					wdb.days.add(wdb1.days.get(0));
					wdb.days.add(wdb2.days.get(0));
					wdb.dList.add(wdb1.dList.get(0));
					wdb.dList.add(wdb2.dList.get(0));
					moreDaysList.add(wdb);
				}
			}
		}
		return moreDaysList;		
	}

	private static List<WeekDayBuilder> getWeekDaysAt(int index, TextGroup textLine) {
		List<WeekDayBuilder> dayList = new ArrayList<WeekDayBuilder>();
		for ( int i=0 ; i<days.length ; i++) {
			List<WordChoice> day1 = matchWordsAt(days[i], index, textLine);
			for (WordChoice d1 : day1) {
				if (d1.getScore() > 0) {
					d1.cannonical = i;
					WeekDayBuilder wdb = new WeekDayBuilder(d1);
					wdb.days.add(days[i][0]);
					wdb.dList.add(d1);
					dayList.add(wdb);
				}
			}
		}	
		return dayList;
	}

	private static ComponentChoice weekCheck2(TextGroup textLine) {
		List<WordChoice> daysFound = new ArrayList<WordChoice>();
		List<Integer> dayNumList = new ArrayList<Integer>();
		for ( int i=0 ; i<days.length ; i++) {
//			WordChoice day =  matchBestWordListFromDebug(days[i], 0, textLine);			
			WordChoice day =  matchBestWordListFrom(days[i], 0, textLine);
//			System.out.println("best match for "+days[i][0]+" is "+day);
			day.cannonical = i;
			if (day.getScore() > 0) {
				daysFound.add(day);
			}
		}
		if (daysFound.size() == 0) {
			return null;
		}
		WordChoiceIndexComparator comparator = new  WordChoiceIndexComparator();
		Collections.sort(daysFound, comparator);
		
		Map<Integer, WordChoice> bestAt = new HashMap<Integer, WordChoice>();
		for (WordChoice day : daysFound) {
			WordChoice best = bestAt.get(day.index);
			if (best == null || best.score < day.score) {
				bestAt.put( day.index, day);
			}
		}

		List<Integer> keys = new ArrayList<Integer>(bestAt.keySet());			
		for (int i=0 ; i<keys.size() ; i++) {
			int index = keys.get(i);
			WordChoice best1 = bestAt.get(index);
			int end1 = index + best1.word.length()-1;
			for ( int j=i+1 ; j<keys.size() ; j++) {
				int idx = keys.get(j);
				WordChoice best2 = bestAt.get(idx);
				if (idx <= end1) { // a collision, only one can be retained
					if (best1.score > best2.score) {
						best2.score = -1; // to remove
					}
					else {
						best1.score = -1;
					}
				}
			}
		}
		daysFound.clear();
		double score = 0.0;
		for ( Integer index : bestAt.keySet()) {
			WordChoice day = bestAt.get(index);
			if (day.score > 0) {
				score += day.score;
				dayNumList.add(day.cannonical);
				daysFound.add(day);
			}
		}
		ComponentChoice compChoice = null;
		if (daysFound.size() == 2) {
			WordChoice day1 = daysFound.get(0);
			WordChoice day2 = daysFound.get(1);
			int start = day1.index + day1.word.length();
//			System.out.println("day1 = "+day1+" day2="+day2+" look for to at"+start);
			WordChoice to = matchFirstWordSynonymBetween(toList, start, day2.index, textLine);
			if (to != null) {
//				System.out.println("found "+to);
				dayNumList.clear();
				for ( int i=day1.cannonical ; i<=day2.cannonical ; i++) {
					dayNumList.add(i);										
				}
			}
			else {
//				System.out.println("to not found ");
			}
			compChoice = new ComponentChoice( ScheduleComponent.WEEKDAYS, textLine.size(), dayNumList);
			compChoice.addTo(day1);
			compChoice.addTo(day2);
			compChoice.addTo(to);

		}
		else if (daysFound.size() > 0) {
			compChoice = new ComponentChoice( ScheduleComponent.WEEKDAYS, textLine.size(), dayNumList);
			for (WordChoice w : daysFound) {
				compChoice.addTo(w);
			}
		}
		return compChoice;		 
	}

	private static ComponentChoice checkForDates(TextGroup textLine) {
		ComponentChoice[] choices = new ComponentChoice[2];
		choices[0] = dateCheck1(textLine);
		System.out.println("date check1 is "+choices[0]);
		choices[1] = dateCheck2(textLine);
		System.out.println("date check2 is "+choices[1]);
		ComponentChoice best = choices[0];
		for ( int i=1 ; i<choices.length ; i++) {
			if (best == null || (choices[i] != null && best.getScore() < choices[i].getScore())) {
				best = choices[i];
			}
		}
		return best;
	}

	private static ComponentChoice dateCheck1(TextGroup textLine) {
		List<DateRangeBuilder> date1 = getDatesAt(0, textLine);
		List<DateRangeBuilder> dateTo = new ArrayList<DateRangeBuilder>();
		for (DateRangeBuilder date : date1) {
			WordChoice dwc = date.getChoice();
			List<WordChoice> to = matchWordsAt(toList, dwc.getEnd(), textLine);
			for (WordChoice t : to) {
				if (t.getScore() > 0) {
					WordChoice wc = new WordChoice(dwc.word + t.word, dwc.index, dwc.getScore() + t.getScore(), dwc.getMisses());
					DateRangeBuilder d2 = new DateRangeBuilder(wc);
					d2.setDay1(date.getDay1());
					d2.setMonth1(date.getMonth1());
					d2.m1 = date.m1;
					d2.d1 = date.d1;
					dateTo.add(d2);
				}
			}
			dateTo.add( date );
		}
		List<DateRangeBuilder> dateRange = new ArrayList<DateRangeBuilder>();
		for (DateRangeBuilder dt : dateTo ) {
			WordChoice dtwc = dt.getChoice();
			List<DateRangeBuilder> date2 = getDatesAt(dtwc.getEnd(), textLine);
			for (DateRangeBuilder d2 : date2) {
				WordChoice d2wc = d2.getChoice();
				if (d2wc.getScore() > 0) {
					WordChoice wc = new WordChoice(dtwc.word+d2wc.word, dtwc.index, dtwc.getScore()+d2wc.getScore(), dtwc.getMisses()+d2wc.getMisses());
					DateRangeBuilder fb = new DateRangeBuilder(wc);
					fb.setDay1(dt.getDay1());
					fb.setMonth1(dt.getMonth1());
					fb.setDay2(d2.getDay1());
					fb.setMonth2(d2.getMonth1());
					fb.d1 = dt.d1;
					fb.m1 = dt.m1;
					fb.d2 = d2.d1;
					fb.m2 = d2.m1;
					dateRange.add(fb);
				}
			}
		}
		DateRangeComparator comparator = new  DateRangeComparator();
		Collections.sort(dateRange, comparator);
/*		
		for (DateRangeBuilder dr : dateRange) {
			System.out.println(dr);
			System.out.println("   d1 = "+dr.d1);
			System.out.println("   d2 = "+dr.d2);
			System.out.println("   m1 = "+dr.m1);
			System.out.println("   m2 = "+dr.m2);
		}
*/		
		if ( dateRange.size() > 0) {
			DateRangeBuilder top = dateRange.get(0);
			List<Integer> mDays = new ArrayList<Integer>();
			mDays.add(top.getDay1());
			mDays.add(top.getDay2());
			List<String> mths = new ArrayList<String>();
			mths.add(top.getMonth1());
			mths.add(top.getMonth2());
			ComponentChoice compChoice = new ComponentChoice( ScheduleComponent.DATES, textLine.size(), mDays, mths);
			compChoice.addTo(top.getChoice());
			return compChoice;
		}
		return null;
	}

	private static List<DateRangeBuilder> getDatesAt(int index, TextGroup textLine) {
		List<DateRangeBuilder> dates = new ArrayList<DateRangeBuilder>();
		for ( int i=0 ; i<months.length ; i++) {
			List<WordChoice> month = matchWordsFrom(months[i], index, textLine);
			for (WordChoice m : month) {
				if (m != null) {
					NumberChoice monthDay = getDigits(textLine, index, m.index-1);
					if (monthDay != null && monthDay.number > 0) {
						String dateString = monthDay.numAsString()+m.word; // no spaces
						WordChoice choice = new WordChoice(dateString, monthDay.index, monthDay.score + m.score, monthDay.misses + m.misses);
						DateRangeBuilder date = new DateRangeBuilder(choice);
						date.setDay1(monthDay.number);
						date.setMonth1(months[i][0]);
						date.m1 = m;
						date.d1 = monthDay;
						dates.add(date);
					}
				}
			}			
		}
		return dates;
	}

	private static ComponentChoice dateCheck2(TextGroup textLine) {
		List<WordChoice> monthsFound = new ArrayList<WordChoice>();
		List<Integer> monthDays = new ArrayList<Integer>();
		List<String> monthNames = new ArrayList<String>();
		for ( int i=0 ; i<months.length ; i++) {
			WordChoice month = matchBestWordListFrom(months[i], 0, textLine);
//			System.out.println("best match for "+months[i][0]+" is "+month);
			month.cannonical = i;
			if (month.getScore() > 0) {
				monthsFound.add(month);
			}
		}
		WordChoiceIndexComparator comparator = new  WordChoiceIndexComparator();
		Collections.sort(monthsFound, comparator);

		Map<Integer, WordChoice> bestAt = new HashMap<Integer, WordChoice>();
		for (WordChoice month : monthsFound) {
			WordChoice best = bestAt.get(month.index);
			if (best == null || best.score < month.score) {
				bestAt.put( month.index, month);
			}
		}

		List<Integer> keys = new ArrayList<Integer>(bestAt.keySet());			
		for (int i=0 ; i<keys.size() ; i++) {
//			System.out.println("Got best at "+keys.get(i)+" "+bestAt.get(keys.get(i)));
			int index = keys.get(i);
			WordChoice best1 = bestAt.get(index);
			int end1 = index + best1.word.length()-1;
			for ( int j=i+1 ; j<keys.size() ; j++) {
				int idx = keys.get(j);
				WordChoice best2 = bestAt.get(idx);
				if (idx <= end1) { // a collision, only one can be retained
					if (best1.score > best2.score) {
						best2.score = -1; // to remove
					}
					else {
						best1.score = -1;
					}
				}
			}
		}

		monthsFound.clear();
		for ( Integer index : bestAt.keySet()) {
			WordChoice month = bestAt.get(index);
			if (month.score > 0) {				
				monthsFound.add(month);
//				System.out.println("retain "+month+" "+month.index+" "+(month.index+month.word.length()));
			}
		}

		WordChoiceScoreComparator comparator2 = new  WordChoiceScoreComparator();
		Collections.sort(monthsFound, comparator2);

		if (monthsFound.size() >= 2) {
			WordChoice firstMonth = monthsFound.get(0);
			WordChoice secondMonth = monthsFound.get(1);
			if (firstMonth.index > secondMonth.index) {
				firstMonth = monthsFound.get(1);
				secondMonth = monthsFound.get(0);
			}
//			System.out.println("first = "+firstMonth+" second = "+secondMonth);
			WordChoice to = matchFirstWordSynonymBetween(toList, firstMonth.index+1, secondMonth.index-1, textLine);
			NumberChoice monthDay1 = getDigits(textLine, 0, firstMonth.index-1);
			NumberChoice monthDay2 = null;
			if (monthDay1 == null) {
				int end = to == null ?  secondMonth.index-1 : to.index-1;
				monthDay1 = getDigits(textLine, firstMonth.index+1, end);
				monthDay2 = getDigits(textLine, secondMonth.index, textLine.size()-1);
			}
			else {
				int start = to == null ? firstMonth.index+1 : to.index+1;
				monthDay2 = getDigits(textLine, start, secondMonth.index-1);
			}
//			System.out.println("day1 = "+monthDay1+" day2 = "+monthDay2+" to = "+to);
			if (monthDay1 != null && monthDay2 != null) {								
				monthDays.add(monthDay1.number);
				monthDays.add(monthDay2.number);
				monthNames.add(months[firstMonth.cannonical][0]);
				monthNames.add(months[secondMonth.cannonical][0]);				
				ComponentChoice compChoice = new ComponentChoice( ScheduleComponent.DATES, textLine.size(), monthDays, monthNames);
				compChoice.addTo(monthDay1);
				compChoice.addTo(monthDay2);
				compChoice.addTo(firstMonth);
				compChoice.addTo(secondMonth);
				compChoice.addTo(to);
				return compChoice;
			}
		}
		return null; // 2 or bust
	}

	private static ComponentChoice checkForPermit(TextGroup textLine) {
		return null;
	}

	private static ComponentChoice checkForRate(TextGroup textLine) {
		return null;
	}

	private static ComponentChoice checkForMisc(TextGroup textLine) {
		return null;
	}

	private static WordChoice matchBestWordListFrom(String[] words, int start, TextGroup textLine) {
		List<WordChoice> choiceList = matchWordsFrom(words, start, textLine);
		if (choiceList.size() == 0) {
			return null;
		}
		WordChoice top = choiceList.get(0);		
		int n=0; // n is the number of ties at the top
		while ( n<choiceList.size() && choiceList.get(n).getNormScore() == top.getNormScore() ) {
			n++;
		}
		for ( int i=0 ; i<n ; i++) { // use aggregate score to pick a winner
			if (choiceList.get(i).getScore() > top.getScore()) {
				top = choiceList.get(i);
			}
		}
		return top;	
	}

	private static List<WordChoice> matchWordsFrom(String[] words, int start, TextGroup textLine) {
		List<WordChoice> choiceList = new ArrayList<WordChoice>();
		int i;
		for ( i=0 ; i<words.length ; i++) {
			WordChoice choice = matchBestWordFrom(words[i], start, textLine);
			if (choice != null) {				
				choiceList.add(choice);
			}
		}
		WordChoiceNormScoreComparator comparator = new WordChoiceNormScoreComparator();
		Collections.sort(choiceList, comparator);
		return choiceList;			
	}

	private static WordChoice matchBestWordFrom(String word, int start, TextGroup textLine) {
		List<WordChoice> matchList = new ArrayList<WordChoice>();
		for ( int i=start ; i<textLine.size() ; i++) {
			matchList.add(matchWordAt(word, i, textLine));
		}
		WordChoiceScoreComparator comparator = new  WordChoiceScoreComparator();
		Collections.sort(matchList, comparator);
		if (matchList.size() > 0) {
			return matchList.get(0);
		}
		else {
			return null;
		}
	}

	private static WordChoice matchFirstWordSynonymBetween( String[] words, int start, int end, TextGroup textLine) {
		List<WordChoice> synList = new ArrayList<WordChoice>();
		for ( int i=0 ; i<words.length ; i++) {
			WordChoice match = matchFirstWordFromTo(words[i], start, end, textLine);
			if (match != null) {
				synList.add(match);
			}
		}
		WordChoiceScoreComparator comparator = new  WordChoiceScoreComparator();
		Collections.sort(synList, comparator);
		if (synList.size() > 0) {
			return synList.get(0); // return synonym with top score
		}
		else {
			return null;
		}
	}

	private static WordChoice matchFirstWordFrom(String word, int start, TextGroup textLine) {				
		for ( int i=start ; i<textLine.size() ; i++) {
			WordChoice choice = matchWordAt(word, i, textLine);
			if (choice.score > 0) {
				return choice;
			}
		}		
		return null;
	}

	private static WordChoice matchFirstWordFromTo(String word, int start, int end, TextGroup textLine) {
		int realEnd = end - word.length() + 1;				
		for ( int i=start ; i<realEnd ; i++) {
			WordChoice choice = matchWordAt(word, i, textLine);
			if (choice.score > 0) {
				return choice;
			}
		}		
		return null;
	}

	private static List<WordChoice> matchWordsAt(String[] words, int index, TextGroup textLine) {
		List<WordChoice> results = new ArrayList<WordChoice>();
		for ( int i=0 ; i<words.length ; i++ ) {
			WordChoice res = matchWordAt(words[i], index, textLine);
			results.add(res);
		}
		return results;
	}

	private static WordChoice matchWordAt(String word, int index, TextGroup textLine) {
		char[] wordChar = word.toCharArray();
		double score = 0;
		int misses = 0;
		int i,k;
		for ( k=0, i=index ; i<textLine.getShapes().size() && k<wordChar.length ; i++, k++) {
			TextShape shape = textLine.getShapes().get(i);
			double charScore = getCharScore(wordChar[k], shape);
			if (charScore > 0.0) {
				score += charScore;
			}
			else {
				misses++;
			}
		}
		return new WordChoice( word, index, score, misses);
	}

	private static double getCharScore( char value, TextShape shape) {
		char lowValue = Character.toLowerCase(value);
		if (shape.getTextMatches() == null) {
			return 0;
		}
		for (TextChoice choice : shape.getTextMatches()) {
			char lowChoice = Character.toLowerCase(choice.getText());
			if ( lowChoice == lowValue ) {
				return choice.getScore();
			}
		}
		return 0; // not found
	}



	private static NumberChoice getDigits(TextGroup textLine, int start, int end) {
		StringBuilder sb = new StringBuilder(10);
		double score = 0.0;
		int first = -1;
		int misses = 0;
		if (start >= 0 && end >= start) {
			for ( int i=start ; i<=end && i<textLine.size() ; i++) {
				TextChoice digit = getTopDigit( textLine.getShapes().get(i));
				if (digit != null) {
					sb.append(digit.getText());
					score += digit.getScore();
					first = first < 0 ? i : first;
				}
				else {
					misses++;
					if (first > -1) {
						break;
					}
				}
			}
		}
		String digString = sb.toString();
		if (digString.length() > 0) {
			int ival = Integer.parseInt(digString);
			return new NumberChoice(ival, first, score, misses);
		}
		return null;
	}

	private static TextChoice getTopDigit( TextShape shape) {
		if (shape.getTextMatches() == null) {
			return null;
		}
		for (TextChoice choice : shape.getTextMatches()) {
			if (Character.isDigit(choice.getText())) {
				return choice;
			}
		}
		return null;
	}

}
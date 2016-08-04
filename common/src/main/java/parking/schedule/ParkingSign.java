package parking.schedule;

import parking.opencv.TextGroup;
import parking.opencv.TextShape;
import parking.opencv.TextChoice;

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
	private List<Integer> ivalues;
	private List<String> svalues;

	public ComponentChoice( ScheduleComponent component, double score,  List<Integer> ivalues) {
		this.component = component;
		this.score = score;
		this.ivalues = ivalues;
	}

	public ComponentChoice( ScheduleComponent component, double score,  List<Integer> ivalues, List<String> svalues) {
		this.component = component;
		this.score = score;
		this.ivalues = ivalues;
		this.svalues = svalues;
	}

	public double getScore() {
		return score;
	}

	public ScheduleComponent getComponent() {
		return component;
	}

	public List<Integer> getIntValues() {
		return ivalues;
	}

	public List<String> getStringValues() {
		return svalues;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(10);
		sb.append(component+" score = "+score);
		if (ivalues != null) {
			for (int i : ivalues) {
				sb.append(" "+i);
			}
		}
		if (svalues != null) {
			for (String s : svalues) {
				sb.append(" "+s);
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

class WordChoice {
	public String word;
	public double score;
	public int index;
	public int cannonical;

	public WordChoice( String word, int index, double score) {
		this.word = word;
		this.index = index;
		this.score = score;
	}

	public double getScore() {
		return score;
	}

	public int getIndex() {
		return index;
	}

	public String toString() {
		return word+" score = "+score+" index = "+index+" cannonical ="+cannonical;
	}
}

class NumberChoice {
	public int number;
	public double score;
	public int index;

	public NumberChoice( int number, int index, double score) {
		this.number = number;
		this.index = index;
		this.score = score;
	}

	public double getScore() {
		double numDig = Math.log(number) + 1; 
		return score;
	}

	public String toString() {
		return number+" score = "+score+" index = "+index;
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

	public static ParkingSchedule readSchedule(ParkingSignType signType, List<TextGroup> textLines) {
		Map<ScheduleComponent, ComponentChoice> components = new HashMap<ScheduleComponent, ComponentChoice>();
		for (TextGroup group : textLines) {
			ComponentChoice choice = classifyLine(group);
			if (choice != null && choice.getScore() > minScore) {
				ComponentChoice existing = components.get(choice.getComponent());
				if ( existing == null || existing.getScore() < choice.getScore()) {
					components.put( choice.getComponent(), choice);
//					System.out.println("Using "+choice+" for text group size = "+group.size());
				}
				else {
//					System.out.println("Ignore "+choice+" for text group size = "+group.size());
				}
				
			}
		}
		return createSchedule( signType, components );
	}

	private static ParkingSchedule createSchedule( ParkingSignType signType, Map<ScheduleComponent, ComponentChoice> components ) {
		ParkingSchedule schedule = new ParkingSchedule(signType);
		for (Map.Entry<ScheduleComponent, ComponentChoice> entry : components.entrySet()) {
			ScheduleComponent component = entry.getKey();
			ComponentChoice choice = entry.getValue();
			switch (component) {
				case TIMEPERIOD:
					{					
						if ( signType == ParkingSignType.PARKING || signType == ParkingSignType.PARKINGMETER) {
							schedule.setTimeLimitMinutes(choice.getIntValues().get(0));
						}
						else {
//							System.out.println("ERROR: dont know how process time limit for parking restriction");
						}
					}				
					break;
				case TIMERANGE:
					{						
						SimpleTime start = new SimpleTime(choice.getIntValues().get(0));
						SimpleTime end = new SimpleTime(choice.getIntValues().get(1));
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
					System.out.println("ERROR: unable to process "+choice.getComponent());
			}
		}
		schedule.setFlags();
		return schedule;
	}

	private static ComponentChoice classifyLine(TextGroup textLine) {
		List<ComponentChoice> choices = new ArrayList<ComponentChoice>();
		System.out.println("Classify line of size "+textLine.size()+" raw text: "+textLine.getRawText());
		System.out.println("Baseline is "+ textLine.getBaseline());
		for ( TextShape shape : textLine.getShapes()) {
			System.out.println(shape.getTextMatchesAsString()+" "+shape.getTopologyX()+" "+shape.getTopologyY());
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
			System.out.println(choice);
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
		WordChoice hourChoice = matchBestWordListFrom(hours, 0, textLine);
		WordChoice minuteChoice = matchBestWordListFrom(minutes, 0, textLine);
		List<Integer> periodValues = new ArrayList<Integer>();
		double score = 0.0;
		if (hourChoice != null && (minuteChoice == null || hourChoice.getScore() > minuteChoice.getScore())) {
			NumberChoice hourVal = getDigits(textLine, 0, hourChoice.index-1);
//			System.out.println("Got hour = "+hourChoice+" value = "+hourVal);
			if (hourVal != null) {
				score = hourChoice.getScore()+hourVal.getScore();
				periodValues.add(hourVal.number*60);
				return new ComponentChoice( ScheduleComponent.TIMEPERIOD, score, periodValues);
			}
		}
		else if (minuteChoice != null) {
			NumberChoice minVal = getDigits(textLine, 0, minuteChoice.index-1);
//			System.out.println("Got minute = "+minuteChoice+" value = "+minVal);
			if (minVal != null) {
				score = minuteChoice.getScore()+minVal.getScore();
				periodValues.add(minVal.number);
				return new ComponentChoice( ScheduleComponent.TIMEPERIOD, score, periodValues);
			}
		}
		return null;
	}

	private static ComponentChoice checkForTimeRange(TextGroup textLine) {
		WordChoice h1 = matchFirstWordFrom("h", 0, textLine);
		List<Integer> rangeValues = new ArrayList<Integer>();
		if (h1 != null) {			
			NumberChoice hourVal1 = getDigits(textLine, 0, h1.index-1);
			WordChoice h2 = matchFirstWordFrom("h", h1.index+1, textLine);
//			System.out.println("Found h1="+h1+" h2 = "+h2+" hourVal1 = "+hourVal1);
			if ( h2 != null && hourVal1 != null) {
				WordChoice to = matchFirstWordSynonymBetween(toList, h1.index+1, h2.index, textLine);
//				int h2start = to == null ? h1.index+1 : to.index+1;
				NumberChoice hourVal2 = getDigits(textLine, h1.index+1, h2.index-1);
//				System.out.println("Found to = "+to+" hourVal2 = "+hourVal2);
				if (hourVal2 != null) {
					double score = h1.getScore() + h2.getScore() + hourVal1.getScore() + hourVal2.getScore();
					rangeValues.add(hourVal1.number);
					rangeValues.add(hourVal2.number);
					return new ComponentChoice( ScheduleComponent.TIMERANGE, score, rangeValues);
				}
			}
		}
		else {
//			System.out.println("no time range found");
		}
		return null;
	}

	private static ComponentChoice checkForWeekDays(TextGroup textLine) {
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
		
		if (daysFound.size() == 2) {
			WordChoice day1 = daysFound.get(0);
			WordChoice day2 = daysFound.get(1);
			int start = day1.index + day1.word.length();
			System.out.println("day1 = "+day1+" day2="+day2+" look for to at"+start);
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
		}
		return new ComponentChoice( ScheduleComponent.WEEKDAYS, score, dayNumList); 
	}

	private static ComponentChoice checkForDates(TextGroup textLine) {
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
				double score = monthDay1.score + monthDay2.score + firstMonth.score + secondMonth.score;
				score = to == null ? score : score + to.score;
				return new ComponentChoice( ScheduleComponent.DATES, score, monthDays, monthNames);
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
		List<WordChoice> choiceList = new ArrayList<WordChoice>();
		for ( int i=0 ; i<words.length ; i++) {
			choiceList.add(matchBestWordFrom(words[i], start, textLine));
		}
		WordChoiceScoreComparator comparator = new WordChoiceScoreComparator();
		Collections.sort(choiceList, comparator);
		if (choiceList.size() > 0) {
			return choiceList.get(0);
		}
		else {
			return null;
		}
	}

	private static WordChoice matchBestWordListFromDebug(String[] words, int start, TextGroup textLine) {
		List<WordChoice> choiceList = new ArrayList<WordChoice>();
		for ( int i=0 ; i<words.length ; i++) {
			choiceList.add(matchBestWordFrom(words[i], start, textLine));
//			choiceList.add(matchBestWordFromDebug(words[i], start, textLine));
		}
		WordChoiceScoreComparator comparator = new WordChoiceScoreComparator();
		Collections.sort(choiceList, comparator);
		if (choiceList.size() > 0) {
			return choiceList.get(0);
		}
		else {
			return null;
		}
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

	private static WordChoice matchBestWordFromDebug(String word, int start, TextGroup textLine) {
		List<WordChoice> matchList = new ArrayList<WordChoice>();
		for ( int i=start ; i<textLine.size() ; i++) {
			matchList.add(matchWordAtDebug(word, i, textLine));
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

	private static WordChoice matchWordAt(String word, int index, TextGroup textLine) {
		char[] wordChar = word.toCharArray();
		double score = 0;
		int i,k;
		for ( k=0, i=index ; i<textLine.getShapes().size() && k<wordChar.length ; i++, k++) {
			TextShape shape = textLine.getShapes().get(i);
			score += getCharScore(wordChar[k], shape);
		}
		score = score / word.length();
		return new WordChoice( word, index, score);
	}

	private static WordChoice matchWordAtDebug(String word, int index, TextGroup textLine) {
//		System.out.println("match word "+word+" at "+index);
		char[] wordChar = word.toCharArray();
		double score = 0;
		int i,k;
		for ( k=0, i=index ; i<textLine.getShapes().size() && k<wordChar.length ; i++, k++) {
			TextShape shape = textLine.getShapes().get(i);
			score += getCharScoreDebug(wordChar[k], shape);
		}
		score = score / word.length();
		return new WordChoice( word, index, score);
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

	private static double getCharScoreDebug( char value, TextShape shape) {
		char lowValue = Character.toLowerCase(value);
		if (shape.getTextMatches() == null) {
			return 0;
		}
		for (TextChoice choice : shape.getTextMatches()) {
			char lowChoice = Character.toLowerCase(choice.getText());
			if ( lowChoice == lowValue ) {
//				System.out.println("first match for "+value+" is "+choice.getText()+" score = "+choice.getScore());
				return choice.getScore();
			}
		}
		return 0; // not found
	}

	private static NumberChoice getDigits(TextGroup textLine, int start, int end) {
		StringBuilder sb = new StringBuilder(10);
		double score = 0.0;
		if (start >= 0 && end >= start) {
			for ( int i=start ; i<=end && i<textLine.size() ; i++) {
				TextChoice digit = getTopDigit( textLine.getShapes().get(i));
				if (digit != null) {
					sb.append(digit.getText());
					score += digit.getScore();
				}
			}
		}
		String digString = sb.toString();
		if (digString.length() > 0) {
			int ival = Integer.parseInt(digString);
			return new NumberChoice(ival, start, score);
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
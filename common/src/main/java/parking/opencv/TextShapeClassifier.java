package parking.opencv;

import parking.util.Logger;
import parking.util.LoggingTag;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TextShapeClassifier {
	
	private static TextShapeClassifier m_instance;
	private static Logger m_logger;
	private static final Map<String, char[]> classifier;
	private static final Map<String, TextProperties> properties;
	static {
		classifier = new HashMap<String, char[]>();
		properties = new HashMap<String, TextProperties>();
	}

    public static TextShapeClassifier getInstance(Logger logger) {
        if (m_instance == null) {
        	m_instance = new TextShapeClassifier(logger);
        }
        return m_instance;
    }

	private TextShapeClassifier(Logger logger)  {
		m_logger = new Logger(logger, this, LoggingTag.Shape);
		buildClassifier();
		buildProperties();
	}

	private void buildProperties() {

		char[] lowerCaseLetter = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		char[] upperCaseLetter = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		char[] digits = "0123456789".toCharArray();
		properties.put( "0", new TextProperties(1.5, new TextBars(6)));
		properties.put( "1", new TextProperties(3.0, new TextBars(0,3,4,5,6), new TextBars2(0,6,7)));
		properties.put( "2", new TextProperties(1.8, new TextBars(2,5)));
		properties.put( "3", new TextProperties(1.6, new TextBars(4,5)));
		properties.put( "4", new TextProperties(1.5, new TextBars(0,4)));
		properties.put( "5", new TextProperties(1.6, new TextBars(1,4)));
		properties.put( "6", new TextProperties(1.5, new TextBars(1), new TextBars(0,1)));
		properties.put( "7", new TextProperties(1.5, new TextBars("blank")));
		properties.put( "8", new TextProperties(1.7, new TextBars()));
		properties.put( "9", new TextProperties(1.7, new TextBars(4), new TextBars(3,4)));
		properties.put( "a", new TextProperties(1.0, new TextBars(0,1,5))); // FIXME
		properties.put( "b", new TextProperties(1.4, new TextBars(0,1)));
		properties.put( "c", new TextProperties(1.0, new TextBars(0,1,2,5)));
		properties.put( "d", new TextProperties(1.4, new TextBars(0,5)));
		properties.put( "e", new TextProperties(1.2, new TextBars(0,1,5))); // FIXME
		properties.put( "f", new TextProperties(2.0, new TextBars(1,2,3), new TextBars2(1,2,3,6,7)));
		properties.put( "g", new TextProperties(1.0, new TextBars(4))); // FIXME
		properties.put( "h", new TextProperties(1.8, new TextBars(0,1,3)));
		properties.put( "i", new TextProperties(1.0, new TextBars2(7)));
		properties.put( "j", new TextProperties(1.0, new TextBars2(4,6,7)));
		properties.put( "k", new TextProperties(1.0, new TextBars2(6,7,9,11)));
		properties.put( "l", new TextProperties(5.0, new TextBars(0,3,4,5,6))); 
		properties.put( "m", new TextProperties(1.0, new TextBars("blank"))); // FIXME
		properties.put( "n", new TextProperties(1.0, new TextBars(0,1,3,5)));
		properties.put( "o", new TextProperties(0.9, new TextBars(0,1,5)));
		properties.put( "p", new TextProperties(1.0, new TextBars(2,3)));
		properties.put( "q", new TextProperties(1.0, new TextBars(3,4)));
		properties.put( "r", new TextProperties(1.4, new TextBars(0,1,2,3,5), new TextBars2(1,6,7) ));
		properties.put( "s", new TextProperties(1.3, new TextBars(1,4)));
		properties.put( "t", new TextProperties(1.7, new TextBars(0,1,2)));
		properties.put( "u", new TextProperties(1.0, new TextBars(0,1,5,6)));
		properties.put( "v", new TextProperties(0.9, new TextBars2(8,9)));
		properties.put( "w", new TextProperties(1.0, new TextBars("blank"))); // FIXME
		properties.put( "x", new TextProperties(1.0, new TextBars2(8,9,10,11))); // FIXME
		properties.put( "y", new TextProperties(1.0, new TextBars2(8,9,10))); // FIXME
		properties.put( "z", new TextProperties(1.0, new TextBars2(0,1,4,5,9,10))); // FIXME
		properties.put( "A", new TextProperties(1.8, new TextBars("blank"))); // FIXME
		properties.put( "B", new TextProperties(1.5, new TextBars()));
		properties.put( "C", new TextProperties(1.5, new TextBars(1,2,6)));
		properties.put( "D", new TextProperties(1.5, new TextBars(6)));
		properties.put( "E", new TextProperties(1.5, new TextBars(1,2)));
		properties.put( "F", new TextProperties(1.5, new TextBars(1,2,3)));
		properties.put( "G", new TextProperties(1.5, new TextBars(1,6)));
		properties.put( "H", new TextProperties(1.5, new TextBars(0,3)));
		properties.put( "I", new TextProperties(1.5, new TextBars(0,1,2,3,6)));
		properties.put( "J", new TextProperties(1.7, new TextBars(0,4,5,6)));
		properties.put( "K", new TextProperties(1.5, new TextBars2(6,7,9,11)));
		properties.put( "L", new TextProperties(1.6, new TextBars(0,1,2,6)));
		properties.put( "M", new TextProperties(1.8, new TextBars("blank"))); // FIXME
		properties.put( "N", new TextProperties(1.5, new TextBars("blank"))); // FIXME
		properties.put( "O", new TextProperties(1.5, new TextBars(6)));
		properties.put( "P", new TextProperties(1.5, new TextBars(2,3)));
		properties.put( "Q", new TextProperties(1.5, new TextBars("blank"))); // FIXME
		properties.put( "R", new TextProperties(1.5, new TextBars(3)));
		properties.put( "S", new TextProperties(1.5, new TextBars(1,4)));
		properties.put( "T", new TextProperties(1.5, new TextBars2(0,1,6,7))); // FIXME
		properties.put( "U", new TextProperties(1.5, new TextBars(0,6)));
		properties.put( "V", new TextProperties(1.5, new TextBars2(8,9))); // FIXME
		properties.put( "W", new TextProperties(1.5, new TextBars("blank"))); // FIXME
		properties.put( "X", new TextProperties(1.5, new TextBars2(8,9,10,11))); // FIXME
		properties.put( "Y", new TextProperties(1.5, new TextBars2(7,8,9))); // FIXME
		properties.put( "Z", new TextProperties(1.5, new TextBars2(0,1,4,5,9,10))); // FIXME

	}

	private void buildClassifier()   {

		doAdd( "1 1",  "1lirLIJT".toCharArray() );
		doAdd( "1:2 1",  "cftF".toCharArray() );
		doAdd("1:2 1:2:1:2:1", "C".toCharArray() );

		doAdd("1 1:2:1", "1hJ".toCharArray() );

		doAdd( "1 1:2",  "hn".toCharArray() );
		doAdd("1 1:2:1:2", "h".toCharArray() );

		doAdd( "1 2:1",  "ruvUVY".toCharArray() );
		doAdd( "1 2", "un".toCharArray() );
		doAdd( "1 2:1:2", "nuH".toCharArray() );

		doAdd("1:2 2:1:2", "kK".toCharArray() );

		doAdd("1 3", "m".toCharArray() );
		
		doAdd( "2:1 1:2:1",  "9D".toCharArray() );
		doAdd( "2:3:1 1:2:1:2:1",  "89a".toCharArray() );
		doAdd( "1:3:2:1 1:2:1", "9".toCharArray() );
		doAdd( "1:2:3:1 1:2:1:2:1", "9".toCharArray() );
		doAdd( "1:3:1 1:2:1:2:1", "9".toCharArray() );
		doAdd( "2:3:1 1:2:1:2:1:2:1", "9".toCharArray() );
		
		doAdd( "1:2:1 1:2:1",  "049bopDOP".toCharArray() );

		doAdd( "2:3:2 1:2", "5".toCharArray() );
		doAdd( "2:3 1:2:1:2:1", "5".toCharArray() );
		doAdd( "2:3:2 1:2:1", "258".toCharArray() );		
		doAdd( "2:3:2 1",  "5szSZ".toCharArray() );		
		doAdd( "2:3:1:2 1:2:1:2:1", "8".toCharArray() );		
		doAdd( "2:3:2 1:2:1:2:1", "358sS".toCharArray() );
		doAdd( "2:3 1:2:1", "5s".toCharArray() );
		doAdd( "3 1:2:1:2:1", "S".toCharArray() );

		doAdd( "2:3:1 1:2:1:2",  "a".toCharArray() );
		doAdd( "2:3:1 1:2:1",  "a".toCharArray() );
		doAdd( "2:3:1 1:2",  "a".toCharArray() );
		doAdd( "3:1 1:2",  "a".toCharArray() );
		doAdd( "2:3:1 1",  "a".toCharArray() );

		doAdd( "2:3 1",  "3".toCharArray() );
		doAdd( "1:2:1 1:2",  "bd".toCharArray() );
		doAdd( "1:2 1:2:1", "0CD".toCharArray() );
		doAdd( "1:2 1:2", "C".toCharArray() );

		doAdd( "1:3:2 2:1",  "e".toCharArray() );		
		doAdd( "1:3:2 1:2:1",  "e".toCharArray() );
		doAdd( "1:3:2 1:2:1:2:1",  "e".toCharArray() );
		doAdd( "1:3 1:2:1", "e".toCharArray() );

		doAdd( "1:3 1", "E".toCharArray() );
		doAdd( "1:3:2 1", "E".toCharArray() );
		doAdd( "1:3:1 1:2:1", "E".toCharArray() );
		doAdd( "1:3:1 1", "E".toCharArray() );
		doAdd( "3 1", "E".toCharArray() );
		
		
		doAdd( "1:2:1 1:2:1:2",  "A".toCharArray() );
		doAdd( "2:1 1:2:1:2",  "A".toCharArray() );
		doAdd( "1:2:1 1",  "7".toCharArray() );
		doAdd( "2:1 1",  "7".toCharArray() );

		doAdd( "1 2:4:3", "M".toCharArray() );
		doAdd( "1 2:3", "M".toCharArray() );
		doAdd( "1 3:4:2", "W".toCharArray() );

		doAdd( "1:2 1:2:1:2", "CR".toCharArray() );
		doAdd( "1 2:3:2", "wN".toCharArray() );
		doAdd( "1:2:1 2:3:2", "N".toCharArray() );
		doAdd( "1:2:1:2:1 2:3:2", "N".toCharArray() );
		
		doAdd( "1:3:2 2:1:2:1", "B".toCharArray() );
		
		doAdd( "1:2:3:2:1 1:2:1", "G".toCharArray() );
		
		doAdd( "1:2:3:2:3:2:1 1:2:3:2:3:2:1", "Q".toCharArray() );
		doAdd("2:1:2 2:1:2", "Xx".toCharArray() );

		doAdd("1:2:1 2:1", "U".toCharArray() );
	}

	private void doAdd(String topology, char[] vals)   {
		Logger logger = new Logger(m_logger, "doAdd");
		char[] curr = classifier.get(topology);
		if (curr == null) {
			classifier.put( topology, vals);
		}
		else {
			logger.error("duplicate key in classifier "+topology+" already exists");
		}
	}

	public List<TextChoice> getMatchingText( TextShape shape) {
		String topology = shape.getTopologyX()+" "+shape.getTopologyY();
		char[] topMatches = classifier.get(topology);
		List<TextChoice> choices = new ArrayList<TextChoice>();
		if (topMatches != null && topMatches.length > 0) {
			for ( int i=0 ; i<topMatches.length ; i++) {
				TextChoice choice = new TextChoice( topMatches[i]);
				String value = Character.toString(topMatches[i]);
				TextProperties props = properties.get(value);
//				System.out.println("Match segments for shape against "+value);
				double score = TextShape.matchSegments( props, shape.getBound(), shape.getTextSegments());
				choice.setScore(score);
				choices.add(choice);
			}
			TextChoiceComparator comparator = new TextChoiceComparator();
			Collections.sort(choices, comparator);

			double minNonZeroScore = 1.0;
			int numZeroScore = 0;
			for ( int i=0 ; i<choices.size() ; i++) {				
				if (choices.get(i).getScore() < 0.0001) {
					numZeroScore++;
				}
				else {
					minNonZeroScore = minNonZeroScore > choices.get(i).getScore() ? choices.get(i).getScore() : minNonZeroScore;
				}
//				System.out.println(choices.get(i));
			}
			if (numZeroScore > 0) {
//				System.out.println("setting zero score for "+numZeroScore+" entries using "+minNonZeroScore);
				double zeroScore = minNonZeroScore/numZeroScore;
				for ( int i=0 ; i<choices.size() ; i++) {
					TextChoice choice = choices.get(i);
					if (choice.getScore() < 0.0001) {
						choice.setScore(zeroScore);
					}
				}
			}
			return choices;
		}
		else {
			return null;
		}
	}
}

class TextChoiceComparator implements Comparator<TextChoice> {
	@Override
	public int compare(TextChoice c1, TextChoice c2) {
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
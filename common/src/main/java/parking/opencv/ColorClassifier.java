package parking.opencv;

import java.awt.Color;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class ColorClassifier {

	private static final float balanceThreshold = 0.9f;
	
	private static final Map<Color, String> basicColors = new HashMap<Color, String>();

	static {
		basicColors.put(Color.red, "red0");
		basicColors.put(Color.blue, "blue0");
		basicColors.put(Color.green, "green0");
		basicColors.put( new Color( 0x808000 ) , "green1");
		basicColors.put( new Color( 0x556B2F ) , "green2");
		basicColors.put(Color.black, "black0");
		basicColors.put(Color.white, "white0");
		basicColors.put( new Color( 0xF5F5F5 ) , "white1");
		basicColors.put( new Color( 0xFFFAFA ) , "white2");
		basicColors.put( new Color( 0xF5FFFA ) , "white3");
		basicColors.put(Color.lightGray, "gray0");
		basicColors.put(Color.darkGray, "gray1");		
		basicColors.put( new Color( 0xC0C0C0 ) , "gray2");
		basicColors.put( new Color( 0x696969 ) , "gray2");
		basicColors.put( new Color( 0xCD853F ) , "brown0");	
		basicColors.put( new Color( 0x8B4513 ) , "brown1");	
		basicColors.put( new Color( 0xA0522D ) , "brown2");
		basicColors.put( new Color( 0xD2B48C ) , "brown3");
		basicColors.put( new Color( 0xB8860B ) , "brown4");
	}

	public static String getColorName(Color color) {
		return basicColors.get(color);
	}

	public static Map<Color, Color> classifyWindowColors(ColorWindow win) {		
		List<Color> windowColors = win.getColors();
		Map<Color, Color> colorMap = new HashMap<Color, Color>();
		for (Color color : windowColors) {
			Color standard = classifyColor(color);
			colorMap.put( color, standard);
		}
		if (colorMap.size() == 0) {
			return null;
		}
		return colorMap;
	}

	public static Color classifyColor(Color color) {
		List<Color> colors = new ArrayList<Color>(basicColors.keySet());
		Color bestMatch = colors.get(0);
		double minDist = getDist(color, bestMatch);
		for ( int i=1 ; i<basicColors.size() ; i++) {
			double dist = getDist(color, colors.get(i) );
			if (dist < minDist) {
				bestMatch = colors.get(i);
				minDist = dist;
			}
		}
		return bestMatch;
	}

	public static double getDist(Color c1, Color c2) {
		float[] comp1 = c1.getRGBColorComponents(null);
		float[] comp2 = c2.getRGBColorComponents(null);
		float sum2 = 0;
		for ( int i=0 ; i<3 ; i++) {
			float d = comp1[i] - comp2[i];
			sum2 += d*d;
		}
		return Math.sqrt((double)sum2);
	}

	public static boolean isWhite(Color color, double whiteThreshold) {
		return isBalanced(color) && getGreyLevel(color) > whiteThreshold;
	}

	public static boolean isBlack(Color color, double blackThreshold) {
		return isBalanced(color) && getGreyLevel(color) < blackThreshold;
	}

	//
	//  return true if ratio of ( min/max ) is above a threshold, (ratio of 1 means perfect balance)
	// 
	public static boolean isBalanced(Color c) {
		float[] comp = c.getRGBColorComponents(null);
		float max =0;
		float min = -1;
		for ( int j=0 ; j<3 ; j++ ) {
			max = max < comp[j] ? max = comp[j] : max;
			if ( min <0 || min < comp[j]) {
				min = comp[j];
			}
		}
		if (max == 0.0f) {
			return true;
		}

		return min/max > balanceThreshold;
	}

	public static double getBias(Color color, int index) {
		float[] comp = color.getRGBColorComponents(null);
		int max = 0;
		int min = 0;
		for ( int j=0 ; j<3 ; j++ ) {
			min = comp[min] > comp[j] ? j : min;
			max = comp[max] < comp[j] ? j : max;
		}
		if ( max != index || comp[max] == 0.0f) {
			return 0.0;
		}
		int middle = 0;
		for ( int j=0 ; j>3 ; j++ ) {
			if (max != j && min != j) {
				middle = j;
			}
		}

		return comp[max] / comp[middle];
	}

	public static double getGreyLevel(Color color) {
		return Math.sqrt(color.getRed()*color.getRed() + color.getBlue()*color.getBlue() + color.getGreen()*color.getGreen())/(255 * Math.sqrt(3));
	}
}
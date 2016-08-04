package parking.opencv;

import org.opencv.core.Point;

import java.util.List;
import java.util.ArrayList;
//
//  Simple representation of a text character using
//  7 bars ( 4 vertical and 3 horizontal )
//   
public class TextBars {

	// 3 horizontal
	private boolean top;
	private boolean middle;
	private boolean bottom;

	// 4 vertical
	private boolean upperLeft;
	private boolean lowerLeft;
	private boolean upperRight;
	private boolean lowerRight;
	
	public TextBars() {
		setBars( allOn() );
	}

	public TextBars( String value) {
		if (value.equals("blank")) {
			setBars( allOff() );
		}
	}

	public TextBars( boolean[] bars) {
		setBars( bars );
	}

	public TextBars( int index) {
		boolean[] bars = allOn();
		bars[index] = false;
		setBars( bars );
	}

	public TextBars( int i1, int i2) {
		boolean[] bars = allOn();
		bars[i1] = false;
		bars[i2] = false;
		setBars( bars );
	}

	public TextBars( int i1, int i2, int i3) {
		boolean[] bars = allOn();
		bars[i1] = false;
		bars[i2] = false;
		bars[i3] = false;
		setBars( bars );
	}

	public TextBars( int i1, int i2, int i3, int i4) {
		boolean[] bars = allOn();
		bars[i1] = false;
		bars[i2] = false;
		bars[i3] = false;
		bars[i4] = false;
		setBars( bars );
	}

	public TextBars( int i1, int i2, int i3, int i4, int i5) {
		boolean[] bars = allOn();
		bars[i1] = false;
		bars[i2] = false;
		bars[i3] = false;
		bars[i4] = false;
		bars[i5] = false;
		setBars( bars );
	}

	private void setBars( boolean[] bars) {
		top = bars[0];
		upperRight = bars[1];
		lowerRight = bars[2];
		bottom = bars[3];
		lowerLeft = bars[4];
		upperLeft = bars[5];
		middle = bars[6];
	}

	private boolean[] allOn() {
		boolean[] all = new boolean[7];
		for ( int i=0 ; i<7 ; i++ ) {
			all[i] = true;
		}
		return all;
	}

	private boolean[] allOff() {
		boolean[] all = new boolean[7];
		for ( int i=0 ; i<7 ; i++ ) {
			all[i] = false;
		}
		return all;
	}

	public List<Line> getStrokes(Rectangle bounds) {
		List<Line> strokes = new ArrayList<Line>();		
		Line[] side = bounds.getSides();
		Line leftSide = side[0];
		Line bottomSide = side[1];
		Line rightSide = side[2];
		Line topSide = side[3];
		Point leftMid = leftSide.findPoint(0.5);
		Point rightMid = rightSide.findPoint(0.5);
		if (top) {
			strokes.add(topSide);
		}
		if (middle) {
			Line mid = new Line(  leftMid, rightMid);
			strokes.add(mid);
		}
		if (bottom) {
			strokes.add(bottomSide);
		}		
		if (upperLeft) {			
			strokes.add( new Line( leftSide.p1, leftMid));
		}
		if (lowerLeft) {
			strokes.add( new Line( leftMid, leftSide.p2) );
		}
		if (upperRight) {
			strokes.add( new Line( rightSide.p1, rightMid) );
		}
		if (lowerRight) {
			strokes.add( new Line( rightMid, rightSide.p2) );
		}
		return strokes;
	}
}
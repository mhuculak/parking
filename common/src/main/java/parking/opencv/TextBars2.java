package parking.opencv;

import org.opencv.core.Point;

import java.util.List;
import java.util.ArrayList;
//
//  More complext representation of a text character than TextBar
//  using 12 bars ( 2 vertical and 6 horizontal and 4 diagonal )
//   
public class TextBars2 {

	// 6 horizontal
	private boolean topLeft;
	private boolean topRight;
	private boolean middleLeft;
	private boolean middleRight;
	private boolean bottomLeft;
	private boolean bottomRight;

	// 2 vertical
	private boolean upperMiddle;
	private boolean lowerMiddle;
	
	// 4 diagonal
	private boolean upperLeft;
	private boolean upperRight;
	private boolean lowerLeft;
	private boolean lowerRight;

	public TextBars2 () {
		setBars( allOff() );
	}

	public TextBars2 ( int i1 ) {
		boolean[] bars = allOff();
		bars[i1] = true;
		setBars( bars );
	}

	public TextBars2 ( int i1, int i2) {
		boolean[] bars = allOff();
		bars[i1] = true;
		bars[i2] = true;
		setBars( bars );
	}

	public TextBars2 ( int i1, int i2, int i3) {
		boolean[] bars = allOff();
		bars[i1] = true;
		bars[i2] = true;
		bars[i3] = true;
		setBars( bars );
	}

	public TextBars2 ( int i1, int i2, int i3, int i4) {
		boolean[] bars = allOff();
		bars[i1] = true;
		bars[i2] = true;
		bars[i3] = true;
		bars[i4] = true;
		setBars( bars );
	}

	public TextBars2 ( int i1, int i2, int i3, int i4, int i5) {
		boolean[] bars = allOff();
		bars[i1] = true;
		bars[i2] = true;
		bars[i3] = true;
		bars[i4] = true;
		bars[i5] = true;
		setBars( bars );
	}

	public TextBars2 ( int i1, int i2, int i3, int i4, int i5, int i6) {
		boolean[] bars = allOff();
		bars[i1] = true;
		bars[i2] = true;
		bars[i3] = true;
		bars[i4] = true;
		bars[i5] = true;
		bars[i6] = true;
		setBars( bars );
	}

	private void setBars( boolean[] bars) {
		topLeft = bars[0];
		topRight = bars[1];
		middleLeft = bars[2];
		middleRight = bars[3];
		bottomLeft = bars[4];
		bottomRight = bars[5];
		upperMiddle = bars[6];
		lowerMiddle = bars[7];
		upperLeft = bars[8];
		upperRight = bars[9];
		lowerLeft = bars[10];
		lowerRight = bars[11];
	}

	private boolean[] allOn() {
		boolean[] all = new boolean[12];
		for ( int i=0 ; i<7 ; i++ ) {
			all[i] = true;
		}
		return all;
	}

	private boolean[] allOff() {
		boolean[] all = new boolean[12];
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
		Point topMid = topSide.findPoint(0.5);
		Point bottomMid = bottomSide.findPoint(0.5);
		Line middleLine = new Line( leftMid, rightMid);
		Point center = middleLine.findPoint(0.5);
		if (topLeft) {
			strokes.add( new Line( topSide.p2, topMid) );
		}
		if (topRight) {
			strokes.add( new Line( topMid, topSide.p1) );
		}
		if (middleLeft) {
			strokes.add( new Line ( middleLine.p1, center) );
		}
		if (middleRight) {
			strokes.add( new Line ( center, middleLine.p2) );
		}
		if (bottomLeft) {
			strokes.add( new Line ( bottomSide.p1, bottomMid) );
		}
		if (bottomRight) {
			strokes.add( new Line ( bottomMid, bottomSide.p2) );
		}
		if (upperMiddle) {
			strokes.add( new Line ( topMid, center) );
		}
		if (lowerMiddle) {
			strokes.add( new Line ( center, bottomMid) );
		}
		if (upperLeft) {
			strokes.add( new Line( topSide.p2, center));
		}
		if (upperRight) {
			strokes.add( new Line( topSide.p1, center));
		}
		if (lowerLeft) {
			strokes.add( new Line ( bottomSide.p1, center));
		}
		if (lowerRight) {
			strokes.add( new Line( bottomSide.p2, center));
		}
		return strokes;

	}
}


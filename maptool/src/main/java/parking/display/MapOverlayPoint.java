package parking.display;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Dimension;

public class MapOverlayPoint extends DisplayPosition implements MapOverlay {
	
	private int ix;
	private int iy;
	private Color color;
	private boolean selected;

	private final int selectRadius = 10;

	public MapOverlayPoint(double x, double y, Color color) {
		super(x, y);
		this.color = color;
		selected = false;
		roundForDisplay();
	}

	public MapOverlayPoint(DisplayPosition p, Color color) {
		super(p.x, p.y);
		this.color = color;
		selected = false;
		roundForDisplay();
	}

	private void roundForDisplay() {
		ix = (int)(super.x + 0.5);
		iy = (int)(super.y + 0.5);
	}

	public boolean isCloseTo(DisplayPosition p, double r) {
//		System.out.println("Distance from "+super.x+" "+super.y+" to selected "+p+" is "+DisplayPosition.distance(this, p)+" radius is "+r);
		return DisplayPosition.distance(this, p) < r;
	}

	public void pan(DisplayPosition delta) {
		super.update(DisplayPosition.delta( this, delta));
		roundForDisplay();
	}

	public void movePoint(DisplayPosition target) {
		super.update(target);
		roundForDisplay();
	}

	public void zoom(Dimension size, double scale) {
		double x = size.width/2.0 + scale * ( super.x - size.width/2.0);
		double y = size.height/2.0 + scale * ( super.y - size.height/2.0);
		super.update(new DisplayPosition( x, y));
		roundForDisplay();
	}

	public void select(boolean selected) {
		this.selected = selected;
	}

	public void draw(Graphics2D g2) {
		g2.setColor(color);
		g2.drawLine(ix-3, iy, ix+3, iy);
		g2.drawLine(ix, iy-3, ix, iy+3);
		if (selected) {
			g2.drawOval(ix-selectRadius, iy-selectRadius, 2*selectRadius, 2*selectRadius);
		}
	}
	
}
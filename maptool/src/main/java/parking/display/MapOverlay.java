package parking.display;

import java.awt.Graphics2D;
import java.awt.Dimension;

public interface MapOverlay {

	public void draw(Graphics2D g2);

	public boolean isCloseTo(DisplayPosition dp, double r);

	public void pan(DisplayPosition delta);

	public void movePoint(DisplayPosition target);

	public void select(boolean selected);

	public void zoom(Dimension size, double scale);
	
}
package parking.opencv;

import org.opencv.core.Point;

public class Transformation {
	
	private Vector translation;
	private double rotation;
	private Point center;

	public Transformation(Vector translation, double rotation, Point center) {
		this.translation = translation;
		this.rotation = rotation;
		this.center = Vector.add( center, translation); // center of rotation
	}

	public Rectangle apply(Rectangle rect) {
		Rectangle tRect = Rectangle.translate(rect, translation);
        Rectangle nRect = Rectangle.rotate(tRect, rotation);
        nRect.setTransform(this);
        return nRect;
	}

	public Ellipse apply(Ellipse ellipse) {
		Ellipse tEllipse = Ellipse.translate( ellipse, translation);
        Ellipse nEllipse = Ellipse.rotate( tEllipse, rotation);
        nEllipse.setTransform(this);
        return nEllipse;
    }

    public Line reverse(Line l) {
    	Line rLine = Line.rotate(l, center, -rotation);
    	return Line.translate(rLine, new Vector(-translation.x, -translation.y));
    }
}
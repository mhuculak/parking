package parking.display;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

public class ThumbNail {
	private BufferedImage image;
	private String caption;
	private final double desiredMaxDim = 150;

	public ThumbNail( BufferedImage orig, String caption) {
		this.caption = caption;
		double max = Math.max( orig.getWidth(), orig.getWidth());
		double scale = desiredMaxDim / max;
		int width = (int)(orig.getWidth() * scale + 0.5);
		int height = (int)(orig.getHeight() * scale + 0.5);
		this.image = reduceTo( orig, width, height);
	}

	public ThumbNail( BufferedImage orig, String caption, int width) {
		this.caption = caption;
		double scale = width;
		scale = scale / orig.getWidth();
		int height = (int)(orig.getHeight() * scale + 0.5);
		this.image = reduceTo( orig, width, height);
	}

	public ThumbNail( BufferedImage orig, String caption, int width, int height) {
		this.caption = caption;
		this.image = reduceTo( orig, width, height);
	}

	public BufferedImage reduceTo( BufferedImage orig, int width, int height) {		
		BufferedImage image = new BufferedImage( width, height, getType(orig));
		Graphics2D g = image.createGraphics();
		g.drawImage(orig, 0, 0, width, height, null);
		g.dispose();
		if (width > height) {
			return SignImage.rotateImage90(image);
		}
		else {
			return image;
		}		
	}

	public BufferedImage getImage() {
		return image;
	}

	public String getCaption() {
		return caption;
	}

	public int getType(BufferedImage img) {
		int type = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType();
		return type;
	}
}
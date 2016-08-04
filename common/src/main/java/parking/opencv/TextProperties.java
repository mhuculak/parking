package parking.opencv;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class TextProperties {
	private List<String> topologies;
	private double aspectRatio;
	private List<TextBars> textBars;
	private List<TextBars2> textBars2;

	public TextProperties(double aspectRatio, TextBars bars) {
		this.aspectRatio = aspectRatio;
		addTextBars(bars);
	}

	public TextProperties(double aspectRatio, TextBars2 bars2) {
		this.aspectRatio = aspectRatio;
		addTextBars2(bars2);
	}

	public TextProperties(double aspectRatio, TextBars bars1, TextBars bars2) {
		this.aspectRatio = aspectRatio;
		addTextBars(bars1);
		addTextBars(bars2);
	}

	public TextProperties(double aspectRatio, TextBars bars1, TextBars2 bars2) {
		this.aspectRatio = aspectRatio;
		addTextBars(bars1);
		addTextBars2(bars2);
	}

	private void addTextBars(TextBars bars) {
		if (textBars == null) {
			textBars = new ArrayList<TextBars>();
		}
		textBars.add(bars);
	}

	private void addTextBars2(TextBars2 bars2) {
		if (textBars2 == null) {
			textBars2 = new ArrayList<TextBars2>();
		}
		textBars2.add(bars2);
	}

	public void addTopology(String topology) {
		if (topologies == null) {
			topologies = new ArrayList<String>();
		}
		topologies.add(topology);
	}

	public double getAspectRatio() {
		return aspectRatio;
	}

	public List<List<Line>> getStrokes(Rectangle bounds) {
		List<List<Line>> strokeList = new ArrayList<List<Line>>();
		if (textBars != null) {
			for (TextBars b : textBars) {
				strokeList.add(b.getStrokes(bounds));
			}
		}
		if (textBars2 != null) {
			for (TextBars2 b : textBars2) {
				strokeList.add(b.getStrokes(bounds));
			}
		}
		return strokeList;
	}
}
package parking.display;

import parking.map.StreetSegment;

import java.util.List;
import java.util.ArrayList;

public class StreetSegmentOverlay extends MapOverlaySegment {

	private StreetSegment mapSegment;

	public StreetSegmentOverlay(String name) {
		super(true);
		mapSegment = new StreetSegment(name);
	}

	public StreetSegmentOverlay(StreetSegmentOverlay seg) {
		super(true);
		mapSegment = new StreetSegment(seg.getMapSegment());
	}

	public StreetSegment getMapSegment() {
		return mapSegment;
	}

	public List<MapOverlay> getSelectable() {
		List<MapOverlay> selectable = new ArrayList<MapOverlay>();
		selectable.addAll(super.getPoints());
		return selectable;
	}
	
}
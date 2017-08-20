package parking.display;

import parking.map.Sign;
import parking.map.Position;
import parking.map.MapBounds;
import parking.util.Utils;


import javax.swing.JComponent;

import javax.imageio.ImageIO;
import javax.imageio.IIOException;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.Polygon;
import java.awt.Dimension;

import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
//
//  TODO:
//
//    1. cells at the same level should not overlap
//
//
//
//

public class DisplayMap extends JComponent {
	private MapTool parent;
	private BufferedImage mapImage;
	private List<MapOverlay> overlays = new ArrayList<MapOverlay>();
	private Graphics2D graphics;
	private Position center;
	private int zoomLevel;
	private Dimension size;
	private Map<Integer, Integer> prevLatIndex = new HashMap<Integer, Integer>();
	private List<MapCell> visibleCells;
	private List<MapCell> available;
	private static Position p0;
	private static double R0;
	public static double latitudeFactor;
	private static final int baseDim = 512;
	private static final double maxLatitude = 70.0;

	private static final Map<Integer,Map<String,MapCell>> mapCells;
	private static final Map<Integer,Map<Integer, Double>> cellLatitude;
	private static final Map<Integer, Integer> initlatIndex;

	private static final int minZoomLevel = 13;
	private static final int maxZoomLevel = 20;
	private static final double nominalLatitude = 45.0;

	static {
		p0 = new Position(0,0);
		R0 = Position.getEarthRadiusMeters(p0);		
		mapCells = new HashMap<Integer,Map<String,MapCell>>();
		cellLatitude = new HashMap<Integer,Map<Integer, Double>>();
		initlatIndex = new HashMap<Integer, Integer>();
		for (int z=minZoomLevel ; z<=maxZoomLevel ; z++) {
			Map<String,MapCell> cells = new HashMap<String,MapCell>();
			mapCells.put(z, cells);
			Map<Integer, Double> cellLat = new HashMap<Integer, Double>();
			cellLatitude.put(z, cellLat);
			int indx = 0;
			double lat = 0;
			while (lat < maxLatitude) {
				cellLat.put(indx, lat);				
				double prevLat = lat;
				lat += getCellSize(new Position(lat,0), z).lat;
				if (lat >= nominalLatitude && prevLat < nominalLatitude) {
					initlatIndex.put(z, indx);
				}
				indx++;
			}
		 }
	}

	public DisplayMap(MapTool parent, Position center, int zoomLevel, Dimension size) {
		this.parent = parent;
		this.center = center;
		this.zoomLevel = zoomLevel;		
		this.size = size;
		latitudeFactor = getResolution(center, zoomLevel) / getResolution(p0, zoomLevel);	
		for ( Integer level : initlatIndex.keySet() ) {
			prevLatIndex.put(level, initlatIndex.get(level));
		}
		if (!createMapImage()) {
			System.out.println("Waiting for download of map...");
		}				
	}

	public void pan(Position newCenter) {
		Position change = Position.delta( newCenter, center );
		DisplayPosition delta = latLngToDisplayDelta(change);
//		System.out.println("Pan by "+delta+" "+change);
		for (MapOverlay ovrly : overlays) {
			ovrly.pan(delta);
		}
		center = newCenter;
		createMapImage();		
	}

	public void zoom(int newZoomLevel) {
		if (newZoomLevel <= maxZoomLevel && newZoomLevel >= minZoomLevel) {
			int prevZoomLevel = zoomLevel;
			double oldRes = getResolution(center, prevZoomLevel);
		 	double newRes = getResolution(center, newZoomLevel);
		 	double scale =  oldRes / newRes;
			for (MapOverlay ovrly : overlays) {
				ovrly.zoom(size, scale);
			}
			zoomLevel = newZoomLevel;
			createMapImage();
		}
	}

	public Position getMapPosition(DisplayPosition dp) {
		double latRes = getResolution(center, zoomLevel);
		double dy = dp.y - size.height/2.0;
		double lat = center.getLatitude() - pixelsToDegrees((int)(dy +0.5), R0, latRes);
		double lngRes = getResolution(center, zoomLevel);
		double dx = dp.x - size.width/2.0;
		double lng = center.getLongitude() + pixelsToDegrees((int)( dx/latitudeFactor + 0.5), R0, latRes);
		return new Position(lat, lng);
	}

	public void addOverlay(MapOverlay overlay) {
		overlays.add(overlay);
	}

	public void removeOverlay(MapOverlay overlay) {
		overlays.remove(overlay);
	}

	public double getLatitudeSize() {
		double latRes = getResolution(center, zoomLevel);
		return pixelsToDegrees(size.height, R0, latRes);
	}

	public double getLongitudeSize() {
		double longRes = getResolution(center, zoomLevel);
		return pixelsToDegrees(size.width, R0, longRes);
	}

	public DisplayPosition latLngToDisplayPosition(Position p) {
		
		Position delta = Position.delta(p, center);
		DisplayPosition dp = latLngToDisplayDelta(delta);
		return new DisplayPosition(size.width/2.0 + dp.x, size.height/2.0  + dp.y);
	}

	private DisplayPosition latLngToDisplayDelta(Position delta) {
		double latRes = getResolution(center, zoomLevel);
		double lngRes = getResolution(center, zoomLevel);
	//	double lngRes = getResolution(p0, zoomLevel);
		double dx = degreesToDisplay( delta.getLongitude(), R0, lngRes);
		double dy = degreesToDisplay( delta.getLatitude(), R0, latRes);
	//	return new DisplayPosition(dx, dy);
		return new DisplayPosition(latitudeFactor * dx, -dy);
	}

	private static CellSize getCellSize(Position p, int level) {
		double lngRes = getResolution(p0, level);
		double latRes = getResolution(p, level);		
		return new CellSize(pixelsToDegrees(baseDim, R0, latRes), pixelsToDegrees(baseDim, R0, lngRes));
	}

	private static CellSize getCellSize(int latIndex, int level) {
		Map<Integer, Double> cellLat = cellLatitude.get(level);
		double lngRes = getResolution(p0, level);
		return new CellSize( cellLat.get(latIndex+1) - cellLat.get(latIndex), pixelsToDegrees(baseDim, R0, lngRes));
	}
	
	private int getLatIndex(Position p, int latIndex, int level) {
		Map<Integer, Double> cellLat = cellLatitude.get(level);
		if (cellLat.get(latIndex) > p.getLatitude()) {
			while (cellLat.get(latIndex) > p.getLatitude()) {
				latIndex--;
			}
			return latIndex;

		}
		else {
			while (cellLat.get(latIndex) < p.getLatitude()) {
				latIndex++;
			}
			return latIndex;
		}
	}

	private MapBounds getMapBounds() {
		int latIndex = prevLatIndex.get(zoomLevel);
		CellSize cellSizeDegrees = getCellSize( latIndex, zoomLevel);
		Position ne = new Position( center.getLatitude() + cellSizeDegrees.lat/2.0, 
									center.getLongitude() + cellSizeDegrees.lng/2.0);
		Position sw = new Position( center.getLatitude() - cellSizeDegrees.lat/2.0, 
									center.getLongitude() - cellSizeDegrees.lng/2.0);
		return new MapBounds(ne, sw);
	}

	private boolean createMapImage() {
//		System.out.println("Create map with zoom level "+zoomLevel+" center "+center);
		final long downloadTimeoutMs = 10000;
		mapImage = null;
		graphics = null;
		visibleCells = checkVisibility();		
		int downloadCount = 0;
		available = new ArrayList<MapCell>();
		for (MapCell cell: visibleCells) {
			cell.getMap(this);
			if (cell.downloadInProgress) {
				downloadCount++;
			}
			else {
				available.add(cell);
			}
		}

		parent.mapBoundsChange(getMapBounds(), this);

		if (downloadCount == 0) {			
			drawMap(available);
			return true;
		}	
		return false;
	}

	private List<MapCell> checkVisibility() {	
		List<MapCell> visible = new ArrayList<MapCell>();
		MapCell primeCell = getCell(center, zoomLevel);
		visible.add(primeCell);
		primeCell.addDisplayPos(center, size, p0, R0);
		String[] indx = primeCell.getKey().split("_");
		int primeLatIndex = Integer.parseInt(indx[0]);
		int primeLngIndex = Integer.parseInt(indx[1]);
//		System.out.println("prime cell "+primeCell);
//		System.out.println("is at latIndex "+primeLatIndex+" longIndex "+primeLngIndex+" pos "+primeCell.getDisplayPos());
		int latIndex = primeLatIndex;
		CellSize cellSize = getCellSize( latIndex, zoomLevel);
		CellPosition dispPos = null;
		do {
			latIndex++;
			dispPos = processCell(visible, latIndex, primeLngIndex, cellSize);
		} while( isVisible(dispPos, size));
		int maxLatIndex = latIndex;
		latIndex =  primeLatIndex;
		do {
			latIndex--;
			dispPos = processCell(visible, latIndex, primeLngIndex, cellSize);
		} while( isVisible(dispPos, size));
		int minLatIndex = latIndex;
		int lngIndex = primeLngIndex;
		do {
			lngIndex++;
			dispPos = processCell(visible, primeLatIndex, lngIndex, cellSize);
		} while( isVisible(dispPos, size));
		int maxLngIndex = lngIndex;
		lngIndex = primeLngIndex;
		do {
			lngIndex--;
			dispPos = processCell(visible, primeLatIndex, lngIndex, cellSize);
		} while( isVisible(dispPos, size));
		int minLngIndex = lngIndex;
		visible = new ArrayList<MapCell>();
//		System.out.println("Visibility matrix:");
		for ( int i=minLngIndex ; i<= maxLngIndex ; i++) {
			for ( int j=minLatIndex ; j<= maxLatIndex ; j++ ) {
				processCell(visible, j, i, cellSize);
			}
		}
		return visible;
	}

	private CellPosition processCell(List<MapCell> visible, int latIndex, int lngIndex, CellSize cellSize) {
		String key = latIndex + "_" + lngIndex;
		Map<String,MapCell> cells = mapCells.get(zoomLevel);
		MapCell cell = cells.get(key);
		if (cell == null) {
			int latDim = (int)(90 / cellSize.lat) + 1;
			cell = createCell(latIndex, lngIndex, latDim, zoomLevel, cellSize);
			cell.setKey(key);
			cells.put(key,cell);
		}
		cell.addDisplayPos(center, size, p0, R0);
		if (isVisible(cell.getDisplayPos(), size)) {
//			System.out.println("cell at latIndex "+latIndex+" longIndex "+lngIndex+" pos "+cell.getDisplayPos()+" is visible");
			visible.add(cell);
		}
		else {
//			System.out.println("cell at latIndex "+latIndex+" longIndex "+lngIndex+" pos "+cell.getDisplayPos()+" is not visible");
		}
		return cell.getDisplayPos();
	}

	private boolean isVisible(CellPosition pos, Dimension size) {
		if (pos.x > 0 && pos.x <size.width && pos.y > 0 && pos.y < size.height) {
			return true;
		}
		else if ((pos.x + baseDim) > 0 && (pos.x + baseDim) < size.width && pos.y > 0 && pos.y < size.height) {
			return true;
		}
		else if (pos.x > 0 && pos.x < size.width && (pos.y + baseDim) > 0 && (pos.y + baseDim) < size.height) {
			return true;
		}
		else if ((pos.x + baseDim) > 0 && (pos.x + baseDim) < size.width && (pos.y + baseDim) > 0 && (pos.y + baseDim) < size.height) {
			return true;
		}
		else {
			return false;
		}
	}

	private MapCell createCell(int latIndex, int longIndex, int latDim, int zoom, CellSize cellSizeDegrees) {
//		System.out.println("create cell at lat indx "+latIndex+" long index "+longIndex+" lat "+(latIndex*cellSizeDegrees.lat)+" lng "+(longIndex*cellSizeDegrees.lat));
		double latOffset = cellSizeDegrees.lat/2.0;
		double lngOffset = cellSizeDegrees.lng/2.0;
		double lat = 0;
		if ( latIndex < latDim ) {
			Map<Integer, Double> cellLat = cellLatitude.get(zoom);
			lat = cellLat.get(latIndex);
		}
		else {
			System.out.println("ERROR: southern hemisphere not currently supported!");
			System.exit(0);
		}
		lat += latOffset;
		double lng = longIndex*cellSizeDegrees.lng + lngOffset;	
		double latRes = getResolution(center, zoomLevel);
		double lngRes = getResolution(center, zoomLevel);	
		int lngSize = degreesToPixels( latitudeFactor*cellSizeDegrees.lng, R0, lngRes);
		int latSize = degreesToPixels( cellSizeDegrees.lat, R0, latRes);
//		System.out.println("new cell size is "+lngSize+" x "+latSize);
		return new MapCell(new Position(lat,lng), zoom, new Dimension(lngSize, latSize)); // cells get skinnier as we move north
	}

	private MapCell getCell(Position p, int zoom) {		
		int latIndex = prevLatIndex.get(zoom);
		latIndex = getLatIndex(p, latIndex, zoom);
		prevLatIndex.put(zoom, latIndex);
		CellSize cellSizeDegrees = getCellSize( latIndex, zoom);
		int latDim = cellLatitude.get(zoom).size();
		if (p.getLatitude() < 0) {			
			latIndex = latDim-latIndex;
		}
		double lng = p.getLongitude() < 0 ? 360 + p.getLongitude() : p.getLongitude();
//		System.out.println("    Got latIndex = "+latIndex+" for "+p.getLatitude()+" cell size = "+cellSizeDegrees.lat);
		int longIndex = (int)(lng / cellSizeDegrees.lng);
//		System.out.println("    Got longIndex = "+longIndex+" for lng = "+lng+" "+p.getLongitude()+" cell size = "+cellSizeDegrees.lng);
		double cellLong = longIndex*cellSizeDegrees.lng;
		double nextCellLong = (longIndex+1)*cellSizeDegrees.lng;
//		System.out.println("Got long index "+longIndex+" cell long "+cellLong+" next lng "+nextCellLong);
		String key = latIndex + "_" + longIndex;
//		System.out.println("    Got key "+key+" for cell at "+p+" cell size "+cellSizeDegrees.lat+" x "+cellSizeDegrees.lng);
		Map<String,MapCell> cells = mapCells.get(zoom);
		MapCell cell = cells.get(key);
		if (cell == null) {
			cell = createCell(latIndex, longIndex, latDim, zoom, cellSizeDegrees);
			cell.setKey(key);
			cells.put(key,cell);
		}
		return cell;
	}

	public Position getCenter() {
		return center;
	}

	public int getZoomLevel() {
		return zoomLevel;
	}

	public int getWidth() {
		if (mapImage != null) {
			return mapImage.getWidth();
		}
		return 0;
	}

	public int getHeight() {
		if (mapImage != null) {
			return mapImage.getHeight();
		}
		return 0;
	}

	//
	//  return map resolution in meters per pixel
	//
	public static double getResolution(Position p, int level) { 
		return 156543.03392 * Math.cos(p.getLatitude() * Math.PI / 180) / Math.pow(2, level);
	}

	private double convertLong(double longitude) {
		if (longitude < 0) {
			longitude = 360 + longitude;
		}
		return longitude;
	}

	private static double pixelsToDegrees(int lenPixels, double radiusOfEarthMeters, double metersPerPixel) {
		double lenMeters = lenPixels * metersPerPixel;	
		double angleRadians = lenMeters / radiusOfEarthMeters;				
		return angleRadians * 180 / Math.PI;
	}
	//
	//   rounding to int introduced error e.g overlay positions drift when panning back and forth many times
	//   should only be used when final target is int 
	//
	public static int degreesToPixels(double angleDeg, double radiusOfEarthMeters, double metersPerPixel) {
		double angleRad = angleDeg * Math.PI / 180;
		double lenMeters = angleRad * radiusOfEarthMeters;
		return Utils.round(lenMeters / metersPerPixel);
	}

	public static double degreesToDisplay(double angleDeg, double radiusOfEarthMeters, double metersPerPixel) {
		double angleRad = angleDeg * Math.PI / 180;
		double lenMeters = angleRad * radiusOfEarthMeters;
		return lenMeters / metersPerPixel;
	}

	public void addMap(MapCell cell) {
		available.add(cell);
//		System.out.println("Available "+available.size()+" visible "+visibleCells.size());
		drawMap(available);
	}

	public void drawMap(List<MapCell> visible) {
//		System.out.println("Drawing "+visible.size()+" maps om the display");
		mapImage = new BufferedImage ( size.width, size.height, BufferedImage.TYPE_INT_ARGB );
		graphics = mapImage.createGraphics();
		for (MapCell cell : available) {
			CellPosition cellPos =  cell.getDisplayPos();
//			System.out.println("Drawing map of size "+cell.getMap(this).getWidth()+" x "+cell.getMap(this).getHeight()+"at pos "+cellPos+" of canvas sized "+size.width+" x "+size.height);
			graphics.drawImage(cell.getMap(this), cellPos.x, cellPos.y, null);
		}
		graphics.dispose();
		parent.revalidate();
		parent.repaint();
	}

	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g.drawImage(mapImage, 0, 0, null);
//		System.out.println("Drawing "+overlays.size()+" overlays");
		for (MapOverlay ovrly : overlays) {
			ovrly.draw(g2);
		}
		g.setColor(Color.BLACK);
        for (MapCell cell : available) {
        	g.drawLine(0, cell.getDisplayPos().y, size.width, cell.getDisplayPos().y);
        	g.drawLine(cell.getDisplayPos().x, 0, cell.getDisplayPos().x, size.height);
        }
    }
}

class CellSize {
	public double lat;
	public double lng;

	public CellSize(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}

	public String toString() {
		return lat+" "+lng;
	}
}

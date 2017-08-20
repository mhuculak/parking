package parking.display;

import parking.map.Position;

import parking.util.HttpDownloader;

import javax.swing.SwingWorker;

import java.awt.image.BufferedImage;
import java.awt.Dimension;

import javax.imageio.ImageIO;
import javax.imageio.IIOException;

import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;


public class MapCell {
	private Position center;
	private int zoomLevel;
	private Dimension size;
	private String url;
	private String imageFile;
	private BufferedImage image;
	public boolean downloadInProgress;
	private DisplayMap parent;
	private CellPosition displayPos;    // where map is displayed on canvas
	private String key; 

	private final static String imageType = "png";
	private final static String mapDirName = "maps";
	private final static String dirPrefix = "level";

	public MapCell(Position c, int zoom, Dimension s) {
		center = c;
		zoomLevel = zoom;
		size = s;
		url = constructMapUrl(c, zoom, s);
		imageFile = constructFileName(c, zoom, s);
	}

	public CellPosition getDisplayPos() {
		return displayPos;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void addDisplayPos(Position displayCenter, Dimension displaySize, Position p0, double R0) {
//		System.out.println("Display center is "+displayCenter+" cell center is "+center);
		double latRes = DisplayMap.getResolution(center, zoomLevel);
		double lngRes = DisplayMap.getResolution(center, zoomLevel);
		Position delta = Position.delta(center, displayCenter);
//		System.out.println("delta = "+delta);
		double dx = DisplayMap.degreesToPixels( delta.getLongitude(), R0, lngRes);
		double dy = DisplayMap.degreesToPixels( delta.getLatitude(), R0, latRes);
//		System.out.println("width: disp - cell = "+displaySize.width+" - "+size.width+" dx = "+dx);
		int x = (int)((displaySize.width - size.width)/2.0 + DisplayMap.latitudeFactor * dx + 0.5);
//		System.out.println("    height: disp - cell = "+displaySize.height+" - "+size.height+" dy = "+dy+" delta = "+delta.getLatitude());
		int y = (int)((displaySize.height - size.height)/2.0  - dy + 0.5);
		displayPos = new CellPosition(x,y);
	}

	public Position getCenter() {
		return center;
	}

	public BufferedImage getMap(DisplayMap parent) {
		this.parent = parent;
		if (downloadInProgress) {
			return null;
		}
		if (image != null) {
			return image;
		}
		try {
			File file = new File(imageFile);
			BufferedImage image = ImageIO.read(file);
			if (image != null) {
				return image;
			}
		}
		catch (IIOException e) {
//			System.out.println("Image file "+imageFile+" does not exist");
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		MapDownloader downloader = new MapDownloader(this, url);
		downloadInProgress = true;
        downloader.execute();
		return null;
	}

	public void addMap() {
		parent.addMap(this);
	}

	public void download(String url) {
		HttpDownloader downloader = new HttpDownloader(null);
		downloader.start(url);
		
		try {
			InputStream is = downloader.getInputStream();
			image = ImageIO.read(is);
			if (image == null) {
				System.out.println("Failed to downloaded map because "+downloader.getErrorMessage()+" response code = "+downloader.getResponseCode());
			}
			is.close();
//			System.out.println("Finished download for "+url);			
						
		}
		catch (Exception ex) {
			System.out.println("Failed to download map because "+ex.toString());
			ex.printStackTrace();
		}
		

		try {
			File imageDir = new File(mapDirName+File.separator+dirPrefix+zoomLevel);
			if (!imageDir.exists()) {
                if (imageDir.mkdirs()) {
                    System.out.println(imageDir.getAbsolutePath() + " was created");
                }
                else {
                    System.out.println("ERROR: failed to create "+imageDir.getAbsolutePath());
                    return;
                }
            }
            File file = new File(imageFile);			
//			System.out.println("Saving "+file.getAbsolutePath());					
			ImageIO.write(image, imageType, file);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	static private String constructFileName(Position p, int level, Dimension s) {
		DecimalFormat decimalFormat = new DecimalFormat("#.000000");
		String latStr = decimalFormat.format(p.getLatitude());
		double lng = p.getLongitude() > 180 ? p.getLongitude() - 360 : p.getLongitude();
		String lngStr = decimalFormat.format(lng);
		return mapDirName+File.separator+dirPrefix+level+File.separator+"map_"+latStr+"_"+lngStr+"_"+s.width+"x"+s.height+"."+imageType;
//		return mapDirName+File.separator+dirPrefix+level+File.separator+"map_"+latStr+"_"+lngStr+"_"+s.width+"x"+s.width+"."+imageType;
	}

	static private String constructMapUrl(Position p, int level, Dimension s) {
		StringBuilder sb = new StringBuilder(10);
		double lng = p.getLongitude() > 180 ? p.getLongitude() - 360 : p.getLongitude();
		DecimalFormat decimalFormat = new DecimalFormat("#.000000");
		String lngStr = decimalFormat.format(lng);
		String latStr = decimalFormat.format(p.getLatitude());
//		System.out.println(lngStr);
		String apiKey = "AIzaSyDpYBCJkQtV4kwHwZL8sNrjvozogKcHnyI";
		sb.append("https://maps.googleapis.com/maps/api/staticmap?");
		sb.append("center="+latStr+","+lngStr+"&");
		sb.append("zoom="+level+"&");
		sb.append("size="+s.width+"x"+s.height+"&");
//		sb.append("size="+s.width+"x"+s.width+"&");
		sb.append("maptype=roadmap&");
		sb.append("key="+apiKey);
		return sb.toString();
	}

	public String toString() {
		return "center: "+center+" "+size.width+"x"+size.height;
	}
}

class MapDownloader extends SwingWorker<Integer, Integer> {

	private MapCell map;
	private String url;

	public MapDownloader(MapCell map, String url) {
		this.map = map;
		this.url = url;
	}

	protected Integer doInBackground() throws Exception {
		map.download(url);
		return 1;
	}

	protected void done() {
//		System.out.println("map download is complete");
		map.downloadInProgress = false;
		map.addMap();
	}
}
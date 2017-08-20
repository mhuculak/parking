package parking.display;

import parking.database.DBmode;
import parking.map.Position;
import parking.map.Address;
import parking.map.MapBounds;
import parking.map.MapInit;
import parking.map.Sign;
import parking.map.SignMarker;
import parking.database.DBcallback;

import parking.database.DatabaseInterface;
import parking.database.HttpInterface;
import parking.database.MongoDBinterface;
import parking.security.User;
import parking.util.Logger;
import parking.util.LoggingTag;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.ComponentOrientation;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.Console;
import java.io.File;


public class MapTool extends JFrame implements DBcallback {
	private JPanel topPanel = new JPanel();
	private JPanel mapPanel = new JPanel();
	private StreetPanel streetPanel = new StreetPanel(this);
	private ControlPanel controlPanel; 
	private SignPanel signPanel;
	private JPanel messagePanel;
	private JLabel messageLabel;
	private	JSplitPane	splitPaneV = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
	private	JSplitPane	topSplitPaneH = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT);
	private	JSplitPane	bottomSplitPaneH = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT);
	private MapToolMenu menu = new MapToolMenu(this);
	private DisplayMap map;
	private MapOverlay selectedOverlay;
	private SignManager signManager = new SignManager(this);
	private DBmode dbMode;
	private Dimension mapSize = new Dimension(1000, 800);
	private DisplayPosition lastMousePress;
	private List<StreetSegmentOverlay> streetSegments = new ArrayList<StreetSegmentOverlay>();
	private StreetSegmentOverlay currentSegment, workingSegment;
	private DatabaseInterface dbIf;
	private List<SignMarker> signMarkers;
	private final int extra = 100;
	private final int defaultZoom = 15;
	private final double selectRadius = 5.0;
	private final Position defaultPos = new Position( 45.4324491, -73.693791);
	private final Color signOverlayColor = Color.RED;
	
	public MapTool(DBmode dbMode, Integer port, String host, User user) {
		File tagMapsFile = new File("tagMaps.txt");
		this.dbMode = dbMode;
		if (dbMode.equals(DBmode.Http)) {
			dbIf = new HttpInterface(port, host, user);
		}
		else {
			Logger logger = new Logger( tagMapsFile, LoggingTag.Database, this);
			dbIf = new MongoDBinterface(port, logger);
		}
		map = new DisplayMap(this, defaultPos, defaultZoom, mapSize);
		controlPanel = new ControlPanel(map);
		signPanel = new SignPanel((int)mapSize.getHeight());
		MapInit mapInit = new MapInit();

		initGUI();

		mapPanel.addMouseListener(new MouseAdapter() { 
        	public void mousePressed(MouseEvent me) {
        		handleMouseClick(me);       		
        	}

        	public void mouseReleased(MouseEvent me) {
        		handleMouseReleased(me);       		
        	}
        });

        mapPanel.addMouseMotionListener(new MouseMotionAdapter() { 
        	public void mouseDragged(MouseEvent me) {
        		handleMouseDrag(me);       		
        	}

        });
       

	}

	public void saveChanges() {
		if (currentSegment != null) {
			streetSegments.remove(currentSegment);			
		}
		workingSegment.setWorking(false);
		streetSegments.add(workingSegment);
		currentSegment = workingSegment;
		streetPanel.setModeSelecting();
	}

	public void revertChanges() {
		map.removeOverlay(workingSegment);
		workingSegment = null;
		streetPanel.setModeSelecting();
		repaint(); 
	}

	public void mapBoundsChange(MapBounds bounds, DisplayMap m) {
		signMarkers = dbIf.getSignMarkers(bounds, this);		
		if (signMarkers != null) {
			overlaySignMarkers(signMarkers, m);
		}
	}

	public void setSignMarkers(List<SignMarker> signMarkers) {
		this.signMarkers = signMarkers;
		overlaySignMarkers(signMarkers, null);		
	}

	public void overlaySignMarkers(List<SignMarker> markers, DisplayMap m) {
		if (m == null) {
			m = map;
		}
		if (markers == null) {
			System.out.println("no markers to overlay");
			return;
		}
		for (SignMarker s : markers) {			
			Position sPos = s.getPosition();				
			DisplayPosition p = m.latLngToDisplayPosition(sPos);
			MapOverlayPoint signOverlay = new MapOverlayPoint(p.x, p.y, signOverlayColor);
			m.addOverlay(signOverlay);
		}
	}

	public void overlaySigns(Map<String, Sign> signs) {
		Iterator<Map.Entry<String, Sign>> entries = signs.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry<String, Sign> entry = entries.next();
			String id = entry.getKey();
			Sign sign = entry.getValue();
			DisplayPosition p = map.latLngToDisplayPosition(sign.getPosition());
			MapOverlayPoint signOverlay = new MapOverlayPoint(p.x, p.y, signOverlayColor);
			map.addOverlay(signOverlay);
		}
	}

	private void initGUI() {
		getContentPane().add( topPanel );
		mapPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0)); // default vgap & hgap for FlowLayout is 5 pixels (use 0 so mouse/map are not offset)
		mapPanel.setPreferredSize(mapSize);		
		splitPaneV.setLeftComponent( topSplitPaneH);
		splitPaneV.setRightComponent( bottomSplitPaneH );
		topSplitPaneH.setLeftComponent( streetPanel );
		topSplitPaneH.setRightComponent( controlPanel );
		bottomSplitPaneH.setLeftComponent( mapPanel );
		bottomSplitPaneH.setRightComponent( signPanel );
		mapPanel.add(map);
        mapPanel.setVisible(true);
        map.setVisible(true);
//        messagePanel = new JPanel();
//        messagePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
//        messageLabel = new JLabel("Messages:");
//        messagePanel.add(messageLabel);
		topPanel.add( splitPaneV );
		setSize( (int)mapSize.getWidth() + signPanel.getWidth() + extra, (int)mapSize.getHeight() + extra);
	}

	private void handleMouseClick(MouseEvent me) {
//		System.out.println("Got mouse event " +  me.getX() + " " + me.getY());
        lastMousePress = new DisplayPosition( me.getX(), me.getY());
        Position selectedPos = map.getMapPosition(lastMousePress);
//        System.out.println("Selected "+selectedPos+" for pixel "+lastMousePress.x+" "+lastMousePress.y);
        if (SwingUtilities.isLeftMouseButton(me)) {
        	if (streetPanel.getModeSelecting()) {
        		Address streetAddress = Address.reverseGeocode( selectedPos);
 				streetPanel.setStreetAddress(streetAddress);
 				currentSegment = getStreetSegmentOverlay(streetAddress.getStreetName());
 				if (currentSegment != null) { 
 					workingSegment = new StreetSegmentOverlay(currentSegment); // make a copy in case we want to undo changes
 				}
 				else {
 					workingSegment = new StreetSegmentOverlay(streetAddress.getStreetName());					
 				}
 				map.addOverlay(workingSegment);
 				streetPanel.setModeWorking();
        	}
        	else { // normal operation...
        		MapOverlay ovr = checkOverlays(lastMousePress);
        		if ( ovr == null) {
					workingSegment.addPoint(  lastMousePress );
				}
				else {
					if (selectedOverlay != null) {
						selectedOverlay.select(false);
					}
					selectedOverlay = ovr;
					selectedOverlay.select(true);
				}
				repaint(); // forces new point to appear on the DisplayMap (Note: map.repaint() does not work here)
        	}
        }
        else if (SwingUtilities.isRightMouseButton(me)) {
        	System.out.println("Right click");
        	if (streetPanel.getModeSelecting() == false) {
        		MapOverlay ovr = checkOverlays(lastMousePress);
        		if (ovr == null) {
        			System.out.println("No overlay selected...insert new point");
        			workingSegment.insertPoint(  lastMousePress );
        		}
        		else if (ovr instanceof MapOverlayPoint){
        			System.out.println("overlay selected...delete point");
        			//
        			//  FIXME: ambiguous cuz ovr could be a point in the segment or a sign
        			//
        			MapOverlayPoint ovrPoint = (MapOverlayPoint)ovr;
        			workingSegment.deletePoint(ovrPoint);
        		}
        		else {
        			System.out.println("non point overlay selected...ignored");
        		}
        		repaint();
        	}
        }
	}

	private void handleMouseReleased(MouseEvent me) {
		if (selectedOverlay != null) {
			DisplayPosition mouseRelease = new DisplayPosition( me.getX(), me.getY() );
			selectedOverlay.movePoint(mouseRelease);
			repaint();
		}
	}

	private void handleMouseDrag(MouseEvent me) {
		if (selectedOverlay != null) {
			DisplayPosition mousePosition = new DisplayPosition( me.getX(), me.getY() );
			selectedOverlay.movePoint(mousePosition);
			repaint();
		}
	}

	private MapOverlay checkOverlays(DisplayPosition dp) {
		List<MapOverlay> selectable = workingSegment.getSelectable();
		for (MapOverlay ov : selectable) {
//			System.out.println("Checking overlay...");
			if (ov.isCloseTo(dp, selectRadius)) {
				System.out.println("Selecting overlay...");
				return ov;
			}
		}
		return null;
	}

	private StreetSegmentOverlay getStreetSegmentOverlay(String name) {
		for (StreetSegmentOverlay seg : streetSegments) {
			if (seg.getMapSegment().name.equals(name)) {
				return seg;
			}
		}
		return null;
	}
	
	public static void main( String args[]) {
		DBmode dbMode = DBmode.Http;
		Integer port = null;
		String userName = null;
		String host = null;
		for (int i=0 ; i<args.length ; i++) {
			String[] kv = args[i].split("=");
			if (kv.length == 2) {
				String key = kv[0];
				String value = kv[1];
				if (key.equals("mode")) {
					try {
						dbMode = DBmode.valueOf(value);
					}
					catch (IllegalArgumentException e) {
						System.out.println("ERROR: unknown DBmode "+value);
					}
				}
				else if (key.equals("port")) {
					port = Integer.parseInt(value);
				}
				else if (key.equals("user")) {
					userName = value;
				}
				else if (key.equals("host")) {
					host = value;
				}
			}
		}
		User user = null;
		if (userName != null) {
			user = new User(userName);			
			char[] passwordArray = null;			
			Console console = System.console();
			if (console != null) {
		 		passwordArray = console.readPassword("Password: ");
		 		user.setPassword(passwordArray);
			}
			else {
				System.out.println("ERROR: cannot open System.console");
				return;
			}
//			user.setPassword(new char[0]);			
		}
		MapTool mapTool = new MapTool(dbMode, port, host, user);
		mapTool.setVisible(true);
		mapTool.setTitle("Map Editing Tool");
		mapTool.setBackground( Color.gray );
		mapTool.setLayout(new GridLayout(1,1));
	}

	public void save() {

	}

	public void open() {

	}

	public void view() {

	}
}
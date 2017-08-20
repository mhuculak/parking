package parking.display;

import parking.map.Position;

import javax.swing.JPanel;
import javax.swing.JButton;

import java.awt.FlowLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ControlPanel extends JPanel {

	DisplayMap map;

	private final double panRatio = 0.25;
	private final JButton zoomInButton  = new JButton("Zoom In");
    private final JButton zoomOutButton  = new JButton("Zoom Out");
    private final JButton panNorthButton = new JButton("Pan North");
    private final JButton panSouthButton = new JButton("Pan South");
    private final JButton panEastButton = new JButton("Pan East");
    private final JButton panWestButton = new JButton("Pan West");

    public ControlPanel(DisplayMap map) {
    	this.map = map;
    	setLayout(new FlowLayout(FlowLayout.RIGHT));
    	add(zoomInButton);
    	add(zoomOutButton);
    	add(panNorthButton);
    	add(panSouthButton);
    	add(panEastButton);
    	add(panWestButton);
        setVisible(true);

    	zoomInButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               	int zoomLevel = map.getZoomLevel();
               	map.zoom(zoomLevel+1); 
            }
        });

        zoomOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               	int zoomLevel = map.getZoomLevel();
               	map.zoom(zoomLevel-1);
            }
        });

        panNorthButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {            	
            	pan( map.getLatitudeSize()*panRatio, 0);
            }
        });

        panSouthButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {            	
            	pan( -map.getLatitudeSize()*panRatio, 0);
            }
        });

        panEastButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {            	
            	pan( 0, map.getLongitudeSize()*panRatio);
            }
        });

        panWestButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {            	
            	pan( 0, -map.getLongitudeSize()*panRatio);
            }
        });
    }

    private void pan(double lat, double lng) {
    	Position currCenter = map.getCenter();
    	Position newCenter = new Position( currCenter.getLatitude() + lat, currCenter.getLongitude() + lng);
        map.pan(newCenter);
    }
}
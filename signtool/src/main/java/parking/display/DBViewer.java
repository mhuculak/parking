package parking.display;

import parking.map.Sign;
import parking.map.Address;
import parking.util.Logger;
import parking.util.LoggingTag;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BoxLayout;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DBViewer extends DbSelector {

	private Map<String, Sign> signs;
	private Logger m_logger;

	public DBViewer(Logger logger) {
		super(logger, "DB Viewer", true);
		m_logger = new Logger(logger, this, LoggingTag.Database);
		m_logger.enable(LoggingTag.Database);
		initDisplay();
	}

	public void viewDatabase() {

	}

	private void viewSigns(String db) {
		getSigns();
		for (String id : signs.keySet()) {
			Sign sign = signs.get(id);
			String user = dbIf().getSignDB().getUser(id);
			user = user == null ? "" : user;
			StringBuilder sb = new StringBuilder(10);
			sb.append(id+" "+sign.getTimeStamp());
			if (sign.getPosition() != null) {
//				Address address = sign.getAddress();
				Address address = null;
				if (address != null) {
					String locality = address.getTown() == null ? address.getCity() : address.getTown();
					locality = locality == null ? address.getProvinceState() : locality;
					locality = locality == null ? "" : locality;
					sb.append(" address: "+address.getShortAddress()+" "+locality);
				}
				else {
					sb.append(" position: "+sign.getPosition().shortString());
				}
			}
			else {
				sb.append(" position: null");
			}
			String imageName = sign.getImageName() == null ? "null" : sign.getImageName();
			String autoSchedule = sign.getAutoSchedule() == null ? "null" : sign.getAutoSchedule().toString();
			String schedule = sign.getParkingSchedule() == null ? "null" : sign.getParkingSchedule().toString();
			sb.append(" image: "+imageName+" auto: "+autoSchedule+" schedule: "+schedule+" user: "+user+" updates: "+sign.getUpdates());
			m_logger.log(sb.toString());
		}		
	}

	private void getSigns() {
		List<String> allSigns = dbIf().getSignDB().getSignsIDs();
		signs = new HashMap<String, Sign>();
		for (String id : allSigns) {
			Sign sign = dbIf().getSignDB().getSign(id);
			if (sign.getTimeStamp() == null) {
				sign = dbIf().getSignDB().getSignOLD(id);
			}
			signs.put(id, sign);
		}
	}

	private void viewPictures(String db) {
		
	}

	private void selectDateRangeFilter() {

	}

	private void selectUserFilter() {

	}

	private void initDisplay() {
		JLabel statusLabel = new JLabel();
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		getContentPane().add(mainPanel);
		setSize(600, 600);
      	setVisible(true);
      	JPanel instructionPanel = new JPanel(new FlowLayout());
      	JLabel instructionLabel = new JLabel("Select database to view.");
      	instructionPanel.add(instructionLabel);
      	mainPanel.add(instructionPanel);
      	mainPanel.add(getSelectPanel());
      	JPanel controlPanel = new JPanel(new FlowLayout());
      	JButton signButton = new JButton("View Signs");
      	signButton.addActionListener(new ActionListener() {
         	public void actionPerformed(ActionEvent e) {
         		if (selectedDB() == null) {
         			statusLabel.setText("No database selected." );
         		} 	
         		else {	
            		viewSigns(selectedDB());
            	}           	          	
         	}          
      	});
      	controlPanel.add(signButton);
      	JButton pictureButton = new JButton("View Pictures");
      	signButton.addActionListener(new ActionListener() {
         	public void actionPerformed(ActionEvent e) {
         		if (selectedDB() == null) {
         			statusLabel.setText("No database selected." );
         		} 	
         		else {	
            		viewPictures(selectedDB());
            	}           	          	
         	}          
      	});
      	controlPanel.add(pictureButton);
      	mainPanel.add(controlPanel);
      	JPanel filterPanel = new JPanel(new FlowLayout());
      	JButton dateButton = new JButton("Date Filter");
      	dateButton.addActionListener(new ActionListener() {
         	public void actionPerformed(ActionEvent e) {         		
            	selectDateRangeFilter();          	          	
         	}          
      	});
      	filterPanel.add(dateButton);
      	JButton userButton = new JButton("User Filter");
      	userButton.addActionListener(new ActionListener() {
         	public void actionPerformed(ActionEvent e) {         		
            	selectUserFilter();          	          	
         	}          
      	});
      	filterPanel.add(userButton);
      	mainPanel.add(filterPanel);
      	JPanel statusPanel = new JPanel(new FlowLayout());            	
      	statusPanel.add(statusLabel);
		mainPanel.add(statusPanel);   	
	}
}
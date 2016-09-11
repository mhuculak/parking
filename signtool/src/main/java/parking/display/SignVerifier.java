package parking.display;

import parking.map.Sign;
import parking.map.Address;
import parking.schedule.ParkingSignType;
import parking.schedule.ParkingSchedule;
import parking.schedule.TimeRange;
import parking.schedule.SimpleTime;
import parking.schedule.WeekDaySet;
import parking.schedule.DateRange;
import parking.schedule.SimpleDate;
import parking.opencv.SignRecognizer;

import parking.util.Logger;
import parking.util.Profiler;
import parking.util.Utils;

import com.mongodb.gridfs.GridFSDBFile;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.BoxLayout;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.SwingWorker;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.awt.event.WindowEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class RecogWorker extends SwingWorker<Integer, Integer> {

	private SignVerifier signVerifier;

	public RecogWorker(SignVerifier signVerifier) {
		this.signVerifier = signVerifier;
	}

	protected Integer doInBackground() throws Exception {
//		System.out.println("Running test in background...");
		signVerifier.recognize();
		return 1;
	}

	protected void done() {

	}
}

public class SignVerifier extends DbSelector { // Note: DbSelector is the main JFrame
	
	private JFrame thumbFrame;
	private JFrame signFrame;
	private JFrame pageFrame; 
	private JFrame verifyFrame;
	private JPanel thumbPanel; 
	private JPanel instructionPanel;	
	private JPanel statusPanel;
	private JPanel controlPanel;
	private JLabel instructionLabel;
	private JLabel statusLabel;        // used by the main frame
	private JLabel verifyStatusLabel;   // used by verify frame
	private JButton button;
	private Logger logger;
	private List<String> signIDs;
	private Map<String, Sign> signs;
	private Map<Sign, ThumbNail> thumbnails;
	private int numPages;
	private int numRows;
	private Sign selectedSign;
	private ParkingSignType selectedSignType;
	private ParkingSchedule recognizedSchedule;
	private RecogWorker recogWorker;

	final int rowLen = 6;
	final int pageLen = 2;
	
	public SignVerifier(Logger logger) {
		super(logger, "Sign Verifier", false);
		this.logger = logger;
      	initDisplay();
	}

	private void show(Sign sign) {
		try {
			BufferedImage image = ImageIO.read(dbIf().getPictureDB().getPicture(sign.getImageName()).getInputStream());
			SignImage signImageFull = new SignImage(image, logger);
			double scale = 800.0/signImageFull.getHeight();
			SignImage signImage = scale < 1.0 ? signImageFull.scale(scale) : signImageFull;
			show(signImage);
		}
		catch (Exception ex) {
			logger.error("Caught "+ex+" trying to fetch image for sign "+sign);
		}		
	}

	private void show(SignImage signImage) {
		signFrame = new JFrame();
		signFrame.setVisible(true);
		signFrame.setTitle("Display Sign");
		JPanel signPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		signFrame.add(signPanel);
		signPanel.add(signImage);
		signFrame.setSize(signImage.getWidth(), signImage.getHeight());
	}

	private void verify(Sign sign, ParkingSchedule auto) {
		SignVerifier verifier = this;
		ParkingSchedule schedule = auto;
		if (auto == null) {
			schedule = sign.getParkingSchedule() == null ? sign.getAutoSchedule() : sign.getParkingSchedule();
		}
				
		verifyFrame = new JFrame();
		verifyFrame.setSize(500, 700);			
		verifyFrame.setVisible(true);
		verifyFrame.setTitle("Verify Sign Data");
		JPanel mainPanel = new JPanel();
		verifyFrame.getContentPane().add(mainPanel);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		JPanel typePanel = new JPanel(new FlowLayout());
		mainPanel.add(typePanel);
		JLabel typeLabel = new JLabel("Sign Type");
		typePanel.add(typeLabel);
		boolean typeValue = schedule == null ? true : schedule.isRestricted();
		final JRadioButton parking = new JRadioButton("Parking", !typeValue);
      	final JRadioButton noParking = new JRadioButton("No Parking", typeValue);
      	selectedSignType = typeValue ? ParkingSignType.NOPARKING : ParkingSignType.PARKING;
      	typePanel.add(parking);
      	typePanel.add(noParking);
		parking.addItemListener(new ItemListener() {
         	public void itemStateChanged(ItemEvent e) {         
            	selectedSignType = ParkingSignType.PARKING;
         	}           
      	});
		noParking.addItemListener(new ItemListener() {
         	public void itemStateChanged(ItemEvent e) {         
            	selectedSignType = ParkingSignType.NOPARKING;
         	}           
      	});
    
		JPanel timeLimitPanel = new JPanel(new FlowLayout());
		mainPanel.add(timeLimitPanel);
		JLabel timeLimitLabel = new JLabel("Time Limit (minutes): ");
		timeLimitPanel.add(timeLimitLabel);
		String timeLimitValue = schedule == null ? "" : Integer.toString(schedule.getTimeLimitMinutes()); 
		final TextField timeLimitText = new TextField(3);
      	timeLimitText.setText(timeLimitValue);
      	timeLimitPanel.add(timeLimitText);

		JPanel timeRangePanel = new JPanel(new FlowLayout());
		mainPanel.add(timeRangePanel);
		JLabel timeRangeLabel = new JLabel("Time Range: ");
		timeRangePanel.add(timeRangeLabel);

		final TextField startHour = new TextField(2);
		timeRangePanel.add(startHour);
		JLabel colon1 = new JLabel(":");
		timeRangePanel.add(colon1);
		final TextField startMin = new TextField(2);
		timeRangePanel.add(startMin);
		JLabel to1 = new JLabel(" to ");
		final TextField endHour = new TextField(2);
		timeRangePanel.add(endHour);
		JLabel colon2 = new JLabel(":");
		timeRangePanel.add(colon2);
		final TextField endMin = new TextField(2);
		timeRangePanel.add(endMin);
		if (schedule != null && schedule.getTimeRange() != null) {
			startHour.setText(schedule.getTimeRange().getStart().getHourString());
			startMin.setText(schedule.getTimeRange().getStart().getMinuteString());
			endHour.setText(schedule.getTimeRange().getEnd().getHourString());
			endMin.setText(schedule.getTimeRange().getEnd().getMinuteString());
		} 
		
		JPanel weekDayPanel = new JPanel();
		weekDayPanel.setBounds(61, 11, 81, 140);
        weekDayPanel.setLayout(new BoxLayout(weekDayPanel, BoxLayout.Y_AXIS));
		mainPanel.add(weekDayPanel);
		JLabel weekDayLabel = new JLabel("Week Days: ");
		weekDayPanel.add(weekDayLabel);
		final List<JCheckBox> weekDays = new ArrayList<JCheckBox>();
		for ( String day : WeekDaySet.allDays) {
            JCheckBox dayCheckBox = schedule == null || schedule.getWeekDays() == null ? new JCheckBox(day) : 
            				new JCheckBox(day, schedule.getWeekDays().getWeekDayMap().get(day.toString()));
            weekDayPanel.add(dayCheckBox);
            weekDays.add(dayCheckBox);
         }

		JPanel dateRangePanel = new JPanel(new FlowLayout());
		mainPanel.add(dateRangePanel);
		JLabel dateRangeLabel = new JLabel("Date Range: ");
		dateRangePanel.add(dateRangeLabel);
		final TextField startDay = new TextField(2);
		dateRangePanel.add(startDay);
		JLabel space = new JLabel(" ");
		dateRangePanel.add(space);
		final TextField startMonth = new TextField(10);
		dateRangePanel.add(startMonth);
		JLabel to2 = new JLabel(" to ");
		dateRangePanel.add(to2);
		final TextField endDay = new TextField(2);
		dateRangePanel.add(endDay);
		dateRangePanel.add(space);
		final TextField endMonth = new TextField(10);
		dateRangePanel.add(endMonth);
		if (schedule != null && schedule.getDateRange() != null) {
			startDay.setText(schedule.getDateRange().getStart().getDay());
			startMonth.setText(schedule.getDateRange().getStart().getMonth());
			endDay.setText(schedule.getDateRange().getEnd().getDay());
			endMonth.setText(schedule.getDateRange().getEnd().getMonth());
		} 

		JPanel addressPanel = new JPanel(new FlowLayout());
		mainPanel.add(addressPanel);
		
//		JLabel addressLabel = new JLabel("Address: Temporarily Unavailable due to Google");
//		addressPanel.add(addressLabel);
		
		JLabel addressLabel = new JLabel("Address: ");
		addressPanel.add(addressLabel);
		final TextField streetNum = new TextField(4);
		addressPanel.add(streetNum);
		final TextField streetName = new TextField(10);
		addressPanel.add(streetName);
		final TextField city = new TextField(10);
		addressPanel.add(city);
		final TextField postal = new TextField(6);
		addressPanel.add(postal);
		if (sign.getPosition() != null) {
			
			Address address = sign.getAddress();
			if (address != null) {
				
				streetNum.setText(address.getStreetNumber());
				streetName.setText(address.getStreetName());
				city.setText(address.getCity());
				postal.setText(address.getPostalCodeZIP());
				
			}			
		}

		JPanel submitPanel = new JPanel(new FlowLayout());
		mainPanel.add(submitPanel);
      	JButton submitButton = new JButton("Submit");
      	submitButton.addActionListener(new ActionListener() {
         	public void actionPerformed(ActionEvent e) {
//         		ParkingSchedule verifiedSchedule = new ParkingSchedule(selectedSignType, logger);
         		ParkingSchedule verifiedSchedule = new ParkingSchedule(selectedSignType);
         		TimeRange timeRange = new TimeRange( new SimpleTime( startHour.getText(), startMin.getText() ), new SimpleTime( endHour.getText(), endMin.getText() ) );
         		if (timeRange.isValid()) {
         			verifiedSchedule.setTimeRange(timeRange);
         		}
         		else {
         			String tr = startHour.getText()+":"+startMin.getText()+","+endHour.getText()+":"+endMin.getText();
         			if (!tr.equals(":,:")) {
         				verifyStatus("time range "+tr+" is invalid");
         				return;
         			}         			      			
         		}
         		WeekDaySet weekDaySet = new WeekDaySet();
         		int k=0;
         		for ( JCheckBox day : weekDays) {
         			if (day.isSelected()) {
         				weekDaySet.add(k);
         			}
         			k++;
         		}
         		if (weekDaySet.size() > 0 ) {
         			verifiedSchedule.setWeekDays(weekDaySet);
         		}
         		DateRange dateRange = new DateRange( new SimpleDate( startMonth.getText(), startDay.getText()), new SimpleDate( endMonth.getText(), endDay.getText()));
         		if (dateRange.isValid()) {
         			verifiedSchedule.setDateRange(dateRange);
         		}
         		else {
         			String tr = startDay.getText()+":"+startMonth.getText()+","+endDay.getText()+":"+endMonth.getText();
         			if (!tr.equals(":,:")) {
         				verifyStatus("time range "+tr+" is invalid");
         				return;
         			}         			
         		}
         		if (!verifiedSchedule.isRestricted()) {
         			verifiedSchedule.setTimeLimitMinutes(timeLimitText.getText());
         			if (verifiedSchedule.getTimeLimitMinutes() == 0 && Utils.isNotBlank(timeLimitText.getText())) {
         				verifyStatus("time limit "+timeLimitText.getText()+" is invalid");
         				return;
         			}
         		}
         		verifiedSchedule.setFlags();
         		sign.setParkingSchedule(verifiedSchedule);
         		dbIf().getSignDB().updateSign(sign, null);
         		message("Added verified schedule "+verifiedSchedule+" to database");
         		verifyFrame.dispatchEvent(new WindowEvent(verifyFrame, WindowEvent.WINDOW_CLOSING));
         		signFrame.dispatchEvent(new WindowEvent(signFrame, WindowEvent.WINDOW_CLOSING));        	
         	}          
      	});      	
      	submitPanel.add(submitButton);
      	JButton cancelButton = new JButton("Cancel");
      	submitPanel.add(cancelButton);
      	cancelButton.addActionListener(new ActionListener() {
      		public void actionPerformed(ActionEvent e) {
      			verifyFrame.dispatchEvent(new WindowEvent(verifyFrame, WindowEvent.WINDOW_CLOSING));
      			signFrame.dispatchEvent(new WindowEvent(signFrame, WindowEvent.WINDOW_CLOSING));
      		}          
      	});  
      	JButton recogButton = new JButton("Recognize");
      	submitPanel.add(recogButton);
      	recogButton.addActionListener(new ActionListener() {
      		public void actionPerformed(ActionEvent e) {
      			verifyFrame.dispatchEvent(new WindowEvent(verifyFrame, WindowEvent.WINDOW_CLOSING));
      			signFrame.dispatchEvent(new WindowEvent(signFrame, WindowEvent.WINDOW_CLOSING));
      			recogWorker = new RecogWorker(verifier);
            	recogWorker.execute();
            	statusLabel.setText("Recognizing sign..." );
      		}          
      	});
      	JButton deleteButton = new JButton("Delete");
      	submitPanel.add(deleteButton);
      	deleteButton.addActionListener(new ActionListener() {
      		public void actionPerformed(ActionEvent e) {
      			removeSign(sign);
      			verifyFrame.dispatchEvent(new WindowEvent(verifyFrame, WindowEvent.WINDOW_CLOSING));
      			signFrame.dispatchEvent(new WindowEvent(signFrame, WindowEvent.WINDOW_CLOSING));
      		}          
      	});  

      	JPanel verifyStatusPanel = new JPanel(new FlowLayout());
		mainPanel.add(verifyStatusPanel);
		if (verifyStatusLabel == null) {
			verifyStatusLabel = new JLabel();
		}
		verifyStatusPanel.add(verifyStatusLabel);
	}

	private void verifyStatus(String message) {
		verifyStatusLabel.setText(message);
		logger.log(message);
	}

	private void setup(String db) {
		fetchThumbs(db);
		numRows = (thumbnails.size() % rowLen) == 0 ? thumbnails.size() / rowLen : thumbnails.size() / rowLen + 1;
		numPages = numRows % pageLen == 0 ? numRows / pageLen : numRows / pageLen + 1;
		System.out.println("num rows = "+numRows+" num pages = "+numPages);
		displayPageActivatorButtons();	
	}

	private void removeSign(Sign sign) {
		thumbFrame.dispatchEvent(new WindowEvent(signFrame, WindowEvent.WINDOW_CLOSING));
		pageFrame.dispatchEvent(new WindowEvent(signFrame, WindowEvent.WINDOW_CLOSING));
		dbIf().getSignDB().removeSign(sign);
		thumbnails.remove(sign);
		numRows = (thumbnails.size() % rowLen) == 0 ? thumbnails.size() / rowLen : thumbnails.size() / rowLen + 1;
		numPages = numRows % pageLen == 0 ? numRows / pageLen : numRows / pageLen + 1;
		displayPageActivatorButtons();
	}

	private void fetchThumbs(String db) {
		message("Fetching "+selectedDbSize()+" thumbnails from database "+db+"...");
		List<String> allSigns = dbIf().getSignDB().getSignsIDs();
		signs = new HashMap<String, Sign>();
		thumbnails = new HashMap<Sign, ThumbNail>();
		int fetched = 0;
		signIDs = new 	ArrayList<String>();
		for (String id : allSigns) {
			Sign sign = dbIf().getSignDB().getSign(id);
			signs.put(id, sign);			
			try {
				String picName = sign.getImageName();
				if (picName != null) {
					GridFSDBFile gfsFile = dbIf().getPictureDB().getPicture( picName );
					if (gfsFile != null) {
						BufferedImage signImage = ImageIO.read(gfsFile.getInputStream());
						if (signImage != null) {
							ThumbNail thumb = new ThumbNail(signImage, sign.displayText());
							thumbnails.put(sign, thumb);
							signIDs.add(id);
							fetched++;
						}
						else {
							logger.error("Could not load picture "+picName+" from the database");
						}
					}
					else {
						logger.error("picture "+picName+" was not found in the database for sign "+sign.toString());
					}
				}
				else {
					logger.error("sign "+sign.toString()+" does not have a picture");
				}
			}
			catch (Exception ex) {
				logger.error("Caught "+ex+" trying to fetch image for sign "+sign);
				ex.printStackTrace();
			}
			statusLabel.setText("Progress: "+(100*fetched/selectedDbSize())+"%");
		}
		System.out.println("Fetched "+thumbnails.size()+" thumbnails");
		message("Fetched "+fetched+" thumbnails");
	}

	public void recognize() {
		Profiler profiler = new Profiler("recognize", null, this);
		try {
			SignRecognizer signRecognizer = new SignRecognizer(dbIf().getPictureDB().getPicture(selectedSign.getImageName()).getInputStream(), logger, profiler);
			ParkingSchedule recognizedSchedule = signRecognizer.readSign(0);			
			profiler.stop();
			if (recognizedSchedule != null) {
				verify(selectedSign, recognizedSchedule);
				show(signRecognizer.getWorkingImage());
				message("Done recognizing sign "+recognizedSchedule.toString() );
			}
			else {
				message("Done recognizing sign..no result" );
			}
		}
		catch (Exception ex) {
			message("Caught exception "+ex+" while recognizing sign");
		}
	}

	private void displayThumbPage( int page ) {		
		int numRows = pageLen;
		int numCols = rowLen;
		int n = numRows*numCols; 
		if (page == numPages-1) {
			n = thumbnails.size() % (pageLen*rowLen);
			if (n == 0) {
				n = pageLen * rowLen;
			}
			numRows = n % rowLen == 0 ? n / rowLen : n / rowLen + 1;
			if ( numRows == 1 ) { // last page has 1 row
				numCols = n;
			}
			System.out.println("Display last page rows = "+numRows+" cols = "+numCols);			
		}
		else {
			System.out.println("Display full page rows = "+numRows+" cols = "+numCols);
		}
		
		int first = page*pageLen*rowLen;
		
		thumbFrame = new JFrame("Thumbnail Page");
		thumbFrame.setSize(400, 400);
		thumbFrame.setVisible(true);
		
		JPanel thumbPanel = new JPanel(new GridLayout(numRows, numCols));
		thumbFrame.add(thumbPanel);
		int i,k;
		System.out.println("Display thumbs first = "+first+" n = "+n);
		for ( k=first, i=0 ; i<n ; i++, k++) {
			String id = signIDs.get(k);
			Sign sign = signs.get(id);
			ThumbNail thumb = thumbnails.get(sign);
			if (thumb != null) {
				JPanel signPanel = new JPanel(new GridLayout(2, 1));
				if (thumb.getImage() != null) {
					JButton btn = new JButton(new ImageIcon(thumb.getImage()));      
      				btn.addActionListener(new SignClickListener());
      				btn.setActionCommand(id);
      				signPanel.add(btn);
      				JEditorPane signLabel = new JEditorPane();
      				signLabel.setEditable(false);
      				signLabel.setContentType("text/html");
      				signLabel.setText(thumb.getCaption());
      				signPanel.add(signLabel);
      				thumbPanel.add(signPanel);
      			}
      			else {
      				System.out.println("ERROR: unexpected null thumbnail image at "+k+" for sign "+sign.toString());
      			}
      		}
      		else {
      			System.out.println("ERROR: unexpected null thumbnail at "+k+" for sign "+sign.toString());
      		}
		}
	}

	private void displayPageActivatorButtons() {		
		pageFrame = new JFrame();
		pageFrame.setLayout(new GridLayout(4, 1));
		pageFrame.setVisible(true);
		pageFrame.setSize(400, 400);
		pageFrame.setTitle("Page Selection");
		JPanel instrPanel = new JPanel(new FlowLayout());
		pageFrame.add(instrPanel);
		JLabel instrLabel = new JLabel("Select a thumbnail page");
		instrPanel.add(instrLabel);
				
		JPanel pagePanel = new JPanel(new FlowLayout());
		for ( int i=0 ; i<numPages ; i++) {
			final int index = i;
			JButton b = new JButton("Page "+i);
      		b.addActionListener(new ActionListener() {
         		public void actionPerformed(ActionEvent e) {
         			displayThumbPage(index);         	          	
         		}          
      		});
      		pagePanel.add(b);      
		}		
		pageFrame.add(pagePanel);
		
		JPanel donePanel = new JPanel(new FlowLayout());
		JButton done = new JButton("Done");
      	done.addActionListener(new ActionListener() {
         	public void actionPerformed(ActionEvent e) {
         		pageFrame.dispatchEvent(new WindowEvent(pageFrame, WindowEvent.WINDOW_CLOSING));
         		thumbFrame.dispatchEvent(new WindowEvent(thumbFrame, WindowEvent.WINDOW_CLOSING));       	          	
         	}          
      	}); 
      	donePanel.add(done);     	
      	pageFrame.add(donePanel);
      	
	}
	
	private void initDisplay() {	
//		setLayout(new GridLayout(4, 1));
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		getContentPane().add(mainPanel);

      	setSize(400, 400);
      	setVisible(true); 

      	instructionPanel = new JPanel(new FlowLayout());
      	instructionLabel = new JLabel("Select database to verify.");
      	instructionPanel.add(instructionLabel);
//      	add(instructionPanel);  
//      	add(getSelectPanel());
      	mainPanel.add(instructionPanel);
      	mainPanel.add(getSelectPanel());
      	controlPanel = new JPanel(new FlowLayout());
      	button = new JButton("Verify Signs");
      	button.addActionListener(new ActionListener() {
         	public void actionPerformed(ActionEvent e) {
         		if (selectedDB() == null) {
         			statusLabel.setText("No database selected." );
         		} 	
         		else {	
            		setup(selectedDB());
            	}           	          	
         	}          
      	});      	
      	controlPanel.add(button);
//      	add(controlPanel);
      	mainPanel.add(controlPanel);
      	statusPanel = new JPanel(new FlowLayout());            	
      	statusLabel = new JLabel();
      	statusPanel.add(statusLabel);
//      	add(statusPanel);
		mainPanel.add(statusPanel);    	
	}

	private void message(String message) {
		logger.log( message);
		statusLabel.setText( message );
	}

	private class SignClickListener implements ActionListener{
      	public void actionPerformed(ActionEvent e) {
         	String id = e.getActionCommand();  
         	System.out.println("Selected Thumbnail " + id);
         	selectedSign = signs.get(id);
         	show(selectedSign);
         	verify(selectedSign, null);
      	}		
	}
}
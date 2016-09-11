package parking.display;

import parking.database.MongoInterface;
import parking.schedule.ParkingSchedule;
import parking.schedule.ReadError;
import parking.schedule.ParkingElement;
import parking.schedule.ResultType;
import parking.opencv.SignRecognizer;
import parking.map.Sign;
import parking.util.Logger;
import parking.util.LoggingTag;
import parking.util.Profiler;

import com.mongodb.gridfs.GridFSDBFile;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.SwingWorker;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
//import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

class ErrorAccumulator {
	private Map<ParkingElement, Map<ResultType,Integer>> elementErrorCounts;
	private Map<ParkingElement, Integer> elementRecogCounts;
	private Map<ParkingElement, Map<ResultType, Double>> elementErrorRates;
	private Map<ResultType, Double> sum;
	private int numCorrect;
	private int recogTotal;
	private double signErrorRate;
	private String metric;

	public ErrorAccumulator(String metric) {
		this.metric = metric;
	}

	public  Map<ParkingElement, Map<ResultType, Double>> getErrorRates() {
		return elementErrorRates;
	}

	public Map<ResultType, Double> getElementSum() {
		return sum;
	}

	public void add(ReadError error) {
		if (elementErrorCounts == null) {
			elementErrorCounts = new HashMap<ParkingElement, Map<ResultType,Integer>>();
			elementRecogCounts = new HashMap<ParkingElement, Integer>();
			elementErrorRates = new HashMap<ParkingElement, Map<ResultType, Double>>();
		}		
		Map<ParkingElement, ResultType> errMap = error.getErr();
//		System.out.println("Adding error map with "+errMap.size()+" values");
		for (ParkingElement element : errMap.keySet()) {
			Integer recogCount = elementRecogCounts.get(element);
			if (recogCount == null) {
				elementRecogCounts.put(element, 1);
			}
			else {
				elementRecogCounts.put(element, ++recogCount);
			}
			Map<ResultType,Integer> typeErrCount = elementErrorCounts.get(element);
			if (typeErrCount == null) {
				typeErrCount = new HashMap<ResultType,Integer>();
			}
			ResultType type = errMap.get(element);

			Integer typeCount = typeErrCount.get(type);
			if (typeCount == null) {				
				typeErrCount.put(type, 1);
			}
			else {
				typeErrCount.put(type, ++typeCount);
			}
			elementErrorCounts.put(element, typeErrCount);
			typeCount = typeErrCount.get(type);
//			System.out.println("Got count "+typeCount+" for "+metric+" "+element.toString()+" "+type.toString());
		}
//		System.out.println("elementErrorCounts has "+elementErrorCounts.size()+" values");
//		System.out.println("elementRecogCounts has "+elementRecogCounts.size()+" values");
	}

	public void compute() {
		System.out.println("compute normal error rates...");
		for (ParkingElement element : ParkingElement.values()) {
			Integer num = elementRecogCounts.get(element);
			Map<ResultType,Integer> typeErrCount = elementErrorCounts.get(element);
			Map<ResultType, Double> typeErrRate = new HashMap<ResultType, Double>();
			if (num != null && typeErrCount != null) {
				for (ResultType type : ResultType.values()) {
					Integer count = typeErrCount.get(type);
					if (count != null) {
						typeErrRate.put(type, count.doubleValue()/num.doubleValue());
					}
					else {
						typeErrRate.put(type, 0.0);
					}
				}
			}
			else {
				for (ResultType type : ResultType.values()) {
					typeErrRate.put(type, 0.0);
				}
			}
			elementErrorRates.put(element, typeErrRate);
		}
		System.out.println("sum errors across elements...");
		sum = new HashMap<ResultType, Double>();
		for (ResultType type : ResultType.values()) {
			double num = 0;
			double count = 0;
			for (ParkingElement element : ParkingElement.values()) {
				Integer n = elementRecogCounts.get(element);
				if ( n != null) {
					num += n.doubleValue();
				}
				Map<ResultType,Integer> typeErrCount = elementErrorCounts.get(element);
				if (typeErrCount != null) {
					Integer c = typeErrCount.get(type);
					if ( c!= null) {
						count += c.doubleValue();
					}
				}
			}
			double rate = num > 0 ? count / num : 0.0;
			sum.put(type, rate);
		}
		System.out.println("done compute");
	}
}

class RecogResult {
	private ParkingSchedule correct;
	private List<ParkingSchedule> topChoices;
	private Map<String, ParkingSchedule> choiceMap;
	private Map<String, ReadError> errors;
	private Exception ex;
	private Profiler profiler;

	public RecogResult(ParkingSchedule correct, Profiler profiler) {
		this.correct = correct;
		this.profiler = profiler;
	}

	public RecogResult(Exception ex, Profiler profiler) {
		this.ex = ex;
		this.profiler = profiler;
	}

	public void add(ParkingSchedule choice, List<String> metrics) {
		if ( topChoices == null ) {
			topChoices = new ArrayList<ParkingSchedule>();
			errors = new HashMap<String, ReadError>();
			choiceMap = new HashMap<String, ParkingSchedule>();
		}
		topChoices.add(choice);
		ReadError err = new ReadError(correct, choice);
		for (String m: metrics) {
//			System.out.println("Add for "+m+" err = "+err.toString());
			errors.put(m, err );
			choiceMap.put(m, choice);
		}
	}

	public Map<String, ReadError> getErrors() {
		return errors;
	}

	public ParkingSchedule getChoice(String metric) {
		return choiceMap.get(metric);
	}

	public boolean isCorrect(String metric) {
		String correctString = correct.toString();
		boolean correct1 = correctString.equals(getChoice(metric).toString());
		boolean sanity = errors.get(metric).isCorrect();
		if (correct1 != sanity) {
//			System.out.println("ERROR: sanity check failed for coreect = "+correct+" "
//				+getChoice(metric).toString()+" "+errors.get(metric).toString());
		}
		return correct1;
	}

	public String toString() {
		if (topChoices != null && topChoices.size() > 0) {
			return topChoices.get(0).toString();
		}
		else if (ex != null) {
			return ex.toString();
		}
		else {
			return "NO RESULT";
		}
	}
}

class TestWorker extends SwingWorker<Integer, Integer> {

	private SignTester signTester;

	public TestWorker(SignTester signTester) {
		this.signTester = signTester;
	}

	protected Integer doInBackground() throws Exception {
//		System.out.println("Running test in background...");
		signTester.run();
		return 1;
	}

	protected void done() {

	}
}

public class SignTester extends DbSelector {
	
	
	private JPanel instructionPanel;
	private JPanel selectPanel;
	private JPanel statusPanel;
	private JPanel controlPanel;
	private JLabel instructionLabel;
	private String instructionMessage;
	private String testingMessage;
	private JLabel statusLabel;
	private JButton button;
	private Logger logger;
	private boolean testCanceled;
	private boolean testRunning;
	private int recognitionsPerformed;
	private int recognitionsSkipped;
	private Map<Integer, String> exceptions;
	private int testSize;
	private TestWorker testWorker;
	private List<String> signIDs;
	private List<RecogResult> results;
	private Map<String, ErrorAccumulator> errorAccumulator;
	private List<String> auto;

	
	private final String[] metrics = {"auto", "top", "combined", "color", "edge", "shape"};

	public SignTester(Logger logger) {
		super(logger, "Sign Tester", true);
		this.logger = new Logger(logger, this, LoggingTag.Accuracy);
		this.logger.enable(LoggingTag.Accuracy);
		testRunning = false;
      	initDisplay();
	}

	public void run() {		
		for ( int i=0 ; i<testSize ; i++ ) {
			recognize(i);
			if (testCanceled) {
				cleanup("Test cancelled.");
				return;
			}
		}
		computeResults();
		System.out.println("done test print results...");
//		printResults();
		displayResults();
		cleanup("Test complete.");
	}

	private void recognize(int recogNum) {
//		System.out.println("Staring recog "+recogNum);
		Profiler profiler = new Profiler("recognize", null, this);
		try {
			Sign sign = dbIf().getSignDB().getSign(signIDs.get(recogNum));
			if (sign != null && sign.getImageName() != null) {
				GridFSDBFile gfsFile = dbIf().getPictureDB().getPicture( sign.getImageName() );	
				if (gfsFile != null) {	
						
					SignRecognizer signRecognizer = new SignRecognizer(gfsFile.getInputStream(), logger, profiler);			
					ParkingSchedule schedule = signRecognizer.readSign(0);			
					profiler.stop();
					ParkingSchedule correctSched = sign.getParkingSchedule();
					if (correctSched != null) {
						logger.log("get "+signRecognizer.size()+" results from recog "+recogNum+" correct = "+correctSched.toString());
						RecogResult result = new RecogResult(sign.getParkingSchedule(), profiler);
						ParkingSchedule autoSched = sign.getAutoSchedule();
						if (autoSched != null) {				 
							result.add(autoSched, auto);
						}
						result.add(schedule, signRecognizer.getTopChoices().get(0).getWinList());
						if (signRecognizer.size() > 0) {
							result.add(schedule, signRecognizer.getTopChoices().get(0).getWinList());
							printWinList(signRecognizer.getTopChoices().get(0).getWinList());
							for ( int i=1 ; i<signRecognizer.size() ; i++ ) {
								result.add( signRecognizer.readSign(i),  signRecognizer.getTopChoices().get(i).getWinList());
								printWinList(signRecognizer.getTopChoices().get(i).getWinList());
							}				
						}
						results.add(result);
						accumulate(result);
						recognitionsPerformed++;
					}
					else {
						recognitionsPerformed++;
					}
				}
				else {
					recognitionsSkipped++;
				}
			}
			else {
				recognitionsSkipped++;
			}
			int progress = 100 * (recognitionsPerformed+recognitionsSkipped) / testSize;
			statusLabel.setText("Progress: "+progress+"%");
		}
		catch (Exception ex) {
			ex.printStackTrace();
			profiler.stop();
			exceptions.put(recogNum, ex.toString());
			results.add(new RecogResult(ex, profiler));
			logger.error("recog "+recogNum+" had exception "+ex);
		}
//		System.out.println("Done recog "+recogNum);
	}

	private void printWinList(List<String> winList) {
		StringBuilder sb = new StringBuilder(10);
		for (String win : winList) {
			sb.append(win+" ");
		}
//		System.out.println("Win list is "+sb.toString());
	}

	private void computeResults() {
		for (String key : errorAccumulator.keySet()) {
			ErrorAccumulator accum = errorAccumulator.get(key);
			accum.compute();
		}
	}

	private void accumulate(RecogResult result) {
		Map<String, ReadError> errMap = result.getErrors();
		for (String key : errMap.keySet()) {
			ErrorAccumulator accum = errorAccumulator.get(key);
			if (accum == null) {
				accum = new ErrorAccumulator(key);
				errorAccumulator.put(key, accum);
			}
			accum.add(errMap.get(key));
		}
	}

	private void setup(String db) {
		testSize = selectedDbSize();
		String message = "Start test with database "+db+" size is "+testSize;
		message(message);
//		System.out.println("change button text to Cancel Test");
		button.setText("Cancel Test");
		revalidate();
		repaint();
		testRunning = true;
		testCanceled = false;
		recognitionsPerformed = 0;		
		exceptions = new HashMap<Integer, String>();
		results = new ArrayList<RecogResult>();
		errorAccumulator = new HashMap<String, ErrorAccumulator>();
		auto = new ArrayList<String>();
		auto.add("auto");
		signIDs = dbIf().getSignDB().getSignsIDs();
		instructionLabel.setText(testingMessage);								
	}	

	private void cleanup(String message) {		
		message(message);
//		System.out.println("change button text to Start Test");
		button.setText("Start Test");
		testRunning = false;
		testCanceled = false;
		instructionLabel.setText(instructionMessage);	
	}

	private void displayResults() {
		logger.log("Completed "+recognitionsPerformed+" of "+testSize+" recognitions");
		logger.log(exceptions.size()+" recognitions had exception");
		for ( int i=0 ; i<results.size() ; i++ ) {
			logger.log(results.get(i).toString());
		}
		int elen = ParkingElement.longest.length();
		int tlen = 6;
		String[] headers = getHeaders(elen, tlen);
		for ( int i=0 ; i<headers.length ; i++ ) {
			logger.log(headers[i]);
		}
		StringBuilder sb = null;
		for (ParkingElement element : ParkingElement.values()) {
			String eleString = element.toString();
			sb = new StringBuilder(100);
			sb.append(eleString+getSpace(eleString, elen));
			for (int i=0 ; i<metrics.length ; i++) {
				String metric = metrics[i];
				ErrorAccumulator accum = errorAccumulator.get(metric);
				Map<ParkingElement, Map<ResultType, Double>> errRates = accum.getErrorRates();
				Map<ResultType, Double> err = errRates.get(element);
				if (err != null) {
					for (ResultType type : ResultType.values()) {
						sb.append(asPercent(err.get(type), tlen));
					}
				}
				else {
					sb.append("null"+getSpace("null", tlen));
				}
			}
			logger.log(sb.toString());
		}
		sb = new StringBuilder(100);
		String eleSumName = "Element Sum";
		sb.append(eleSumName+getSpace(eleSumName, elen));
		for (int i=0 ; i<metrics.length ; i++) {
			Map<ResultType, Double> elementSum = errorAccumulator.get(metrics[i]).getElementSum();
			for (ResultType type : ResultType.values()) {
				sb.append(asPercent(elementSum.get(type), tlen));
			}
		}
		logger.log(sb.toString());
	}

	private void printResults() {
		for (ParkingElement element : ParkingElement.values()) {
//			System.out.println("element "+element.toString() );
			for (int i=0 ; i<metrics.length ; i++) {				
				String metric = metrics[i];				
				ErrorAccumulator accum = errorAccumulator.get(metric);
				if (accum != null) {
					Map<ParkingElement, Map<ResultType, Double>> errRates = accum.getErrorRates();
					if (errRates != null ) {
						Map<ResultType, Double> err = errRates.get(element);
						if (err != null ) {
							for (ResultType type : ResultType.values()) {
								if (err.get(type) > 0.0) {
									System.out.println(element.toString()+" "+metrics[i]+" "+type.toString()+" "+err.get(type));
								}
							}
						}
						else {
							System.out.println("no err rates found for element "+element.toString());
						}
					}
					else {
						System.out.println("no err rates found for metric "+metric);
					}
				}
				else {
					System.out.println("no accum found for metric "+metric);
				}
			}
		}
	}

	private String[] getHeaders(int elen, int tlen) {
		String[] headers = new String[2];
		StringBuilder sb = new StringBuilder(10);
		String eleHeader = "Parking Element";
		int numTypes = ResultType.values().length;
		int metricLen = numTypes*tlen;
		int[] metricPos = new int[metrics.length];
		for ( int i=0 ; i<metricPos.length ; i++ ) {
			metricPos[i] = elen + metricLen*(2*i+1)/2 - metrics[i].length()/2;
		}
		sb.append(eleHeader);
		int prev = eleHeader.length();
		for ( int i=0 ; i<metricPos.length ; i++ ) {
			sb.append(getSpace(null, metricPos[i] - prev));
			sb.append(metrics[i]);
			prev = metricPos[i] + metrics[i].length();
		}
		headers[0] = sb.toString();
		sb = new StringBuilder(10);
		for (ResultType type : ResultType.values()) {
			String abrv = ResultType.abbrev.get(type);
			sb.append(getSpace(abrv, tlen-1));
			sb.append(abrv);
		}
		String resTypeHdr = sb.toString();
		sb = new StringBuilder(10);
		sb.append(getSpace(null, elen));
		for ( int i=0 ; i<metricPos.length ; i++ ) {
			sb.append(resTypeHdr);
		}
		headers[1] = sb.toString();
		return headers;
	}

	private String asPercent(double value, int max) {
		String format = "%"+max+".2f";
		return String.format(format,100*value);
	}

	private String getSpace(String s, int max) {
		StringBuilder sb = new StringBuilder(10);
		int diff = s == null ? max : max - s.length();
		for ( int i=0 ; i<diff+1 ; i++ ) {
			sb.append(" ");
		}
		return sb.toString();
	}

	private void message(String message) {
		logger.log( message);
		statusLabel.setText( message );
	}

	//
	//  frame with 3 panels: instruction, select (radio buttons), status
	//
	private void initDisplay() {
		SignTester tester = this;
		setLayout(new GridLayout(4, 1));
      	setSize(400, 400);
      	setVisible(true); 

      	instructionMessage = "Select the database to test from.";
		testingMessage = "Test in progress...";
      	instructionPanel = new JPanel(new FlowLayout());
      	instructionLabel = new JLabel(instructionMessage);
      	instructionPanel.add(instructionLabel);
      	add(instructionPanel);          
      	add(getSelectPanel());
      	controlPanel = new JPanel(new FlowLayout());
      	button = new JButton("Start Test");
      	button.addActionListener(new ActionListener() {
         	public void actionPerformed(ActionEvent e) {
         		if (testRunning == false) {
         			if (selectedDB() == null) {
         				statusLabel.setText("No database selected." );
         			} 	
         			else {	
            			setup(selectedDB());
            			testWorker = new TestWorker(tester);
            			testWorker.execute();
            		}
            	}
            	else {
            		testCanceled = true;
            	}            	
         	}          
      	});      	
      	controlPanel.add(button);
      	add(controlPanel);

      	statusPanel = new JPanel(new FlowLayout());            	
      	statusLabel = new JLabel();
      	statusPanel.add(statusLabel);
      	add(statusPanel);    	
	}
}
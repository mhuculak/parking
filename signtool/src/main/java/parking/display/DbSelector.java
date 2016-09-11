package parking.display;

import parking.database.MongoInterface;
import parking.util.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import java.awt.FlowLayout;

import java.util.Map;
import java.util.HashMap;

public class DbSelector extends JFrame {
	private Map<String, MongoInterface> dbIf;
	private Map<String, Integer> dbSize;
	private String selectedDB;
	private JPanel selectPanel;
	private boolean useAll;
	private final String[] databaseNames = { "parking_test", "demo_test", "test_test" };       //  writable
	private final String[] allDatabaseNames = { "parking", "demo", "test", "parking_test", "demo_test", "test_test" };    // readable

	public DbSelector(Logger logger, String title, boolean useAll) {
		super(title);
		this.useAll = useAll;
		initDatabase(logger);
		initDisplay();
	}

	public String[] getDbNames() {
		return databaseNames;
	}

	public String[] getAllDbNames() {
		return allDatabaseNames;
	}

	public void selectDB(String db) {
		selectedDB = db;
		dbIf.get(selectedDB);
	}

	public String selectedDB() {
		return selectedDB;
	}

	public MongoInterface dbIf() {
		return dbIf.get(selectedDB);
	}

	public JPanel getSelectPanel() {
		return selectPanel;
	}
/*
	public Map<String, Integer> dbSize() {
		return dbSize;
	}
*/
	public int selectedDbSize() {
		return dbSize.get(selectedDB);
	}

	private void initDisplay() {
		selectPanel = new JPanel();
      	selectPanel.setBounds(61, 11, 81, 140);
        selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.Y_AXIS));
        ButtonGroup buttonGroup = new ButtonGroup();
      	for ( String db : dbSize.keySet()) {
      		JPanel buttonPanel = new JPanel(new FlowLayout());
      		JLabel buttonLabel =  new JLabel(db+" "+dbSize.get(db)+" signs");
      		buttonPanel.add(buttonLabel);
      		JRadioButton button = new JRadioButton(new DatabaseSelect(this, db));      		
      		buttonPanel.add(button);
      		buttonGroup.add(button);
      		selectPanel.add( buttonPanel );     		
      	}
      	
	}

	private void initDatabase(Logger logger) {
		dbIf = new HashMap<String, MongoInterface>();
		dbSize = new HashMap<String, Integer>();
		String[] dbNames = useAll ? allDatabaseNames : databaseNames;
		for ( int i=0 ; i<dbNames.length ; i++) {
			String db = dbNames[i];
			MongoInterface mongo = MongoInterface.getInstance(db, logger);
			int size = mongo.getSignDB().getSize();
			dbSize.put(db, size);
			dbIf.put(db, mongo);
		}
		selectedDB = null;
	}
}
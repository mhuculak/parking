package parking.display;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class DatabaseSelect extends AbstractAction {

	private String db;
	private DbSelector selector;

	public DatabaseSelect(DbSelector selector, String db) {
		this.db = db;
		this.selector = selector;
	}

	@Override
    public void actionPerformed(ActionEvent e) {
        selector.selectDB(db);
    }

}
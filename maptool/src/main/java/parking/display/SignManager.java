package parking.display;

import parking.map.Sign;

import javax.swing.SwingWorker;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class SignManager {

	private MapTool parent;
	private Map<String, Sign> signs;

	public SignManager(MapTool parent) {
		this.parent = parent;
	}

	public void fetch(String url) {

	}

	public void dataAvailable() {
		parent.overlaySigns(signs);
	}
}

class SignFetcher extends SwingWorker<Integer, Integer> {

	private SignManager signManager;
	private String url;

	public SignFetcher(SignManager signManager, String url) {
		this.signManager = signManager;
		this.url = url;
	}

	protected Integer doInBackground() throws Exception {
		signManager.fetch(url);
		return 1;
	}

	protected void done() {
		signManager.dataAvailable();
	}
}
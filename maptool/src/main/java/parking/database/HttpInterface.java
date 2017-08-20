package parking.database;

//import parking.display.DisplayMap;
import parking.map.SignMarker;
import parking.map.MapBounds;
import parking.security.User;
import parking.util.HttpDownloader;

import javax.swing.SwingWorker;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

//
//  Asynchronous interface used to access the DB using HTTP
//
//  Note: DatabaseInterface is designed for both synchronous and
//        asynchronous tinterfaces
//

enum CallbackType {
		SignMarkers,
}

public class HttpInterface implements DatabaseInterface {

	private Integer port;
	private User user;
	private String host;
	private String urlBase;
	private ObjectMapper mapper = new ObjectMapper();

	

	public HttpInterface(Integer port, String host, User user) {
		this.port = port;
		this.user = user;
		if (host == null) {
			this.host = "localhost";
		}
		else {
			this.host = host;
		}
		urlBase = "http://"+this.host+":"+port;
	}

	public List<SignMarker> getSignMarkers(MapBounds bounds, DBcallback callback) {
		if (callback != null) {
			double neLat = bounds.getNE().getLatitude();
			double neLng = bounds.getNE().getLongitude();
			double swLat = bounds.getSW().getLatitude();
			double swLng = bounds.getSW().getLongitude();
			String url = urlBase+"/parking/main/signs?nela="+neLat+"&nelg="+neLng+"&swla="+swLat+"&swlg="+swLng;
			HttpWorker worker = new HttpWorker(this, url, user, callback, CallbackType.SignMarkers);
        	worker.execute();			
		}
		return null; // result returned by asynch callback
	}

	public List<SignMarker> parseSignMarkers(String response) {
		SignMarker[] markerArray = null;
		try {
			markerArray = mapper.readValue(response, SignMarker[].class);
		}
		catch (Exception ex) {
			System.out.println("Caught exception "+ex);
//			System.out.println(response);

		}
		List<SignMarker> mlist = Arrays.asList(markerArray);

        return mlist;
	}
}

class HttpWorker extends SwingWorker<Integer, Integer> {

	private String url;
	private User user;
	private DBcallback callback;
	private HttpInterface parent;
	private CallbackType type;
	private HttpDownloader downloader;

	public HttpWorker(HttpInterface parent, String url, User user, DBcallback callback, CallbackType type) {
		this.url = url;
		this.user = user;
		this.callback = callback;
		this.type = type;
		this.parent = parent;
		downloader = new HttpDownloader(user, "http-interface");
	}

	protected Integer doInBackground() throws Exception {
//		System.out.println("fetching "+url);			 
		downloader.start(url);
		downloader.waitForResponse();		
		return 1;
	}

	protected void done() {
		switch (type) {
			case SignMarkers: 
				String response = downloader.getResponse();
				if (response == null) {
					System.out.println("No response because "+downloader.getErrorMessage());
				}
				else { 
					List<SignMarker> mkrs = parent.parseSignMarkers(response);
					callback.setSignMarkers(mkrs);
				}
				break;
			default:
		}
	}
}
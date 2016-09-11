package parking.util;

import java.util.Map;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class HttpClient {

	private HttpURLConnection connection;

	public HttpResponse doGet(String urls) throws IOException {
		
		URL url = new URL(urls);
		connection = (HttpURLConnection) url.openConnection();			
		connection.setRequestMethod("GET");
		return new HttpResponse(connection);
		
	}

	public HttpResponse doPost(String urls, Map<String, String> postData, Map<String, String> headers) throws IOException {
		
		URL url = new URL(urls);
		connection = (HttpURLConnection) url.openConnection();
		if (headers != null) {
			for (String header : headers.keySet()) {
				connection.setRequestProperty(header, headers.get(header));
        	}
        }
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		PrintWriter out = new PrintWriter(connection.getOutputStream());
		StringBuilder sb = new StringBuilder(10);
		for (Map.Entry<String, String> entry : postData.entrySet()) {
			String keyValue = entry.getKey()+"="+URLEncoder.encode(entry.getValue(), "UTF-8");      		
      		sb.append(keyValue+"&");
      	}
      	out.println(sb.toString());
      	out.close();
      	return new HttpResponse(connection);
      	
	}
/*
	public HttpResponse doPost(String urls, Map<String, String> postData, Map<String, String> headers, File file, String mediatType) throws IOException {
		
		URL url = new URL(urls);
		connection = (HttpURLConnection) url.openConnection();
		if (headers != null) {
			for (String header : headers.keySet()) {
				connection.setRequestProperty(header, headers.get(header));
        	}
        }
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		DataOutputStream outputStream = DataOutputStream(connection.getOutputStream());
      	
      	return new HttpResponse(connection);
      	
	}
*/	
}
package parking.util;

import java.util.*;
import java.io.*;
import java.net.*;

public class HttpClient {

	private HttpURLConnection connection;
	private InputStream inputStream;	

	public void doGet(String urls) {
		try {
			URL url = new URL(urls);
			connection = (HttpURLConnection) url.openConnection();			
			connection.setRequestMethod("GET");
			inputStream = connection.getInputStream();
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
	}

	public void doPost(String urls, Map<String, String> postData) {
		try {
			URL url = new URL(urls);
			connection = (HttpURLConnection) url.openConnection();	
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			PrintWriter out = new PrintWriter(connection.getOutputStream());
			for (Map.Entry<String, String> entry : postData.entrySet()) {
				String keyValue = entry.getKey()+URLEncoder.encode(entry.getValue(), "UTF-8");      		
      			out.println(keyValue+"&");
      		}
      		out.close();
      		inputStream = connection.getInputStream();
      	}
      	catch (Exception e)	{
			e.printStackTrace();
		}
	}

	public InputStream getInputStream() {
		return inputStream;
	}
}
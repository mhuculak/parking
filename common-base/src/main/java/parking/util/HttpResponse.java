package parking.util;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.io.IOException;

public class HttpResponse {
	private int httpStatus;
	private String body;

	public HttpResponse(HttpURLConnection connection) throws IOException {
		
		
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder(100);
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        in.close();
       	body = sb.toString();
        httpStatus = connection.getResponseCode();
	}

	public int getStatus() {
		return httpStatus;
	}

	public String getBody() {
		return body;
	}
}
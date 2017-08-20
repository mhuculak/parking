package parking.util;

import parking.security.User;

import com.squareup.okhttp.ResponseBody;
import okhttp3.OkHttpClient;
import okhttp3.Callback;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Headers;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Base64;

public class HttpDownloader {

	private User user;
    private String userAgent;
	private Response response;
	private int httpResponseCode;
    private String errorMessage;
    private String responseBody;

	public HttpDownloader(User user) {
		this.user = user;
	}

    public HttpDownloader(User user, String userAgent) {
        this.user = user;
        this.userAgent = userAgent;
    }

	public void start(String url) {
		try {
            OkHttpClient httpClient = new OkHttpClient();            
            Request httpRequest = null;
            Request.Builder builder = new Request.Builder().url(url);
            if (user != null) {
                String userPass = user.getUserName()+":"+user.getPassword();
//                System.out.println("encode "+userPass);
                String  encodedUserPass = Base64.getEncoder().encodeToString(userPass.getBytes());
//                System.out.println("encode "+encodedUserPass);
                builder.addHeader("Authorization", "Basic "+encodedUserPass);
            }
            if (userAgent != null) {
                builder.addHeader("User-Agent", userAgent);
            } 
            httpRequest = builder.build();
            HttpDownloader downloader = this;
            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
 //                       System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }
                    downloader.responseBody = response.body().string();
                    downloader.response = response;                              
                }
            });         
            
        } catch (Exception e) {
            errorMessage = e.toString();
            e.printStackTrace();
        }
	}

    public void waitForResponse() {
//        System.out.println("Waiting for http response...");
        while (response == null) {
            try {
                Thread.sleep(100);
            } 
            catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
//        System.out.println("Got http response");
    }

	public String getResponse() {
//        System.out.println("Get http response");
        try {
            if (responseBody != null) {
//                System.out.println("Return response body");
                return responseBody;
            }
            if (response != null) {
//                System.out.println("Read response");
		        return readResponse(response);
            }
            else {                    
                return null;
            }
        }
        catch (IOException ex) {
            return ex.toString();
        }            
	}

	public String getErrorMessage() {
        if (errorMessage == null) {
            if (response != null) {
		        return response.message();
            }
            return null;
        } 
        return errorMessage;
	}

	public int getResponseCode() {
		return response.code();
	}

    public InputStream getInputStream() {        
        okhttp3.ResponseBody body = response.body();
        return body.byteStream();          
    }

	private String readResponse(Response response) throws IOException {
                
        if (response.isSuccessful()) {
            okhttp3.ResponseBody body = response.body();
            long len = body.contentLength();            
            if (len > 0 ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()));
                StringBuilder sb = new StringBuilder(100);
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();                
                return sb.toString();
            }
//            System.out.println("reponse len is "+len+" code = "+response.code()+" message = "+response.message());
            return "";
        }
        System.out.println("response not successful");
        return null;
    }
}
package parking.parkingapp;

import parking.security.User;

import android.os.AsyncTask;
import com.squareup.okhttp.ResponseBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DownloadClient extends AsyncTask<String, Integer, Boolean> {

    private User user;
    private int httpResponseCode;
    private String downloadResponse;
    private String errorMessage;
    private MapsActivity parent;
    protected static final String TAG = "parking";

    public DownloadClient(MapsActivity parent, User user) {
        this.user = user;
        this.parent = parent;
        Log.i(TAG, "phone is "+user.getPhone());
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... urls) {
        try {
            Log.i(TAG, "HTTP Get "+urls[0]);
            OkHttpClient httpClient = new OkHttpClient();
            String userPass = user.getUserName()+":"+user.getPassword();
            String  encodedUserPass = Base64.encodeToString(userPass.getBytes(), Base64.NO_WRAP);
            Request httpRequest = new Request.Builder()
                    .url(urls[0])
                    .addHeader("Authorization", "Basic "+encodedUserPass)
                    .addHeader("Phone", user.getPhone())
                    .build();
            Response response = httpClient.newCall(httpRequest).execute();
            downloadResponse = readResponse(response);
            errorMessage = response.message();
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "exception", e);
            errorMessage = e.toString();
        }
        return false;
    }

    protected void onProgressUpdate(Integer... progress) {
//        setProgressPercent(progress[0]);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (parent != null) {
            if (success) {
                Log.i(TAG, "pass result to main activity");
                parent.downloadResultAvailable(downloadResponse);
            }
            else {
                Log.i(TAG, "pass failure "+errorMessage+" to main activity response code = "+httpResponseCode);
                parent.downloadConnectionFailure(errorMessage, httpResponseCode);
            }
        }
    }

    private String readResponse(Response response) throws IOException {
        httpResponseCode = response.code();
        Log.i(TAG, "response code is "+httpResponseCode);
        if (response.isSuccessful()) {
            Log.i(TAG,"succesful upload "+response.message());
            okhttp3.ResponseBody body = response.body();
            long len = body.contentLength();
            Log.i(TAG, "got "+len+" byte response");
            if (len > 0 ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()));
                StringBuilder sb = new StringBuilder(100);
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                Log.i(TAG, "got response " + sb.toString());
                return sb.toString();
            }
            return "";
        }
        return null;
    }
}

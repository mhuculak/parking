package parking.parkingapp;

import android.os.AsyncTask;
import com.squareup.okhttp.ResponseBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import android.util.Log;

public class DownloadClient extends AsyncTask<String, Integer, Boolean> {

    protected static final String TAG = "location-updates-sample";

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }

    @Override
    protected Boolean doInBackground(String... urls) {
        try {
            OkHttpClient httpClient = new OkHttpClient();
            Request httpRequest = new Request.Builder().url(urls[0]).build();
            Log.i(TAG,"fetching "+urls[0]);
            Response response = httpClient.newCall(httpRequest).execute();
            if (response.isSuccessful()) {
                String jsonData = response.body().string();
                JSONObject Jobject = new JSONObject(jsonData);
                return true;
            }

        } catch (Exception e) {

        }
        return false;
    }

    protected void onProgressUpdate(Integer... progress) {
//        setProgressPercent(progress[0]);
    }


    protected void onPostExecute(String result) {

    }

}

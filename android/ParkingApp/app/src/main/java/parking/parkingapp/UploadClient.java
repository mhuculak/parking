package parking.parkingapp;

import parking.schedule.SignSchedule;
import parking.security.User;
import parking.map.Trajectory;

import android.os.AsyncTask;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.CookieJar;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Authenticator;
import okhttp3.Route;
import okhttp3.Credentials;
import okhttp3.Headers;

import org.json.JSONObject;
import android.util.Log;
import java.io.File;
import android.net.Uri;
import android.content.ContentResolver;
import android.content.Context;
import android.webkit.MimeTypeMap;

import android.util.Base64;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UploadClient extends AsyncTask<String, Integer, Boolean> {
    protected static final String TAG = "parking";

    private File uploadFile;
    private SignSchedule signSchedule;
    private Trajectory trajectory;
    double startTime;
    double endTime;
    String signID;
    private MediaType mediaType;
    private User user;
    private OkHttpClient httpClient;
    private MapsActivity parent;
    private Register register;
    private String uploadResponse;
    private String errorMessage;
    private int httpResponseCode;

    public UploadClient(File uploadFile, MapsActivity parent, User user) {
        this.uploadFile = uploadFile;
        this.user = user;
        this.parent = parent;
        httpClient = createHttpClient(user, 120);
        mediaType = MediaType.parse("image/jpeg"); // FIXME
    }

    public UploadClient(SignSchedule schedule, MapsActivity parent, User user) {
        this.signSchedule = schedule;
        this.user = user;
        this.parent = parent;
        httpClient = createHttpClient(user, 10);
    }

    public UploadClient(Trajectory trajectory, String signID, double startTime, double endTime, MapsActivity parent, User user) {
        this.trajectory = trajectory;
        this.startTime = startTime;
        this.endTime = endTime;
        this.signID = signID;
        this.user = user;
        this.parent = parent;
        httpClient = createHttpClient(user, 10);
    }

    public UploadClient(User user, Register register) {
        this.user = user;
        this.register = register;
    }

    private OkHttpClient createNoAuthClient(int timeout) {
        Log.i(TAG, "create retry client with "+timeout+" sec timeout");
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
        return httpClient;
    }

    private OkHttpClient createHttpClient(User user, int timeout) {
        final User lUser = user;
        Log.i(TAG, "create HTTP client with "+timeout+" sec timeout");
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) {
                        System.out.println("Authenticating for response: " + response);
                        System.out.println("Challenges: " + response.challenges());
                        String credential = Credentials.basic(lUser.getUserName(), lUser.getPassword());
                        return response.request().newBuilder()
                                .header("Authorization", credential)
                                .build();
                    }
                })
                .build();
        return httpClient;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... urls) {
        if (uploadFile != null) {
            return uploadFile(urls[0]);
        }
        else if (signSchedule != null){
            return uploadSchedule(urls[0]);
        }
        else if (trajectory != null) {
            return uploadTrajectory(urls[0]);
        }
        else if (register != null) {
            return uploadRegistration(urls[0]);
        }
        return false;
    }

    private boolean uploadTrajectory(String url) {
        try {
            JSONObject jObj = trajectory.serialize();
            String json = jObj.toString();
            Log.i(TAG, "uploading trajectory " + json+ " to "+url);
            RequestBody formBody = new FormBody.Builder()
                    .add("trajectory", json)
                    .add("signID", signID)
                    .add("start", Double.toString(startTime))
                    .add("end", Double.toString(endTime))
                    .build();
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("Phone", user.getPhone())
                    .build();
            Response response = httpClient.newCall(httpRequest).execute();
            uploadResponse = readResponse(response);
            if (uploadResponse != null) {
                return true;
            }
            Log.e(TAG,"failure response = "+response.message());
            errorMessage = response.message();
        } catch (Exception ex) {
            errorMessage = ex.toString();
            Log.e(TAG, "exception", ex);
        }
        return false;
    }
    private boolean uploadSchedule(String url) {
        try {
            JSONObject jObj = signSchedule.serialize();
            String json = jObj.toString();
            Log.i(TAG, "uploading schedule " + json+ " to "+url);
            RequestBody formBody = new FormBody.Builder()
                    .add("verified", json)
                    .add("signID", signSchedule.id)
                            .build();
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("Phone", user.getPhone())
                    .build();
            Response response = httpClient.newCall(httpRequest).execute();
            Headers responseHeaders = response.headers();
            for (int i = 0; i < responseHeaders.size(); i++) {
                Log.i(TAG, responseHeaders.name(i) + ": " + responseHeaders.value(i));
            }
            uploadResponse = readResponse(response);
            if (uploadResponse != null) {
                return true;
            }
            Log.e(TAG,"failure response = "+response.message());
            errorMessage = response.message();
        } catch (Exception ex) {
            errorMessage = ex.toString();
            Log.e(TAG, "exception", ex);
        }
        return false;
    }

    private boolean uploadRegistration(String url) {
        try {
            OkHttpClient registrationClient = createNoAuthClient(10);
            JSONObject jObj = user.serialize();
            String json = jObj.toString();
            Log.i(TAG, "uploading registration info " + json+ " to "+url);
            RequestBody formBody = new FormBody.Builder()
                    .add("register", json)
                    .build();
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();
            Response response = registrationClient.newCall(httpRequest).execute();
            uploadResponse = readResponse(response);
            if (uploadResponse != null) {
                return true;
            }
            Log.e(TAG,"failure response = "+response.message());
            errorMessage = response.message();
        } catch (Exception ex) {
            errorMessage = ex.toString();
            Log.e(TAG, "exception", ex);
        }
        return false;
    }

    private String readResponse(Response response) throws IOException {
        httpResponseCode = response.code();
        if (response.isSuccessful()) {
            Log.i(TAG,"succesful upload "+response.message());
            ResponseBody body = response.body();
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

    private boolean uploadFile(String url) {
        MultipartBody multipart = null;
        Request httpRequest = null;
        try {  // FIXME: probably need to either:
               //      a) increase retry timeout OR
               //      b) use an asych protocol

            multipart = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", uploadFile.getName(), RequestBody.create(mediaType, uploadFile)).build();

            httpRequest = new Request.Builder()
                    .url(url)
                    .post(multipart)
                    .addHeader("Phone", user.getPhone())
                    .build();

            Log.i(TAG,"fetching "+url);
            Response response = httpClient.newCall(httpRequest).execute();
            uploadResponse = readResponse(response);
            if (uploadResponse != null) {
                return true;
            }
            Log.e(TAG,"failure response = "+response.message());

        } catch (Exception e) {
            Log.e(TAG, "Caught excpetion "+e+" attempting to post file "+uploadFile.getPath()+" to "+url);
            Log.e(TAG, "exception", e);
            try { // if fist you don't succeed
                OkHttpClient retryClient = createNoAuthClient(120);
                String userPass = user.getUserName()+":"+user.getPassword();
                String  encodedUserPass = Base64.encodeToString(userPass.getBytes(), Base64.NO_WRAP);
                httpRequest = new Request.Builder()
                        .url(url)
                        .post(multipart)
                        .addHeader("Authorization", "Basic "+encodedUserPass)
                        .addHeader("Phone", user.getPhone())
                        .build();
                Response response = retryClient.newCall(httpRequest).execute();
                uploadResponse = readResponse(response);
                if (uploadResponse != null) {
                    return true;
                }
                Log.e(TAG,"retry failure response = "+response.message());
                errorMessage = response.message();
            }
            catch (Exception ex) {
                errorMessage = ex.toString();
                Log.e(TAG, "Caught excpetion "+ex+" on retry...giving up");
                Log.e(TAG, "exception", ex);
            }
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
                parent.uploadResultAvailable(uploadResponse);
            }
            else {
                parent.uploadConnectionFailure(errorMessage, httpResponseCode);
            }
        }
        else if (register != null) {
            if (success) {
                Log.i(TAG, "pass result to the registration fragment");
                register.uploadResultAvailable(uploadResponse);
            }
            else {
                register.uploadConnectionFailure(errorMessage, httpResponseCode);
            }
        }
    }
}

package parking.parkingapp;

import parking.security.User;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
// import android.support.v4.app.Fragment;
import android.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Button;

public class Register extends Fragment {

    private User user;
    private String phoneNumber;
    private String registrationUrl;
    private TextView statusText;
    private CustomerRegistered mListener;
    private final String TAG = "parking";

    public Register(String phoneNumber, String url) {
        registrationUrl = url;
        this.phoneNumber = phoneNumber;
    }

    public static Register newInstance(String phone, String url) {
        Register fragment = new Register(phone, url);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register,
                container, false);
        Button submitButton = (Button) view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        return view;
    }

    public void submit() {
        statusText = (TextView)getView().findViewById(R.id.status_message);
        EditText userNameText = (EditText)getView().findViewById(R.id.user_name_value);
        EditText passwordText = (EditText)getView().findViewById(R.id.password_value);
        EditText retypePasswordText = (EditText)getView().findViewById(R.id.retype_password_value);
        EditText emailText = (EditText)getView().findViewById(R.id.email_value);
        EditText addressText = (EditText)getView().findViewById(R.id.home_address_value);
        String password = passwordText.getText().toString();
        String retypePassword = retypePasswordText.getText().toString();
        if (!password.equals(retypePassword)) {
            statusText.setText("Passwords do not match");
            return;
        }
        user = new User(userNameText.getText().toString(), password.toCharArray(), phoneNumber, emailText.getText().toString(), addressText.getText().toString());
        UploadClient uploadClient = new UploadClient(user, this);
        uploadClient.execute(registrationUrl);
    }

    public void uploadResultAvailable(String resultData) {
        if (resultData == null || resultData.length() == 0) {
            Log.i(TAG, "customer sucessfully registered...");
            mListener.customerRegistered(user);
            return;
        }
        statusText.setText(resultData); // reason why server did not accept registration
    }

    public void uploadConnectionFailure(String message, int httpResponseCode) {
        statusText.setText(message);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CustomerRegistered) {
            mListener = (CustomerRegistered) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface CustomerRegistered {
        void customerRegistered(User user);
    }
}

package parking.parkingapp;

import parking.map.Position;
import parking.map.Address;
import parking.map.Place;
import parking.schedule.WeekDay;
import parking.schedule.SignSchedule;
import parking.schedule.ParkingSchedule;
import parking.schedule.TimeRange;
import parking.schedule.SimpleTime;
import parking.schedule.WeekDaySet;
import parking.schedule.DateRange;
import parking.schedule.SimpleDate;
import parking.schedule.ParkingSignType;
import parking.schedule.SimpleScheduleForJson;
import parking.util.Utils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.ImageButton;
import android.support.v7.app.AppCompatActivity;
import android.provider.MediaStore;
import android.content.Intent;

import java.util.Map;
import java.util.HashMap;

public class SignVerifier extends AppCompatActivity {

    private SignSchedule sign;
    private Uri signPictureUri;
    private RadioButton parkingButton;
    private RadioButton noParkingButton;
    private EditText timeLimit;
    private CheckBox noTimeRange;
    private CheckBox noDateRange;
    private CheckBox noWeekDays;
    private Place signPlace;
    private double bestAccuracy;
    private LinearLayout signTypeLayout;
    private LinearLayout timeLimitLayout;
    private LinearLayout timeRangeLayout;
    private LinearLayout dateRangeLayout;
    private LinearLayout weekDaysLayout;
    private LinearLayout addressLayout;
    private LinearLayout buttonLayout;
    private TextView startTime;
    private TextView endTime;
    private TextView startDate;
    private TextView endDate;
    private TextView warningMessage;
    private ImageView fullImage;
    private ImageView backgroundImage;
    private Map<String, String> origAddress;
    private Map<String, Integer> addressID;
    private String[] addressKeys = { "streetNum", "streetName", "city" };
    private final double maxDistanceKm = 0.1;
    private final double minAccuracyThreshold = 20;

    protected static final String TAG = "parking";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate SignVerifier");

        setContentView(R.layout.activity_verify);

        parkingButton = (RadioButton)findViewById(R.id.radio_parking);
        noParkingButton = (RadioButton)findViewById(R.id.radio_no_parking);
        timeLimit = (EditText)findViewById(R.id.time_limit_value);
        noTimeRange = (CheckBox)findViewById(R.id.no_time_range);
        noDateRange = (CheckBox)findViewById(R.id.no_date_range);
        noWeekDays = (CheckBox)findViewById(R.id.no_week_days);
        signTypeLayout = (LinearLayout)findViewById(R.id.sign_type);
        timeLimitLayout = (LinearLayout)findViewById(R.id.time_limit);
        timeRangeLayout = (LinearLayout)findViewById(R.id.time_range);
        dateRangeLayout = (LinearLayout)findViewById(R.id.date_range);
        weekDaysLayout = (LinearLayout)findViewById(R.id.week_days);
        addressLayout = (LinearLayout)findViewById(R.id.address);
        buttonLayout = (LinearLayout)findViewById(R.id.buttons);
        startTime = (TextView)findViewById(R.id.start_time_text);
        endTime = (TextView)findViewById(R.id.end_time_text);
        startDate = (TextView)findViewById(R.id.start_date_text);
        endDate = (TextView)findViewById(R.id.end_date_text);
        fullImage = (ImageView)findViewById(R.id.sign_full_image);
        backgroundImage = (ImageView)findViewById(R.id.background_image);
        warningMessage = (TextView)findViewById(R.id.warning_message);
        addressID = new HashMap<String, Integer>();
        addressID.put("streetNum", R.id.street_number);
        addressID.put("streetName", R.id.street_name);
        addressID.put("city", R.id.city_value);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                try {
                    sign = (SignSchedule) extras.getSerializable("schedule");
                    signPictureUri = (Uri) extras.getParcelable("image");
                    signPlace = (Place)extras.getSerializable("place");
                    bestAccuracy = (Double)extras.getSerializable("bestAccuracy");
                    if (signPlace != null) {
                        Log.i(TAG, "place from phone is " + signPlace.shortAddress());
                    }
                    setup();
                }
                catch (Exception ex) {
                    Log.e(TAG, "exception", ex);
                }

            }
        }

        Log.i(TAG, "onCreate SignVerifier done");
    }

    public void showImage(View view) {
        Log.i(TAG,"show full sized image");
//        thumbNailer.setVisibility(View.GONE);
        fullImage.setVisibility(View.VISIBLE);
    }

    public void hideImage(View view) {
        Log.i(TAG, "hide full sized image");
//        thumbNailer.setVisibility(View.VISIBLE);
        fullImage.setVisibility(View.GONE);
    }

    public void cursorToEnd(View view) {
        EditText et = (EditText)view;
        int len = et.getText().toString().length();
        Log.i(TAG, "set cursor to len");
        et.setSelection(len);
    }

    public void toggleTimeRange(View view) {
        if (noTimeRange.isChecked()) {
            timeRangeLayout.setVisibility(View.GONE);
        }
        else {
            timeRangeLayout.setVisibility(View.VISIBLE);
        }
    }

    public void toggleDateRange(View view) {
        if (noDateRange.isChecked()) {
            dateRangeLayout.setVisibility(View.GONE);
        }
        else {
            dateRangeLayout.setVisibility(View.VISIBLE);
        }
    }

    public void toggleWeekDays(View view) {
        if (noWeekDays.isChecked()) {
            weekDaysLayout.setVisibility(View.GONE);
        }
        else {
            weekDaysLayout.setVisibility(View.VISIBLE);
        }
    }
    public void setStartTime(View view) {
        DialogFragment timeFragment = new TimeSelector(startTime);
        timeFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void setEndTime(View view) {
        DialogFragment timeFragment = new TimeSelector(endTime);
        timeFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void setStartDate(View view) {
        DialogFragment timeFragment = new DateSelector(startDate);
        timeFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void setEndDate(View view) {
        DialogFragment timeFragment = new DateSelector(endDate);
        timeFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void submit(View view) {
        Log.i(TAG, "Submit: get schedule");
        SignSchedule verifiedSign = null;
        try {
            ParkingSchedule verifiedSchedule = getSchedule();
            Log.i(TAG, "Submit: get sign");
            verifiedSign = getSign(verifiedSchedule);
            Log.i(TAG, "Submit: return result");
        }
        catch (Exception ex) {
            Log.e(TAG, "exception", ex);
        }
        Intent intent = new Intent(this, MapsActivity.class);
        if (verifiedSign != null) {
            intent.putExtra("verified", verifiedSign);
        }
        startActivity(intent);
    }

    private ParkingSchedule getSchedule() {
        boolean changeSchedule = false;
        ParkingSchedule verifiedSchedule = new ParkingSchedule();
        verifiedSchedule.setRestriction(noParkingButton.isChecked());
        if (sign.schedule == null || verifiedSchedule.isRestricted() != sign.schedule.isRestriction) {
            Log.i(TAG, "found sign type change");
            changeSchedule = true;
        }
        if (!noTimeRange.isChecked()) {
            Log.i(TAG, "getSchedule: get time range");
            parking.schedule.SimpleTime start = new SimpleTime(startTime.getText().toString());
            parking.schedule.SimpleTime end = new SimpleTime(endTime.getText().toString());
            parking.schedule.TimeRange timeRange = new TimeRange(start, end);
            verifiedSchedule.setTimeRange(timeRange);
            if (sign.schedule == null || sign.schedule.timeRange == null) {
                Log.i(TAG, "getSchedule: found time range added");
                changeSchedule = true;
            } else if (!timeRange.getStart().equals(sign.schedule.timeRange.start) ||
                    !timeRange.getEnd().equals(sign.schedule.timeRange.end)) {
                changeSchedule = true;
                Log.i(TAG, "getSchedule: found time range modified");
            }
        } else if (sign.schedule != null && sign.schedule.timeRange != null) {
            Log.i(TAG, "getSchedule: found time range removed");
            changeSchedule = true;
        }
        if (!verifiedSchedule.isRestricted()) {
            Log.i(TAG, "parking allowed...check for time limit");
            if (timeLimit != null) {
                Log.i(TAG, "timeLimit defined...");
                if (timeLimit.getText() != null) {
                    Log.i(TAG, "timeLimit.getText() defined...");
                    Log.i(TAG, "getSchedule: set time limit to " + timeLimit.getText().toString());
                    verifiedSchedule.setTimeLimitMinutes(Utils.parseInt(timeLimit.getText().toString()));
                }
            }
            else {
                Log.i(TAG, "getSchedule: timelimit is not available");
            }
        }
        Log.i(TAG, "check for time limit change");
        if (sign.schedule == null || verifiedSchedule.getTimeLimitMinutes() != sign.schedule.timeLimitMinutes) {
            Log.i(TAG, "found time limit change");
            changeSchedule = true;
        }
        if (!noWeekDays.isChecked()) {
            Log.i(TAG, "getSchedule: get week days");
            WeekDaySet weekDaySet = new WeekDaySet();
            for (WeekDay day : WeekDay.values()) {
                CheckBox checkBox = getWeekdayBox(day);
                if (checkBox != null && checkBox.isChecked()) {
                    weekDaySet.add(day);
                }
            }
            if (weekDaySet.size() > 0) {
                verifiedSchedule.setWeekDays(weekDaySet);
            }
            if (sign.schedule == null || sign.schedule.weekDays == null) {
                Log.i(TAG, "found week day set added");
                changeSchedule = true;
            } else if (sign.schedule.weekDays.weekDaySet.size() != weekDaySet.size()) {
                Log.i(TAG, "found week day set size change");
                changeSchedule = true;
            } else {
                for (int i = 0; i < weekDaySet.size(); i++) {
                    if (weekDaySet.get(i) != sign.schedule.weekDays.weekDaySet.get(i).toString()) {
                        Log.i(TAG, "found week days modified");
                        changeSchedule = true;
                    }
                }
            }
        }
        else if (sign.schedule != null && sign.schedule.weekDays != null) {
            Log.i(TAG, "found week day set removed");
            changeSchedule = true;
        }
        if (!noDateRange.isChecked()) {
            Log.i(TAG, "getSchedule: get date range");
            String[] sd = startDate.getText().toString().split(" ");
            String[] ed = endDate.getText().toString().split(" ");
            DateRange dateRange = new DateRange(new SimpleDate(sd[0], sd[1]), new SimpleDate(ed[0], ed[1]));
            verifiedSchedule.setDateRange(dateRange);
            if (sign.schedule == null || sign.schedule.dateRange == null) {
                Log.i(TAG, "getSchedule: found date range added");
                changeSchedule = true;
            } else if (!dateRange.getStart().equals(sign.schedule.dateRange.start) ||
                    !dateRange.getEnd().equals(sign.schedule.dateRange.end)) {
                Log.i(TAG, "getSchedule: found date range modified");
                changeSchedule = true;
            }
        } else if (sign.schedule != null && sign.schedule.dateRange != null) {
            Log.i(TAG, "getSchedule: found date range removed");
            changeSchedule = true;
        }
        Log.i(TAG, "getSchedule: set flags");
        verifiedSchedule.setFlags();
        if(changeSchedule) {
            return verifiedSchedule;
        }
        return null;
    }

    private SignSchedule getSign(ParkingSchedule verifiedSchedule) {
        SignSchedule verifiedSign = null;
        boolean changeAddress = false;
        StringBuilder newPlace = new StringBuilder(10);
        for (int i = 0; i < addressKeys.length; i++) {
            String key = addressKeys[i];
            EditText addressText = (EditText) findViewById(addressID.get(key));
            if (origAddress == null || !origAddress.get(key).equals(addressText.getText().toString())) {
                changeAddress = true;
                Log.i(TAG, "getSign: found address change for key "+key+" value "+addressText.getText().toString());
            }
            newPlace.append(addressText.getText().toString() + " ");
        }
        Log.i(TAG, "getSign: new address is "+newPlace.toString());
        if (changeAddress) {
/*
            String prevAddress = signPlace == null ? null : signPlace.shortAddress();
            Place verifiedPlace = Place.findPlace(newPlace.toString(), signPlace.getPosition(), maxDistanceKm);
            if (prevAddress != null) {
                double dist = Position.getDistanceKm(verifiedPlace.getPosition(), signPlace.getPosition());
                //
                //  FIXME: should probably warn the user if dist is large
                //
                Log.i(TAG, "Address changed from " + prevAddress + " to " + verifiedPlace.shortAddress() + " distance is " + dist + " km");
            }

            verifiedSign = new SignSchedule(verifiedSchedule, verifiedPlace, sign.id);
*/
            verifiedSign = new SignSchedule(verifiedSchedule, newPlace.toString(), sign.id);
        }

        if (verifiedSign == null && verifiedSchedule != null) {
            verifiedSign = new SignSchedule(verifiedSchedule, null, sign.id);
        }
        return verifiedSign;
    }

    public void cancel(View view) {
        Log.i(TAG, "Cancel");
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    private void setup() {
        signTypeLayout.setVisibility(View.VISIBLE);
        SimpleScheduleForJson schedule = null;
        if (sign.schedule == null) {
            Log.i(TAG, "auto schedule is null...");
            schedule = new SimpleScheduleForJson();
        }
        else {
            Log.i(TAG, "using auto schedule "+sign.schedule.toString());
            schedule = sign.schedule;
        }
        if (schedule.isRestriction) {
            Log.i(TAG, "No Parking");
            noParkingButton.setChecked(true);
        } else {
            Log.i(TAG, "Parking");
            parkingButton.setChecked(true);
        }

        Log.i(TAG, "time limit is " + schedule.timeLimitMinutes);
        timeLimit.setText("");
        timeLimit.append(Integer.toString(schedule.timeLimitMinutes));
        timeLimitLayout.setVisibility(View.VISIBLE);
        if (schedule.hasTimeRange()) {
            Log.i(TAG, "time range = " + schedule.timeStart() + " to " + schedule.timeEnd());
            startTime.setText(schedule.timeStart());
            endTime.setText(schedule.timeEnd());
            timeRangeLayout.setVisibility(View.VISIBLE);
        } else {
            Log.i(TAG, "no time range");
            noTimeRange.setChecked(true);
            startTime.setText("00:00");
            endTime.setText("00:00");
            timeRangeLayout.setVisibility(View.GONE);
        }

        if (schedule.hasDateRange()) {
            Log.i(TAG, "date range = " + schedule.dateStart() + " to " + schedule.dateEnd());
            startDate.setText(schedule.dateStart());
            endDate.setText(schedule.dateEnd());
            dateRangeLayout.setVisibility(View.VISIBLE);
        } else {
            Log.i(TAG, "no date range");
            noDateRange.setChecked(true);
            startDate.setText("April 1");
            endDate.setText("December 1");
            dateRangeLayout.setVisibility(View.GONE);
        }
        if (schedule.hasWeekDays()) {
            for ( WeekDay day : schedule.weekDaySet()) {
                Log.i(TAG, "week day set contains " + day.toString());
                CheckBox checkBox = getWeekdayBox(day);
                if (checkBox != null) {
                    checkBox.setChecked(true);
                }
            }
            weekDaysLayout.setVisibility(View.VISIBLE);
        }
        else {
            Log.i(TAG, "no date range");
            noWeekDays.setChecked(true);
            weekDaysLayout.setVisibility(View.GONE);
        }
        Log.i(TAG, "best accuracy = "+bestAccuracy);
        if (bestAccuracy > minAccuracyThreshold || bestAccuracy <0) {
            if (bestAccuracy < 0) {
                warningMessage.setText("GPS Unavailable: be sure to enter correct address!");
            }
            else {
                warningMessage.setText("GPS accuracy is weak (" + bestAccuracy + " meters) be sure to enter address");
            }
            if (signPlace != null && signPlace.getAddress() != null) {
                origAddress = new HashMap<String, String>();
                Address signAddress = signPlace.getAddress();
                Log.i(TAG, "got address " + signPlace.shortAddress());
                for (String key : addressID.keySet()) {
                    EditText addressText = (EditText) findViewById(addressID.get(key));
                    String value = null;
                    if (key.equals("streetNum")) {
                        value = signAddress.getStreetNumber();
                    } else if (key.equals("streetName")) {
                        value = signAddress.getStreetName();
                    } else if (key.equals("city")) {
                        value = signAddress.getTownOrCity();
                    }
                    addressText.setText("");
                    if (value != null) {
                        addressText.append(value);
                    } else {
                        addressText.append("?");
                    }
                    Log.i(TAG, "put " + addressText.getText().toString() + " for adress key " + key);
                    origAddress.put(key, addressText.getText().toString());
                }
            } else {
                Log.i(TAG, "no address avail");
            }
            addressLayout.setVisibility(View.VISIBLE);
        }
        else {
            addressLayout.setVisibility(View.INVISIBLE);
        }

        if (signPictureUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), signPictureUri);
                fullImage.setImageBitmap(bitmap);
//                backgroundImage.setImageBitmap(bitmap);
        //        Bitmap thumb = scaleDown(bitmap, 500, true);
        //        thumbNailer.setImageBitmap(thumb);
        //        viewImage.setImageBitmap(thumb);
            } catch (Exception ex) {
                Log.e(TAG, "Exception " + ex + " while setting thumbnail");
            }
        }
        buttonLayout.setVisibility(View.VISIBLE);
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());
        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width, height, filter);
        return newBitmap;
    }

    private CheckBox getWeekdayBox(WeekDay day) {
        CheckBox checkBox = null;
//        Log.i(TAG, "getWeekdayBox for "+day.toString());
        switch (day) {
            case Monday:
                checkBox = (CheckBox) findViewById(R.id.monday);

                break;
            case Tuesday:
                checkBox = (CheckBox) findViewById(R.id.tuesday);
                break;
            case Wednesday:
                checkBox = (CheckBox) findViewById(R.id.wednesday);
                break;
            case Thursday:
                checkBox = (CheckBox) findViewById(R.id.thursday);
                break;
            case Friday:
                checkBox = (CheckBox) findViewById(R.id.friday);
                break;
            case Saturday:
                checkBox = (CheckBox) findViewById(R.id.saturday);
                break;
            default:
//                Log.i(TAG, "getWeekdayBox: return null");
        }
        return checkBox;
    }

}

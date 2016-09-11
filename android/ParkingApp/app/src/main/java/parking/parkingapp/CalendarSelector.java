package parking.parkingapp;

import java.util.Calendar;

import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.DialogFragment;
import android.widget.TextView;
import java.util.Calendar;

public class CalendarSelector {

    private TextView textView;
    private Calendar calendar;
    private AppCompatActivity activity;
    private boolean selectTime;
    private boolean selectDate;

    public CalendarSelector(AppCompatActivity activity, int id, boolean selectDate, boolean selectTime) {
        this.activity = activity;
        this.selectTime = selectTime;
        this.selectDate = selectDate;
        textView = (TextView)activity.findViewById(id);
        calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        String dateText = day + "/" + (month + 1) + "/" + year;
        String timeText = hour + ":" + convertMinute(minute);
        if (selectDate && selectTime) {
            textView.setText( dateText+ " -" +timeText );
        }
        else if (selectDate) {
            textView.setText( dateText );
        }
        else if (selectTime) {
            textView.setText( timeText );
        }
    }

    private String convertMinute(int minute) {
        return minute < 10 ? "0"+Integer.toString(minute) : Integer.toString(minute);
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public boolean timeOnly() {
        return selectTime && !selectDate;
    }

    public boolean dateOnly() {
        return selectDate && !selectTime;
    }

    public String toString() {
        if (textView != null) {
            return textView.getText().toString();
        }
        return null;
    }

    public void addHour(int hoursToAdd) {
        calendar.add(Calendar.HOUR_OF_DAY, hoursToAdd); // date will move to the next day if required
    }

    public void show() {
        if (selectTime) {
            DialogFragment timeFragment = new TimeSelector(this);
            timeFragment.show(activity.getSupportFragmentManager(), "timePicker");
        }
        if (selectDate) {
            DialogFragment dateFragment = new DateSelector(this);
            dateFragment.show(activity.getSupportFragmentManager(), "datePicker");
        }
    }

    public Calendar getCalendar() {
        return calendar;
    }
}

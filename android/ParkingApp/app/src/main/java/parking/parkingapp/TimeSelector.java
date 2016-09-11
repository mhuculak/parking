package parking.parkingapp;

import parking.util.Utils;

import java.util.Calendar;
import android.os.Bundle;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.util.Log;

public class TimeSelector extends DialogFragment implements
        TimePickerDialog.OnTimeSetListener {

    private CalendarSelector calendarSelector;
    private TextView timeText;
    private int hour;
    private int minute;
    protected static final String TAG = "location-updates-sample";

    TimeSelector(CalendarSelector calendarSelector) {
        this.calendarSelector = calendarSelector;
        Calendar c = calendarSelector.getCalendar();
        if (c != null) {
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        }
        else {
            useCurrentTime();
        }
    }

    TimeSelector(TextView timeText) {
        this.timeText = timeText;
        if (timeText.getText() != null) {
            String dText = timeText.getText().toString();
            if (dText != null) {
                parseField(dText);
                return;
            }
        }
        useCurrentTime();
    }


    private void parseField(String field) {
        String[] comp = field.split("-");
        String tString = null;
        if (comp.length == 2) {
            field = comp[1];
        }
        String[] hrMin = field.split(":");
        if (hrMin.length == 2) {
            hour = Utils.parseInt(hrMin[0]);
            minute = Utils.parseInt((hrMin[1]));
        }
        else {
            useCurrentTime();
        }
    }

    private void useCurrentTime() {
        final Calendar c = Calendar.getInstance();
        hour = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    private String convertMinute(int minute) {
        return minute < 10 ? "0"+Integer.toString(minute) : Integer.toString(minute);
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (calendarSelector != null) {
            Calendar c = calendarSelector.getCalendar();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            c.set(year, month, day, hourOfDay, minute, 0);
            if (calendarSelector.timeOnly()) {
                calendarSelector.setText(hourOfDay + ":" + convertMinute(minute));
            }
            else {
                calendarSelector.setText(day + "/" + (month + 1) + "/" + year + " -" + hourOfDay + ":" + convertMinute(minute));
            }
        }
        else {
            timeText.setText(hourOfDay + ":" + convertMinute(minute));
        }
    }
}
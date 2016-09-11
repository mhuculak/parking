package parking.parkingapp;

import parking.util.Utils;
import parking.schedule.SimpleDate;

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

public class DateSelector extends DialogFragment implements
        DatePickerDialog.OnDateSetListener {

    private CalendarSelector calendarSelector;
    private TextView dateText;
    private int year;
    private int month;
    private int day;
    protected static final String TAG = "location-updates-sample";

    DateSelector(CalendarSelector calendarSelector) {
        this.calendarSelector = calendarSelector;
        Calendar c = calendarSelector.getCalendar();
        if (c != null) {
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        }
        else {
            useCurrentDate();
        }
    }

    DateSelector(TextView dateText) {
        this.dateText = dateText;
        if (dateText.getText() != null) {
            String dText = dateText.getText().toString();
            if (dText != null) {
                parseField(dText);
                return;
            }
        }
        useCurrentDate();
    }

    private void parseField(String dText) {
        if (dText != null) {
            String[] comp = dText.split("-");
            if (comp.length == 2) {
                dText = comp[0];
            }
            String[] ymd = dText.split("/");

            if (ymd.length == 3) {
                day = Utils.parseInt(ymd[0]);
                month = Utils.parseInt(ymd[1]) - 1;
                year = Utils.parseInt(ymd[2]);
            }
            else {
                String[] md = dText.split(" ");
                if (md.length == 2) {
                    SimpleDate sd = new SimpleDate(md[0], md[1]);
                    day = sd.getDayNum();
                    month = sd.getMonthNum();
                    final Calendar c = Calendar.getInstance();
                    year = c.get(Calendar.YEAR);
                }
                else {
                    useCurrentDate();
                }
            }
        }
    }

    private void useCurrentDate() {
        final Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        if (calendarSelector != null) {
            Calendar c = calendarSelector.getCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            c.set(year, month, day, hour, minute, 0);
            if (calendarSelector.dateOnly()) {
                calendarSelector.setText(SimpleDate.getMonth(month)+" "+day);
            }
            else {
                calendarSelector.setText(day + "/" + (month + 1) + "/" + year + " -" + hour + ":" + convertMinute(minute));
            }
       }
       else {
           dateText.setText(SimpleDate.getMonth(month)+" "+day);
       }
    }

    private String convertMinute(int minute) {
        return minute < 10 ? "0"+Integer.toString(minute) : Integer.toString(minute);
    }
}

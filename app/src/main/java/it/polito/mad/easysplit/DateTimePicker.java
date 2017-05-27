package it.polito.mad.easysplit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;

class DateTimePicker implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener{
    public interface Listener {
        void onDateTimeChanged(Calendar calendar);
    }

    private Calendar mCalendar;
    private DatePickerDialog mDatePickerDialog;
    private TimePickerDialog mTimePickerDialog;
    private Listener mListener;


    public Listener getListener() {
        return mListener;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public Calendar getCalendar() {
        return mCalendar;
    }


    DateTimePicker(Context ctx) { this(ctx, null); }

    DateTimePicker(Context ctx, Long initialTime) {
        mCalendar = Calendar.getInstance();
        if (initialTime != null)
            mCalendar.setTimeInMillis(initialTime);

        mDatePickerDialog = new DatePickerDialog(ctx, this,
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH));
        // Disable future dates
        mDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        mTimePickerDialog = new TimePickerDialog(ctx, this,
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                true);
    }

    void show() {
        mDatePickerDialog.show();
    }

    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        mCalendar.set(year, monthOfYear, dayOfMonth);
        mTimePickerDialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mCalendar.set(Calendar.MINUTE, minute);
        if (mListener != null)
            mListener.onDateTimeChanged(mCalendar);
    }
}

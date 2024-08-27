package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;



import java.util.Calendar;
import java.util.Date;

public class DateRangePickerDialog1 extends DialogFragment {

    Callback mCallback;
    Date mDate;
    DatePickerDialog mDatePickerDialog;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Calendar calendar = Calendar.getInstance();

        // Set initial date if mDate is provided
        if (mDate != null) {
            calendar.setTime(mDate);
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog
        DatePickerDialog mDatePickerDialog = new DatePickerDialog(getActivity(), (view, selectedYear, selectedMonth, selectedDay) -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(selectedYear, selectedMonth, selectedDay);

            if (mCallback != null) {
                mCallback.onDateTimeRecurrenceSet(
                        selectedCal.getTime(),
                        0, 0,
                        null, null
                );
            }
            dismiss();
        }, year, month, day);

        // Set minimum and maximum dates if necessary
        mDatePickerDialog.getDatePicker().setMinDate(0); // Set minimum date to epoch
        mDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis()); // Set maximum date to today

        return mDatePickerDialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
     /*   Calendar calendar = Calendar.getInstance();

        // Set initial date if mDate is provided
        if (mDate != null) {
            calendar.setTime(mDate);
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog
        mDatePickerDialog = new DatePickerDialog(getActivity(), (view, selectedYear, selectedMonth, selectedDay) -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(selectedYear, selectedMonth, selectedDay);

            if (mCallback != null) {
                // Since we are using a DatePickerDialog, there is no recurrence option.
                // We'll call the callback with the selected date and dummy time values (0 for hour and minute).
                mCallback.onDateTimeRecurrenceSet(
                        selectedCal.getTime(),
                        0, 0,
                        null, null // No recurrence information available
                );
            }
            dismiss();
        }, year, month, day);

        // Set minimum and maximum dates if necessary
        mDatePickerDialog.getDatePicker().setMinDate(0); // Set minimum date to epoch
        mDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis()); // Set maximum date to today

        // Show the DatePickerDialog when the view is created
        mDatePickerDialog.show();
*/
        // Return null because the DatePickerDialog itself handles the UI
        return null;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void onDateTimeRecurrenceSet(Date selectedDate, int hourOfDay, int minute, String data, String recurrenceRule);
    }
}

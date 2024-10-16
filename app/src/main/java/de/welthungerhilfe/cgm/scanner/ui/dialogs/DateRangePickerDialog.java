/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;



import java.util.Calendar;
import java.util.Date;

import de.welthungerhilfe.cgm.scanner.R;

public class DateRangePickerDialog extends DialogFragment {
   /* SublimePicker mSublimePicker;

    SublimeListenerAdapter mListener = new SublimeListenerAdapter() {
        @Override
        public void onCancelled() {
            dismiss();
        }

        @Override
        public void onDateTimeRecurrenceSet(SublimePicker sublimeMaterialPicker, SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
            if (mCallback != null) {
                mCallback.onDateTimeRecurrenceSet(selectedDate,
                        hourOfDay, minute, recurrenceOption, recurrenceRule);
            }
            dismiss();
        }
    };

    Callback mCallback;
    Date mDate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mSublimePicker = (SublimePicker) getActivity().getLayoutInflater().inflate(R.layout.sublime_picker, container);

        SublimeOptions options = new SublimeOptions();
        options.setCanPickDateRange(true);
        options.setDisplayOptions(SublimeOptions.ACTIVATE_DATE_PICKER);
        options.setDateRange(0, System.currentTimeMillis());

        Calendar startCal = Calendar.getInstance();
        if (mDate != null) {
            startCal.setTime(mDate);
        }
        options.setDateParams(startCal, startCal);

        mSublimePicker.initializePicker(options, mListener);

        return mSublimePicker;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule);
    }*/
}

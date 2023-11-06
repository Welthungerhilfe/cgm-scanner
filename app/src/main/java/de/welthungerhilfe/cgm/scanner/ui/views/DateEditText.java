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
package de.welthungerhilfe.cgm.scanner.ui.views;

import android.content.Context;
import android.graphics.Rect;
import com.google.android.material.textfield.TextInputEditText;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;

public class DateEditText extends TextInputEditText {

    public interface DateInputListener {

        void onDateEntered(String value);
    }

    private DateInputListener listener;

    public DateEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    public void setOnDateInputListener(DateInputListener l) {
        listener = l;
    }

    @Override
    public void onEditorAction(int actionCode) {
        super.onEditorAction(actionCode);

        //finish editing
        if (actionCode == EditorInfo.IME_ACTION_DONE) {
            if (listener != null) {
                listener.onDateEntered(getText().toString());
            }
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        //reset on first selection
        if (focused) {
           // fillFormat();
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (listener != null) {
                listener.onDateEntered(getText().toString());
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onSelectionChanged(int start, int end) {
        super.onSelectionChanged(start, end);

        char separator = DataFormat.getDateSeparator(getContext());
        int len = getText().length();
        if (len > 0) {
            if (start == end) {

                //validate all previous characters
                for (int i = 0; i < start - 1; i++) {
                    char c = getText().charAt(i);
                    if (c != separator) {
                        if ((c < '0') || (c > '9')) {
                            setSelection(i, i + 1);
                            return;
                        }
                    }
                }

                //get next selectable character
                while (start < len) {
                    if (getText().charAt(start) == separator) {
                        start++;
                    } else {
                        break;
                    }
                }

                //update selection
                if (start < len) {
                    setSelection(start, start + 1);
                } else {
                    setSelection(0, 1);
                }
            } else if (end - start != 1) {
                setSelection(start);
            }
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        //reset on invalid characters
        if (text.length() > 0) {
            fillFormat();
        }

        //validate input
        String input = text.toString();
        String format = DataFormat.getDateFormat(getContext());
        char separator = DataFormat.getDateSeparator(getContext());
        if (input.length() == format.length()) {

            //fill the missing numbers
            input = input.replaceAll("YYYY", "2000");
            input = input.replaceAll("YYY", "000");
            input = input.replaceAll("YY", "00");
            int index = input.indexOf('Y');
            if (index > 0) {
                char c = getText().charAt(index - 1);
                switch (c) {
                    case '0':
                    case '2':
                    case '4':
                    case '8':
                        input = input.replaceAll("Y", "0");
                        break;
                    default:
                        input = input.replaceAll("Y", "2");
                        break;

                }
            }
            input = input.replaceAll("MM", "12");
            index = input.indexOf('M');
            if (index > 0) {
                char c = getText().charAt(index - 1);
                switch (c) {
                    case '0':
                        input = input.replaceAll("M", "1");
                        break;
                    default:
                        input = input.replaceAll("M", "2");
                        break;

                }
            }
            input = input.replaceAll("DD", "01");
            input = input.replaceAll("D", "1");

            //get checked date
            long timestamp = DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, input);
            String checked = DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, timestamp);

            //get a fixed date
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = text.charAt(i);
                if (c != separator) {
                    if ((c >= '0') && (c <= '9')) {
                        output.append(checked.charAt(i));
                        continue;
                    }
                }
                output.append(c);
            }

            //reset if the fixed date is different
            if (output.toString().compareTo(text.toString()) != 0) {
                setText(format.toUpperCase());
            }

            //reset if the date is from the future
            else if (timestamp > System.currentTimeMillis()) {
                setText(format.toUpperCase());
            }
        }
    }

    private void fillFormat() {
        String format = DataFormat.getDateFormat(getContext());
        if (getText().length() != format.length()) {
            setText(format.toUpperCase());
        }
    }
}

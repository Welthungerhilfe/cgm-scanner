package de.welthungerhilfe.cgm.scanner.unittests;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.test.core.app.ApplicationProvider;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DataFormatTest {


    private Context context;

    @Before
    public void setUpContext() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void convertFormat() {

        //testing for TimestampFormat.DATE
        String str = DataFormat.convertFormat(context, DataFormat.TimestampFormat.DATE);
        assertThat("dd-MM-yyyy", is(str));


        //testing for TimestampFormat.TIME
        str = DataFormat.convertFormat(context, DataFormat.TimestampFormat.TIME);
        if (DateFormat.is24HourFormat(context)) {
            assertThat("kk:mm", is(str));

        } else {
            assertThat("hh:mm", is(str));
        }

        //testing for TimestampFormat.DATE_AND_TIME
        str = DataFormat.convertFormat(context, DataFormat.TimestampFormat.DATE_AND_TIME);
        if (DateFormat.is24HourFormat(context)) {
            assertThat("kk:mm dd-MM-yyyy", is(str));

        } else {
            assertThat("hh:mm dd-MM-yyyy", is(str));
        }
    }

    @Test
    public void filesize() {

        //testing for byte => 10MB
        String str = DataFormat.filesize(context, 10485760L);
        assertThat("10.0MB", is(str));

        //testing for 1MB =< byte < 10MB
        str = DataFormat.filesize(context, 1048576);
        assertThat("1.0MB", is(str));

        //testing for 1KB =< byte < 1MB
        str = DataFormat.filesize(context, 1048);
        assertThat("1.0KB", is(str));

        //testing for byte < 1KB
        str = DataFormat.filesize(context, 512);
        assertThat("0.5KB", is(str));

        //testing when byte = 0KB
        str = DataFormat.filesize(context, 0);
        assertThat("-", is(str));
    }

    @Test
    public void time() {
        //testing for millisecond >= 3600000
        String str = DataFormat.time(context, 3600000);
        assertThat("1.0h", is(str));

        //testing for millisecond >= 60000
        str = DataFormat.time(context, 60000);
        assertThat("1.0min", is(str));

        //testing for millisecond >= 1000
        str = DataFormat.time(context, 1000);
        assertThat("1.0s", is(str));

        //testing for millisecond >= 1
        str = DataFormat.time(context, 1);
        assertThat("1ms", is(str));

        //testing for millisecond = 0
        str = DataFormat.time(context, 0);
        assertThat("-", is(str));
    }


    //for testing averageValue with some data
    @Test
    public void averageValueTest() {
        ArrayList<Long> longValues = new ArrayList<>();
        longValues.add(1000L);
        longValues.add(1002L);

        long values = DataFormat.averageValue(longValues);
        assertThat(1001L, Matchers.is(values));
    }

    //for testing averageValue with null and empty list
    @Test
    public void averageValueTestForEmptyandNullList() {
        //testing with null arrayList
        ArrayList<Long> longValues = null;
        long values = DataFormat.averageValue(longValues);
        assertThat(0L, Matchers.is(values));

        //testing with empty arrayList
        longValues = new ArrayList<>();
        values = DataFormat.averageValue(longValues);
        assertThat(0L, Matchers.is(values));
    }

    // ########## Testing of parseDouble(ArrayList<Long> values) ##########
    @Test
    public void parseDoubleTest() {
        //test string with "," character
        String str = "25,0123";
        Double value = DataFormat.parseDouble(str);
        assertThat(25.0123, Matchers.is(value));

        //test string without "," character
        str = "1";
        value = DataFormat.parseDouble(str);
        assertThat(1.0, Matchers.is(value));

        //test string with null value
        str = null;
        value = DataFormat.parseDouble(str);
        assertThat(0.0, Matchers.is(value));

        //test string with empty value
        str = "";
        value = DataFormat.parseDouble(str);
        assertThat(0.0, Matchers.is(value));

        //test string with invalid number format
        str = "test";
        value = DataFormat.parseDouble(str);
        assertThat(0.0, Matchers.is(value));

    }

    // ########## Testing of parseDouble(ArrayList<Long> values) ##########
    @Test
    public void parseFloatTest() {
        //test string with "," character
        String str = "25,0123";
        float value = DataFormat.parseFloat(str);
        assertThat(25.0123F, Matchers.is(value));

        //test string without "," character
        str = "1";
        value = DataFormat.parseFloat(str);
        assertThat(1.0F, Matchers.is(value));

        //test string with null value
        str = null;
        value = DataFormat.parseFloat(str);
        assertThat(0.0F, Matchers.is(value));

        //test string with empty value
        str = "";
        value = DataFormat.parseFloat(str);
        assertThat(0.0F, Matchers.is(value));

        //test string with invalid number format
        str = "test";
        value = DataFormat.parseFloat(str);
        assertThat(0.0F, Matchers.is(value));

    }

    @Test
    public void getNameFromEmail() {
        //testing for email
        String email = "cgm@childgrowthmonitor.org";
        String name = DataFormat.getNameFromEmail(email);
        assertThat("cgm", Matchers.is(name));

        //testing for null email
        email = null;
        name = DataFormat.getNameFromEmail(email);
        assertThat("unknown", Matchers.is(name));

        //testing for empty email
        email = null;
        name = DataFormat.getNameFromEmail(email);
        assertThat("unknown", Matchers.is(name));

    }

    @Test
    public void checkDoubleDecimals() {

        //Check for two decimal
        String str = "2.33";
        int decimalNumber = DataFormat.checkDoubleDecimals(str);
        assertThat(2, Matchers.is(decimalNumber));

        //Check for one decimal
        str = "2.3";
        decimalNumber = DataFormat.checkDoubleDecimals(str);
        assertThat(1, Matchers.is(decimalNumber));

        //Check for No Decimal
        str = "23";
        decimalNumber = DataFormat.checkDoubleDecimals(str);
        assertThat(0, Matchers.is(decimalNumber));

        //Check With "," insteadof "."
        str = "2,3";
        decimalNumber = DataFormat.checkDoubleDecimals(str);
        assertThat(1, Matchers.is(decimalNumber));
    }
}
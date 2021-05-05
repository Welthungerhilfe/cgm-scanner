package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.os.Build;
import android.text.format.DateFormat;

import androidx.test.core.app.ApplicationProvider;

import org.bouncycastle.util.test.FixedSecureRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DataFormatTest {


    private Context context;

    @Before
    public void setUpContext(){
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
        String str = DataFormat.filesize(context,10485760L);
        assertThat("10.0MB",is(str));

        //testing for 1MB =< byte < 10MB
        str = DataFormat.filesize(context,1048576);
        assertThat("1.0MB",is(str));

        //testing for 1KB =< byte < 1MB
        str = DataFormat.filesize(context,1048);
        assertThat("1.0KB",is(str));

        //testing for byte < 1KB
        str = DataFormat.filesize(context,512);
        assertThat("0.5KB",is(str));

        //testing when byte = 0KB
        str = DataFormat.filesize(context,0);
        assertThat("-",is(str));
    }

    @Test
    public void time() {
        //testing for millisecond >= 3600000
        String str = DataFormat.time(context,3600000);
        assertThat("1.0h",is(str));

        //testing for millisecond >= 60000
        str = DataFormat.time(context,60000);
        assertThat("1.0min",is(str));

        //testing for millisecond >= 1000
        str = DataFormat.time(context,1000);
        assertThat("1.0s",is(str));

        //testing for millisecond >= 1
        str = DataFormat.time(context,1);
        assertThat("1ms",is(str));

        //testing for millisecond = 0
        str = DataFormat.time(context,0);
        assertThat("-",is(str));
    }
}
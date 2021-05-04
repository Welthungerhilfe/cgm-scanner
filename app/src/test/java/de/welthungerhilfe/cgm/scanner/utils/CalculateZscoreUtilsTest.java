package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Config(sdk = Build.VERSION_CODES.P)
@RunWith(RobolectricTestRunner.class)
public class CalculateZscoreUtilsTest {

    private Context context;


    @Before
    public void setUpContext(){
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void ZscoreTest_1() {

        double zScoreWTA = CalculateZscoreUtils.setData(context,106.0,16.100,1616,"female",0);
        double zScoreHTA = CalculateZscoreUtils.setData(context,106.0,16.100,1616,"female",1);
        double zScoreWTH = CalculateZscoreUtils.setData(context,106.0,16.100,1616,"female",2);

        System.out.println(" "+context+" "+zScoreHTA+" "+zScoreWTA+" "+zScoreWTH);

        assertThat("0.08563550387093903", is(zScoreWTA));
        assertThat("-0.37356210743171075", is(zScoreHTA));
        assertThat("-0.6712285381035584", is(zScoreWTH));






    }

}

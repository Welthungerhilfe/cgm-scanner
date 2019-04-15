package de.welthungerhilfe.cgm.scanner;

import android.content.Context;
import android.test.mock.MockApplication;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;

import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class TestAppController {
    @Mock
    Context context;

    @Test
    public void getPersonId() {
        //String validId = AppController.getInstance().getPersonId();
        String valid =  String.format("%s_person_%s_%s", "AndroidUUID", Utils.getUniversalTimestamp(), Utils.getSaltString(16));
        String invalidId = "9e58cfb935e72628_Mandloi_1545035711986_cyd2vEf3TUTPBzyq";

        String[] array = valid.split("_");

        Assert.assertEquals("Person ID schema is not correct, expect 3 underscores", 4, array.length);
        Assert.assertEquals("Wrong object name, expected : person", "person", array[1]);
        try {
            long timestamp = Long.parseLong(array[2]);
            //Assert.assertTrue("timestamp generated incorrectly", System.currentTimeMillis() < timestamp);
        } catch (NumberFormatException e) {
            Assert.fail("timestamp is not correct format");
        }
        Assert.assertEquals("Random string generated incorrectly", 16, array[3].length());
    }

    @Test
    public void getMeasureId() {
        String valid = String.format("%s_measure_%s_%s", "AndroidUUID", Utils.getUniversalTimestamp(), Utils.getSaltString(16));
        String invalidId = "9e58cfb935e72628_measure1_1545035746888_CetMsJLZJvUsTboi";

        String[] array = valid.split("_");

        Assert.assertEquals("Measure ID schema is not correct, expect 3 underscores", 4, array.length);
        Assert.assertEquals("Wrong object name, expected : measure", "measure", array[1]);
        try {
            long timestamp = Long.parseLong(array[2]);
            //Assert.assertTrue("timestamp generated incorrectly", System.currentTimeMillis() < timestamp);
        } catch (NumberFormatException e) {
            Assert.fail("timestamp is not correct format");
        }
        Assert.assertEquals("Random string generated incorrectly", 16, array[3].length());
    }

    @Test
    public void getArtifactId() {
        String valid = String.format("%s_artifact-%s_%s_%s", "AndroidUUID", "pcd", Utils.getUniversalTimestamp(), Utils.getSaltString(16));
        String invalidId = "9e58cfb935e72628_artifact-pcd2_1550288920148_eKtxZ4ZRBdXXIbQL";

        String[] array = valid.split("_");
        ArrayList<String> types = new ArrayList<>();
        types.add("pcd");
        types.add("rgb");

        Assert.assertEquals("Artifact ID schema is not correct, expect 3 underscores", 4, array.length);
        Assert.assertEquals("Artifact type is missing", 2, array[1].split("-").length);
        Assert.assertEquals("Wrong object name, expected : artifact", "artifact", array[1].split("-")[0]);
        Assert.assertTrue("Wrong artifact type, expected : pcd or rgb", types.contains(array[1].split("-")[1]));
        try {
            long timestamp = Long.parseLong(array[2]);
            //Assert.assertTrue("timestamp generated incorrectly", System.currentTimeMillis() < timestamp);
        } catch (NumberFormatException e) {
            Assert.fail("timestamp is not correct format");
        }
        Assert.assertEquals("Random string generated incorrectly", 16, array[3].length());
    }
}
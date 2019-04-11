package de.welthungerhilfe.cgm.scanner;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static org.junit.Assert.*;

public class AppControllerTest {

    @Test
    public void getPersonId() {
        String validId = "9e58cfb935e72628_person_1545035711986_cyd2vEf3TUTPBzyq";
        String invalidId = "9e58cfb935e72628_Mandloi_1545035711986_cyd2vEf3TUTPBzyq";

        String[] array = invalidId.split("_");

        Assert.assertEquals("Person ID schema is not correct, expect 3 underscores", 4, array.length);
        Assert.assertEquals("Wrong object name, expected : person", "person", array[1]);
        try {
            long timestamp = Long.parseLong(array[2]);
            Assert.assertTrue("timestamp generated incorrectly", System.currentTimeMillis() < timestamp);
        } catch (NumberFormatException e) {
            Assert.fail("timestamp is not correct format");
        }
        Assert.assertEquals("Random string generated incorrectly", 16, array[3].length());
    }

    @Test
    public void getMeasureId() {
        String validId = "9e58cfb935e72628_measure_1545035746888_CetMsJLZJvUsTboi";
        String invalidId = "9e58cfb935e72628_measure1_1545035746888_CetMsJLZJvUsTboi";

        String[] array = invalidId.split("_");

        Assert.assertEquals("Measure ID schema is not correct, expect 3 underscores", 4, array.length);
        Assert.assertEquals("Wrong object name, expected : measure", "measure", array[1]);
        try {
            long timestamp = Long.parseLong(array[2]);
            Assert.assertTrue("timestamp generated incorrectly", System.currentTimeMillis() < timestamp);
        } catch (NumberFormatException e) {
            Assert.fail("timestamp is not correct format");
        }
        Assert.assertEquals("Random string generated incorrectly", 16, array[3].length());
    }

    @Test
    public void getArtifactId() {
        String validId = "9e58cfb935e72628_artifact-pcd_1550288920148_eKtxZ4ZRBdXXIbQL";
        String invalidId = "9e58cfb935e72628_artifact-pcd2_1550288920148_eKtxZ4ZRBdXXIbQL";

        String[] array = invalidId.split("_");
        ArrayList<String> types = new ArrayList<>();
        types.add("pcd");
        types.add("rgb");

        Assert.assertEquals("Artifact ID schema is not correct, expect 3 underscores", 4, array.length);
        Assert.assertEquals("Artifact type is missing", 2, array[1].split("-").length);
        Assert.assertEquals("Wrong object name, expected : artifact", "artifact", array[1].split("-")[0]);
        Assert.assertTrue("Wrong artifact type, expected : pcd or rgb", types.contains(array[1].split("-")[1]));
        try {
            long timestamp = Long.parseLong(array[2]);
            Assert.assertTrue("timestamp generated incorrectly", System.currentTimeMillis() < timestamp);
        } catch (NumberFormatException e) {
            Assert.fail("timestamp is not correct format");
        }
        Assert.assertEquals("Random string generated incorrectly", 16, array[3].length());
    }
}
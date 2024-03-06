package de.welthungerhilfe.cgm.scanner.unittests;

import org.junit.Test;

import java.util.ArrayList;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import de.welthungerhilfe.cgm.scanner.AppController;


public class TestAppController {
    @Test
    public void getPersonId() {
        //String validId = AppController.getInstance().getPersonId();
        String valid =  String.format("%s_person_%s_%s", "AndroidUUID", AppController.getUniversalTimestamp(), AppController.getSaltString(16));
        String invalidId = "9e58cfb935e72628_Mandloi_1545035711986_cyd2vEf3TUTPBzyq";

        String[] array = valid.split("_");

        assertThat("Person ID schema is not correct, expect 3 underscores", 4, is(array.length));
        assertThat("Wrong object name, expected : person", "person", is(array[1]));
        try {
            long timestamp = Long.parseLong(array[2]);
            //Assert.assertTrue("timestamp generated incorrectly", System.currentTimeMillis() < timestamp);
        } catch (NumberFormatException e) {
            assertFalse("timestamp is not correct format",false);
        }
        assertThat("Random string generated incorrectly", 16, is(array[3].length()));
    }

    @Test
    public void getMeasureId() {
        String valid = String.format("%s_measure_%s_%s", "AndroidUUID", AppController.getUniversalTimestamp(), AppController.getSaltString(16));
        String invalidId = "9e58cfb935e72628_measure1_1545035746888_CetMsJLZJvUsTboi";

        String[] array = valid.split("_");

        assertThat("Measure ID schema is not correct, expect 3 underscores", 4, is(array.length));
        assertThat("Wrong object name, expected : measure", "measure", is(array[1]));
        try {
            long timestamp = Long.parseLong(array[2]);
            //Assert.assertTrue("timestamp generated incorrectly", System.currentTimeMillis() < timestamp);
        } catch (NumberFormatException e) {
            assertFalse("timestamp is not correct format",false);
        }
        assertThat("Random string generated incorrectly", 16, is(array[3].length()));
    }

    @Test
    public void getArtifactId() {
        String valid = String.format("%s_artifact-%s_%s_%s", "AndroidUUID", "pcd", AppController.getUniversalTimestamp(), AppController.getSaltString(16));
        String invalidId = "9e58cfb935e72628_artifact-pcd2_1550288920148_eKtxZ4ZRBdXXIbQL";

        String[] array = valid.split("_");
        ArrayList<String> types = new ArrayList<>();
        types.add("pcd");
        types.add("rgb");

        assertThat("Artifact ID schema is not correct, expect 3 underscores", 4, is(array.length));
        assertThat("Artifact type is missing", 2, is(array[1].split("-").length));
        assertThat("Wrong object name, expected : artifact", "artifact", is(array[1].split("-")[0]));
        assertTrue("Wrong artifact type, expected : pcd or rgb", types.contains(array[1].split("-")[1]));
        try {
            long timestamp = Long.parseLong(array[2]);
            //Assert.assertTrue("timestamp generated incorrectly", System.currentTimeMillis() < timestamp);
        } catch (NumberFormatException e) {
           assertFalse("timestamp is not correct format",false);
        }
        assertThat("Random string generated incorrectly", 16, is(array[3].length()));
    }
}

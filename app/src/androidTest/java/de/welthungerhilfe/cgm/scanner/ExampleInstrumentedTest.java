package de.welthungerhilfe.cgm.scanner;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("de.welthungerhilfe.cgm.scanner", appContext.getPackageName());
    }

    @Test
    public void testPersonIds() {
        String validId = AppController.getInstance().getPersonId();
        String invalidId1 = "9e58cfb935e72628_Mand_loi_1545035711986_cyd2vEf3TUTPBzyq";
        String invalidId2 = "9e58cfb935e72628_Mandloi_1545035711986_cyd2vEf3TUTPBzyq";

        String[] invalidIdArray1 = invalidId1.split("_");
        String[] invalidIdArray2 = invalidId2.split("_");
        String[] validIdArray = validId.split("_");

        assertEquals("Person ID schema is not correct, expect 3 underscores", 4, validIdArray.length);
        assertEquals("Wrong object name, expected : person", "person", validIdArray[1]);

        assertEquals("Person ID schema is not correct, expect 3 underscores", not(4), validIdArray1.length);
        assertEquals("Wrong object name, expected : person", not("person"), validIdArray2[1]);
    }

    @Test
    public void testMeasureIds() {
        String validId = AppController.getInstance().getMeasureId();
        String invalidId = "9e58cfb935e72628_measure1_1545035746888_CetMsJLZJvUsTboi";

        String[] array = validId.split("_");

        Assert.assertEquals("Measure ID schema is not correct, expect 3 underscores", 4, array.length);
        //Assert.assertEquals("wrong UUID", Utils.getAndroidID(InstrumentationRegistry.getTargetContext().getContentResolver()), array[0]);
        Assert.assertEquals("Wrong object name, expected : measure", "measure", array[1]);
    }

    @Test
    public void testArtifactIds() {
        String validId = AppController.getInstance().getArtifactId("pcd");
        String invalidId = "9e58cfb935e72628_artifact-pcd2_1550288920148_eKtxZ4ZRBdXXIbQL";

        String[] array = validId.split("_");
        ArrayList<String> types = new ArrayList<>();
        types.add("pcd");
        types.add("rgb");

        Assert.assertEquals("Artifact ID schema is not correct, expect 3 underscores", 4, array.length);
        //Assert.assertEquals("wrong UUID", Utils.getAndroidID(InstrumentationRegistry.getTargetContext().getContentResolver()), array[0]);
        Assert.assertEquals("Artifact type is missing", 2, array[1].split("-").length);
        Assert.assertEquals("Wrong object name, expected : artifact", "artifact", array[1].split("-")[0]);
        Assert.assertEquals("Wrong artifact type, expected : pcd or rgb", true, types.contains(array[1].split("-")[1]));
    }
}

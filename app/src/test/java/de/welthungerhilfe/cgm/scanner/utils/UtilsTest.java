package de.welthungerhilfe.cgm.scanner.utils;

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class UtilsTest {
    // ########## Testing of averageValue(ArrayList<Long> values) ##########

    //for testing averageValue with some data
    @Test
    public void averageValueTest() {
        ArrayList<Long> longValues = new ArrayList<>();
        longValues.add(1000L);
        longValues.add(1002L);

        long values = Utils.averageValue(longValues);
        assertThat(1001L, is(values));
    }

    //for testing averageValue with null and empty list
    @Test
    public void averageValueTestForEmptyandNullList() {
        //testing with null arrayList
        ArrayList<Long> longValues = null;
        long values = Utils.averageValue(longValues);
        assertThat(0L, is(values));

        //testing with empty arrayList
        longValues = new ArrayList<>();
        values = Utils.averageValue(longValues);
        assertThat(0L, is(values));
    }

    // ########## Testing of parseDouble(ArrayList<Long> values) ##########
    @Test
    public void parseDoubleTest() {
        //test string with "," character
        String str = "25,0123";
        Double value = Utils.parseDouble(str);
        assertThat(25.0123, is(value));

        //test string without "," character
        str = "1";
        value = Utils.parseDouble(str);
        assertThat(1.0, is(value));

        //test string with null value
        str = null;
        value = Utils.parseDouble(str);
        assertThat(0.0, is(value));

        //test string with empty value
        str = "";
        value = Utils.parseDouble(str);
        assertThat(0.0, is(value));

        //test string with invalid number format
        str = "test";
        value = Utils.parseDouble(str);
        assertThat(0.0, is(value));

    }

    // ########## Testing of parseDouble(ArrayList<Long> values) ##########
    @Test
    public void parseFloatTest() {
        //test string with "," character
        String str = "25,0123";
        float value = Utils.parseFloat(str);
        assertThat(25.0123F, is(value));

        //test string without "," character
        str = "1";
        value = Utils.parseFloat(str);
        assertThat(1.0F, is(value));

        //test string with null value
        str = null;
        value = Utils.parseFloat(str);
        assertThat(0.0F, is(value));

        //test string with empty value
        str = "";
        value = Utils.parseFloat(str);
        assertThat(0.0F, is(value));

        //test string with invalid number format
        str = "test";
        value = Utils.parseFloat(str);
        assertThat(0.0F, is(value));

    }

    @Test
    public void getNameFromEmail() {
        //testing for email
        String email = "cgm@childgrowthmonitor.org";
        String name = Utils.getNameFromEmail(email);
        assertThat("cgm", is(name));

        //testing for null email
        email = null;
        name = Utils.getNameFromEmail(email);
        assertThat("unknown", is(name));

        //testing for empty email
        email = null;
        name = Utils.getNameFromEmail(email);
        assertThat("unknown", is(name));

    }

    @Test
    public void checkDoubleDecimals() {

        //Check for two decimal
        String str = "2.33";
        int decimalNumber = Utils.checkDoubleDecimals(str);
        assertThat(2, is(decimalNumber));

        //Check for one decimal
        str = "2.3";
        decimalNumber = Utils.checkDoubleDecimals(str);
        assertThat(1, is(decimalNumber));

        //Check for No Decimal
        str = "23";
        decimalNumber = Utils.checkDoubleDecimals(str);
        assertThat(0, is(decimalNumber));

        //Check With "," insteadof "."
        str = "2,3";
        decimalNumber = Utils.checkDoubleDecimals(str);
        assertThat(1, is(decimalNumber));
    }

    @Test
    public void validateStdTestQrCodeTest() {
        boolean isValid = Utils.isValidateStdTestQrCode("STD_TEST_20210721");
        assertThat(true, is(isValid));
    }
}
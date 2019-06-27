package de.welthungerhilfe.cgm.scanner.helper.service;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.Locale;

import static de.welthungerhilfe.cgm.scanner.helper.receiver.AddressReceiver.RESULT_FAILED;
import static de.welthungerhilfe.cgm.scanner.helper.receiver.AddressReceiver.RESULT_SUCCESS;

public class AddressService extends IntentService {
    private static final String IDENTIFIER = "AddressService";

    private ResultReceiver resultReceiver;

    public AddressService() {
        super(IDENTIFIER);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String msg = "";

        resultReceiver = intent.getParcelableExtra("add_receiver");
        Location location = intent.getParcelableExtra("add_location");

        if (resultReceiver != null && location != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
            } catch (Exception ioException) {
                Log.e("", "Error in getting address for the location");
            }

            if (addresses == null || addresses.size()  == 0) {
                sendResultsToReceiver(RESULT_FAILED, msg);
            } else {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++)
                    sb.append(address.getAddressLine(i));

                msg = sb.toString();

                sendResultsToReceiver(RESULT_SUCCESS, msg);
            }
        } else {
            sendResultsToReceiver(RESULT_FAILED, msg);
        }
    }

    private void sendResultsToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString("address_result", message);
        resultReceiver.send(resultCode, bundle);
    }
}

package de.welthungerhilfe.cgm.scanner.helper.receiver;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public abstract class AddressReceiver extends ResultReceiver {
    public static final int RESULT_SUCCESS = 0x01;
    public static final int RESULT_FAILED = 0x00;

    public abstract void onAddressDetected(String result);

    public AddressReceiver(Handler handler) {
        super(handler);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {

        if (resultCode == RESULT_SUCCESS) {
            onAddressDetected(resultData.getString("address_result"));
        } else {
            onAddressDetected("");
        }
    }
}

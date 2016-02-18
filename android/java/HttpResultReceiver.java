package com.mooo.sms_dev.smscustomer;

/**
 * Created by admin on 2016-02-17.
 */
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class HttpResultReceiver extends ResultReceiver {
    private Receiver mReceiver;

    public HttpResultReceiver(Handler handler) {
        super(handler);
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);

    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {

        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}

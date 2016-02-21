package com.mooo.sms_dev.smsclient2;

/**
 * Created by admin on 2016-02-20.
 */
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

public class HttpResultReceiver extends ResultReceiver {
    private Receiver mReceiver;
    private static String TAG = "com.mooo.sms_dev.smsclient2.ResultReceiver";

    public HttpResultReceiver(Handler handler) {
        super(handler);
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);

    }

    public void setReceiver(Receiver receiver) {
        Log.i(TAG, "setReceiver");
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {

        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}

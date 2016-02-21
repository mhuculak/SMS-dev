package com.mooo.sms_dev.smsclient2;

/**
 * Created by admin on 2016-02-20.
 */


import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.content.Context;
import android.view.View;
import android.os.ResultReceiver;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;
import android.util.Log;


public class HttpService extends IntentService {


    private final IBinder binder = new HttpServiceBinder();
    private HttpClient httpClient = null;
    private ResultReceiver receiver = null;
    private static String TAG = "com.mooo.sms_dev.smsclient2.HttpService";

    public HttpService() {
//        Log.i(TAG, "Ctor");
        super("HttpService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent");
        receiver = intent.getParcelableExtra("receiverTag"); // receiver.send() is a callback in the thread which gave us this object
    }

    public class HttpServiceBinder extends Binder {
        HttpService getService() {
            Log.i(TAG, "getService");
            return HttpService.this;
        }
    }

    public HttpClient getClient() {
        Log.i(TAG, "getClient");
        return httpClient;
    }

    public void connectToCompany(String from, String to, String content) {
        Log.i(TAG, "connectToCompany");
        String url = getResources().getString(R.string.connect_url);
        Log.i(TAG, "connectToCompan yith url " + url);
        httpClient = new HttpClient(HttpMethod.POST, url);
        Map<String, String> post_data = new HashMap<String, String>();

        post_data.put( "Device", "android" );
        post_data.put( "Action", "avail" );
        post_data.put( "From", from);
        post_data.put("To", to);
        if (content != null && !content.equals("")) {
            post_data.put("Body", content);
        }

        httpClient.setPostData(post_data);
        Connect(httpClient);
//        return client;
    }

    public void sendMessage(String from, String to, String route_id, String content) {
        Log.i(TAG, "sendMessage");
        String url = getResources().getString(R.string.connect_url);
        httpClient = new HttpClient(HttpMethod.POST, url);
        Map<String, String> post_data = new HashMap<String, String>();

        post_data.put( "Device", "android" );
        post_data.put( "Action", "route-selected" );
//        post_data.put( "Route-name", route_name);
        post_data.put( "Route-id", route_id);
        post_data.put( "From", from );
        post_data.put("To", to);
        post_data.put("Body", content);

        httpClient.setPostData(post_data);

        Connect(httpClient);
    }

    public void rejectConfirmation(String from, String to, String route_id) {
        Log.i(TAG, "rejectConfirmation");
        String url = getResources().getString(R.string.connect_url);
        httpClient = new HttpClient(HttpMethod.POST, url);
        Map<String, String> post_data = new HashMap<String, String>();

        post_data.put( "Device", "android" );
        post_data.put( "Action", "reject-route" );
//        post_data.put( "Route-name", route_name);
        post_data.put( "Route-id", route_id);
        post_data.put( "From", from );
        post_data.put("To", to);

        httpClient.setPostData(post_data);

        Connect(httpClient);
    }

    public int Connect(final HttpClient client) {
        Log.i(TAG, "Connect enter");
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (httpClient != null) {
                    Log.i(TAG, "Connect conecting...");
                    int http_status = httpClient.connect();
                    Log.i(TAG, "Connect got status " + http_status);
                    Bundle bundle = new Bundle();
                    Log.i(TAG, "Connect invoke callback");
                    receiver.send(http_status, bundle); // this invokes the onReceiveResult callback of the activity that owns the receiver
                }
            }
        };
        Thread connectThread = new Thread(r);
        connectThread.start();
        return Service.START_STICKY;
    }

}


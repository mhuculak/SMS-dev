package com.mooo.sms_dev.smscustomer;

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
    private static String TAG = "com.mooo.sms_dev.smscustomer.HttpService";

    public HttpService() {
        super("HttpService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        receiver = intent.getParcelableExtra("receiverTag");
    }

    public class HttpServiceBinder extends Binder {
        HttpService getService() {
            return HttpService.this;
        }
    }

    public HttpClient getClient() {
        return httpClient;
    }

    public void connectToCompany(String from, String to) {
        String url = getResources().getString(R.string.connect_url);
        httpClient = new HttpClient(HttpMethod.POST, url);
        Map<String, String> post_data = new HashMap<String, String>();

        post_data.put( "Device", "android" );
        post_data.put( "Action", "get-available" );
        post_data.put( "From", from);
        post_data.put("To", to);

        httpClient.setPostData(post_data);
        Connect(httpClient);
//        return client;
    }

    public void sendMessage(String from, String to, String route_id, String route_name, String content) {
        String url = getResources().getString(R.string.connect_url);
        httpClient = new HttpClient(HttpMethod.POST, url);
        Map<String, String> post_data = new HashMap<String, String>();

        post_data.put( "Device", "android" );
        post_data.put( "Action", "route-selected" );
        post_data.put( "Route-name", route_name);
        post_data.put( "Route-id", route_id);
        post_data.put( "From", from );
        post_data.put("To", to);
        post_data.put("Body", content);

        httpClient.setPostData(post_data);

        Connect(httpClient);
    }

    public int Connect(final HttpClient client) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (httpClient != null) {
                    int http_status = httpClient.connect();
                    Bundle bundle = new Bundle();
                    receiver.send(http_status, bundle);
                }
            }
        };
        Thread connectThread = new Thread(r);
        connectThread.start();
        return Service.START_STICKY;
    }

}

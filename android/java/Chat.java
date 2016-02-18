package com.mooo.sms_dev.smscustomer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;

import com.mooo.sms_dev.smscustomer.HttpService.HttpServiceBinder;
import com.mooo.sms_dev.smscustomer.HttpResultReceiver.Receiver;

import java.util.HashMap;
import java.util.Map;
import android.util.Log;

public class Chat extends AppCompatActivity implements Receiver {

    private static TextView repName;
    private static String myPhone;
    private static String businessPhone;
    private static String route_id;
    private static EditText messageText;
    private static TextView chatText;
    HttpService httpService;
    private HttpClient httpClient = null;
    public HttpResultReceiver receiver;
    boolean isBound;
    private static String TAG = "com.mooo.sms_dev.smscustomer.Chat";

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            HttpService.HttpServiceBinder binder = (HttpService.HttpServiceBinder)service;
            httpService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
*/
        repName = (TextView) findViewById(R.id.repName);
        messageText = (EditText) findViewById(R.id.sendMessageText);
        chatText = (TextView) findViewById(R.id.chatText);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {

            } else {
                String route_name = extras.getString("route-name");
                route_id = extras.getString("route-id");
                repName.setText(route_name);
                businessPhone = extras.getString("businessPhone");
                myPhone = extras.getString("myPhone");
            }
        }
        receiver = new HttpResultReceiver(new Handler());
        receiver.setReceiver(this);

        Intent intent = new Intent(this, HttpService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        intent.putExtra("receiverTag", receiver);
        startService(intent);

    }

    public void sendMessage(View view) {
        String message = messageText.getText().toString();
        chatText.append(message + "\n");
        httpService.sendMessage(myPhone, businessPhone, route_id, repName.getText().toString(), message);
        messageText.setText("");
    }

    public void messageSent() {
        String response = httpClient.getBody();

        if (response != null) {
            Log.i(TAG, "messageSent: response " + response);
            chatText.append(response + "\n");
        }
        else {
            Log.i(TAG, "messageSent: no response ");
            chatText.append("no response status " + httpClient.getStatusCode() + "\n");
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        httpClient = httpService.getClient();
        Log.i(TAG, "onReceiveResult: got HTTP status " + httpClient.getStatusCode());
        messageSent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch(id) {
            case R.id.menu_connect:
                Intent intent = new Intent(this, Connect.class);
                startActivity(intent);
                break;
            case R.id.menu_route:
                break;
            case R.id.menu_chat:
                break;
            default:
        }

        return super.onOptionsItemSelected(item);
    }
}

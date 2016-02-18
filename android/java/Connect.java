package com.mooo.sms_dev.smscustomer;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Handler;

import com.mooo.sms_dev.smscustomer.HttpService.HttpServiceBinder;
import com.mooo.sms_dev.smscustomer.HttpResultReceiver.Receiver;

import java.util.*;

public class Connect extends AppCompatActivity implements Receiver {

    private static EditText businessPhoneNumber;
    private static EditText myPhoneNumberEntered = null;
    private static TextView myPhoneNumberLabel;
    private static TextView statusText;
    private static String myPhoneNumber = null;
    private static String businessPhoneNumberUsed = null;
    private HttpClient httpClient = null;
    public HttpResultReceiver receiver;
    HttpService httpService;
    boolean isBound;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            HttpServiceBinder binder = (HttpServiceBinder)service;
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
        setContentView(R.layout.activity_connect);
/*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
*/
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        myPhoneNumber = telephonyManager.getLine1Number();
        if (myPhoneNumber == null || myPhoneNumber.equals("")) {
            myPhoneNumberLabel = (TextView)findViewById(R.id.myPhoneLabel);
            myPhoneNumberLabel.setText("Enter Your Phone Number:");
            myPhoneNumberEntered = (EditText)findViewById(R.id.myPhoneNumber);
        }

        businessPhoneNumber = (EditText) findViewById(R.id.phoneNumber);
        statusText = (TextView) findViewById(R.id.statusText);
        receiver = new HttpResultReceiver(new Handler());
        receiver.setReceiver(this);

        Intent intent = new Intent(this, HttpService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        intent.putExtra("receiverTag", receiver);
        startService(intent);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {

            } else {
                businessPhoneNumberUsed = extras.getString("businessPhone");
                businessPhoneNumber.setText(businessPhoneNumberUsed);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        stopService(new Intent(mContext, ServiceRemote.class));
        unbindService(serviceConnection);

    }

    public void getPhoneNumber(View view) {
        Intent intent = new Intent(this, NumberList.class);
        startActivity(intent);
    }

    public void doConnect(View view) {

        if (myPhoneNumberEntered != null) {
            myPhoneNumber = myPhoneNumberEntered.getText().toString();
        }
        if (businessPhoneNumberUsed == null) {
            businessPhoneNumberUsed = businessPhoneNumber.getText().toString();
        }
        httpService.connectToCompany(myPhoneNumber, businessPhoneNumberUsed);
        statusText.setText("Connecting...");
    }

    public void onConnected() {
        NumberList.savePhoneNumber(this.getApplicationContext().getFilesDir(), businessPhoneNumberUsed);
        statusText.setText("Connected");
        if (httpClient.getStatusCode() == 200) {
            List<String> lines = httpClient.getLines();
            if (lines.size() > 1 && lines.get(0).equals("OK")) { // status + list of available
                Intent intent = new Intent(this, Route.class);
                String[] available = new String[lines.size()-1];
                int i=0;
                for ( i=1 ; i<lines.size() ; i++) {
                    available[i-1] = lines.get(i);
                }
                intent.putExtra("available", available);
                intent.putExtra("businessPhone", businessPhoneNumber.getText().toString());
                intent.putExtra("myPhone", myPhoneNumber);
                startActivity(intent);
            }
            else if (lines.size() >= 1) { // error string
                statusText.setText(lines.get(0));
            }
        }
        else {
            statusText.setText("Cannot Connect");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch(id) {
            case R.id.menu_connect:
                break;
            case R.id.menu_route:
                break;
            case R.id.menu_chat:
                break;
            default:
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        httpClient = httpService.getClient();
        onConnected();
    }


}

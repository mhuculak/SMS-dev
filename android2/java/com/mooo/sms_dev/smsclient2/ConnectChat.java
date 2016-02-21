package com.mooo.sms_dev.smsclient2;

import android.telephony.TelephonyManager;
import android.telephony.SmsManager;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.ComponentName;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.EditText;
import android.text.method.ScrollingMovementMethod;

import com.mooo.sms_dev.smsclient2.HttpService.HttpServiceBinder;
import com.mooo.sms_dev.smsclient2.HttpResultReceiver.Receiver;
import android.os.Handler;

import java.util.*;

import android.util.Log;

enum CustomerStatus { UNKNOWN, PENDING_ROUTING, CONFIRM_ROUTING, ROUTED, WAITING }; // should be the same as found in SMSmessage class

public class ConnectChat extends AppCompatActivity implements Receiver {

    private static String TAG = "com.mooo.sms_dev.smsclient2.ConnectChat";
    private static TextView companyName;
    private static TextView messageArea;
    private static EditText messageText;
    private static String sendMode = "HTTP";
    private static String clientPhoneNumber = null;
    private static String routeID = null;
    private static boolean isBound = false;
    private static CustomerStatus customerStatus = CustomerStatus.UNKNOWN;
    private static Map<String, String> available = null;
    private static String[] availAsString = null;
    private static Company selectedCompany = null;
    private static String messageAreaTextSave = "";

    private HttpService httpService;
    public HttpResultReceiver httpResultReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_chat);
        companyName = (TextView)findViewById(R.id.companyName);
        messageArea = (TextView)findViewById(R.id.messageArea);
        messageArea.setMovementMethod(new ScrollingMovementMethod());
        messageText = (EditText)findViewById(R.id.textMessage);
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        clientPhoneNumber = telephonyManager.getLine1Number();
        if (clientPhoneNumber == null || clientPhoneNumber.equals("")) {
            messageArea.append("ERROR: unable to retreive your phone number from phone. Please enter the number below and click " + R.string.send_message_button_name +"\n");
            messageText.setHint("phone number");
        }
        // setup the HttpService
        httpResultReceiver = new HttpResultReceiver(new Handler());
        Log.i(TAG, "onCreate: set recceiver");
        httpResultReceiver.setReceiver(this);

        Intent intent = new Intent(this, HttpService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        intent.putExtra("receiverTag", httpResultReceiver);
        startService(intent);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                Log.i(TAG, "onCreate: handling input");
                if (selectedCompany == null) {

                    selectedCompany = (Company) extras.getSerializable("company");
                    if (selectedCompany != null) {
                        Log.i(TAG, "onCreate: found selected company " + selectedCompany.toString());

                    }
                }
                String selected_route_id = extras.getString("route-id");
                if (selected_route_id != null) {
                    Log.i(TAG, "onCreate: found selected route id " + selected_route_id);
                    routeID = selected_route_id;
                    doSendMessage(null); // this will send the new route to the server
                }
                String semdModeFromSettings = extras.getString("send-mode");
                if (semdModeFromSettings != null) {
                    sendMode = semdModeFromSettings;
                }
            }
            else {
                Log.i(TAG, "onCreate: no input");
            }
        }
        else {
            Log.i(TAG, "onCreate: have saved intance state");
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                Log.i(TAG, "onCreate: have extras");
            }
            else {
                Log.i(TAG, "onCreate: no extras");
            }
        }
        if (selectedCompany != null) {
            if (selectedCompany.getName().equals("")) {
                companyName.setText(selectedCompany.getPhone());
            } else {
                companyName.setText(selectedCompany.getName());
            }
        }
        messageArea.setText(messageAreaTextSave);
    }

    public void doPickNumber(View view) {
        Log.i(TAG, "doPickNumber");
        Intent intent = new Intent(this, PickNumber.class);
        selectedCompany = null;
        startActivity(intent);
    }

    public void doSendMessage(View view) {
        String message = messageText.getText().toString();
        if (clientPhoneNumber == null) {
            clientPhoneNumber = messageText.getText().toString();
            messageText.setHint("send message");
            return;
        }
        if (selectedCompany == null) {
            messageArea.append("ERROR: no company selected - hint: try \"" + R.string.pick_number_button_name + "\" button\n");
            return;
        }

        if (sendMode.equals("HTTP")) { // when HTTP fetch is done on the main thread is causing rendering issues so we use a separate thread
            sendHttpMessage(clientPhoneNumber, selectedCompany, message, routeID);
        }
        else if (sendMode.equals("SMS")) {
            sendSMSmessage(clientPhoneNumber, selectedCompany, message, routeID );
        }
        else {
            String errmsg = "ERROR: invalid send mode " + sendMode +"\n";
            Log.e(TAG, errmsg);
            messageArea.append(errmsg); // should never happen
        }

    }

    private void sendSMSmessage(String from, Company company, String message, String route_id) {
        SmsManager smsManager = SmsManager.getDefault();
        String message_sent;
        if (route_id == null) {
            message_sent = message;
        }
        else {
            message_sent = "#goi route_id\n" + message; // prefix message with a route command
        }
        smsManager.sendTextMessage(company.getPhone(), null, message, null, null);
    }

    private void sendHttpMessage(String from, Company company, String message, String route_id) {
        Log.i(TAG, "sendHttpMessage: from " + from + " to " + company.getPhone());

        messageText.setText("");
        switch(customerStatus) {
            case UNKNOWN:
            case WAITING:
                Log.i(TAG, "sendHttpMessage: connecting with message = " + message);
                httpService.connectToCompany(from, company.getPhone(), message);
                if (message != null && !message.equals("")) {
                    messageArea.append(message + "\n");
                }
                break;
            case PENDING_ROUTING:
            case ROUTED:
                if (route_id == null || route_id.equals("") || message == null || message.equals("")) {
                    String errmsg = "sendHttpMessage: cannot send with message " + message + " to route " + route_id + " in state " + customerStatus +"\n";
                    Log.e(TAG, errmsg);
                    messageArea.append(errmsg);
                } else {
                    Log.i(TAG, "sendHttpMessage: send message = " + message + " to route " + route_id);
                    httpService.sendMessage(from, company.getPhone(), route_id, message);
                    messageArea.append(message + "\n");
                }
                break;
            case CONFIRM_ROUTING:
                if (route_id == null || route_id.equals("") || message == null || message.equals("")) {
                    String errmsg = "sendHttpMessage: cannot send with message " + message + " to route " + route_id + " in state " + customerStatus  +"\n";
                    Log.e(TAG, errmsg);
                    messageArea.append(errmsg);
                } else {
                    Log.i(TAG, "sendHttpMessage: send message = " + message + " to route " + route_id);
                    messageArea.append(message + "\n");
                    if (message.equalsIgnoreCase("y")) {
                        httpService.sendMessage(from, company.getPhone(), route_id, message);
                    } else {
                        httpService.rejectConfirmation(clientPhoneNumber, selectedCompany.getPhone(), route_id);
                    }
                }
                break;
        }
    }

    public void handleHttpResult(HttpClient httpClient) {
        if (httpClient.getStatusCode() != 200) {
            String errmsg = "ERROR: failed to send message via HTTP status code is " + httpClient.getStatusCode() + "\n";
            Log.e(TAG, errmsg);
            messageArea.append(errmsg);
            return;
        }
        SMSserverResponse serverResponse = new SMSserverResponse(httpClient);
        HandleServerResponse(serverResponse);
    }

    private void HandleServerResponse(SMSserverResponse serverResponse) {
        Intent intent;
        String errmsg;
        customerStatus = serverResponse.getCustomerStatus();
        Log.i(TAG, "HandleServerResponse: result = " + serverResponse.getResult() + " status = " + customerStatus + " company = " + serverResponse.getCompanyName());
        if (!serverResponse.getResult().equals("OK")) {
            errmsg = "HandleServerResponse: server sent error " + serverResponse.getResult() + " in state " + customerStatus +"\n";
            Log.e(TAG, errmsg);
            messageArea.append(errmsg);
        }
        if (selectedCompany.getName().equals("")) {
            Log.i(TAG, "HandleServerResponse: set company name to " + serverResponse.getCompanyName());
            selectedCompany.setName(serverResponse.getCompanyName());
            companyName.setText(serverResponse.getCompanyName());
            Log.i(TAG, "HandleServerResponse: company name is now " + selectedCompany.getName());
            PickNumber.saveCompany(this.getApplicationContext().getFilesDir(), selectedCompany);
        }
        switch(customerStatus) {
            case UNKNOWN:  // server should not finish a request in this state
                errmsg = "HandleServerResponse: server unexpectedly returned state " + customerStatus +"\n";
                Log.e(TAG, errmsg);
                messageArea.append(errmsg);
                break;
            case PENDING_ROUTING: // for this state we fire up another activity to pick a route from a list of available choices
                intent = new Intent(this, Route.class);
                available = new HashMap<String, String>();
                availAsString = serverResponse.getAvailable();
                for ( int i=0 ; i<availAsString.length ; i++ ) {
                    String[] id_name = availAsString[i].split("::");
                    if (id_name.length == 2) {
                        available.put(id_name[0], id_name[1]);
                    }
                }
                intent.putExtra("available", availAsString);
                messageAreaTextSave = messageArea.getText().toString();
                startActivity(intent);
                break;
            case CONFIRM_ROUTING: // display a (y/n) confirmation question
                routeID = serverResponse.getConfirmID();
                String route_name = available.get(routeID);
                messageArea.append("Would you like to chat with " + route_name + "? (y/n)\n");
                break;
            case ROUTED: // normal mode...just display the message
                Log.i(TAG, "HandleServerResponse: get message " + serverResponse.getMessage() + " customer state is " + customerStatus);
                messageArea.append(serverResponse.getMessage()+"\n");
                break;
            case WAITING: // display message while client waits for someone to become available
                messageArea.append(serverResponse.getWaitingMessage()+"\n");
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_connect_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected");
        int id = item.getItemId();
        Intent intent;
        messageAreaTextSave = messageArea.getText().toString();
        switch(id) {
            case R.id.menu_settings:
                intent = new Intent(this, Settings.class);
                intent.putExtra("parent", "Connect");
                startActivity(intent);
                break;
            case R.id.menu_number:
                intent = new Intent(this, PickNumber.class);
                startActivity(intent);
                break;
            case R.id.menu_route:
                if (availAsString != null) {
                    intent = new Intent(this, Route.class);
                    intent.putExtra("available", availAsString);
                    startActivity(intent);
                }
                break;
            default:
        }

        return super.onOptionsItemSelected(item);
    }

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


    // onReceiveResult is a callback invoked from the HttpService thread when it calls ResultReceiver.send()
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        Log.i(TAG, "onReceiveResult Callback");
        // httpClient is created on the HttpService and passed to this activity so we can access the results of the fetch
        handleHttpResult(httpService.getClient());
    }


}

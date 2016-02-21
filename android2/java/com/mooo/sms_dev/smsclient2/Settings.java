package com.mooo.sms_dev.smsclient2;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class Settings extends AppCompatActivity {

    private static Button sendModeButton;
    private String sendMode = "HTTP";
    private String parent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sendModeButton = (Button) findViewById(R.id.sendModeButton);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            parent = extras.getString("parent");
        }
    }

    public void onClickSendMode(View view) {
        if (sendMode.equals("HTTP")) {
            sendMode = "SMS";
            sendModeButton.setText("SMS");
        } else {
            sendMode = "HTTP";
            sendModeButton.setText("HTTP");
        }
    }

    public void onDone(View view) {

        Intent intent = null;

        if (parent.equals("Route")) {
            intent = new Intent(this, Route.class);
        }
        else {
            intent = new Intent(this, ConnectChat.class); // default
        }
        intent.putExtra("send-mode", sendMode);
        startActivity(intent);
    }

}

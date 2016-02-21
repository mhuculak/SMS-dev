package com.mooo.sms_dev.smsclient2;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Route extends AppCompatActivity {

    private static ListView listView;
//    private static String businessPhone;
//    private static String myPhone;
    private static String[] available_names = null;
    private static String[] available_ids = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);
/*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
*/
        final ListView listView = (ListView)findViewById(R.id.available);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String)(listView.getItemAtPosition(position));
                doRoute(selected, available_ids[position]);
            }
        });
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {

            }
            else {
                String[] available = extras.getStringArray("available");
                available_names = new String[available.length];
                available_ids = new String[available.length];
                int i;
                for ( i=0 ; i<available.length ; i++ ) {
                    String[] id_name = available[i].split("::");
                    if (id_name.length == 2) {
                        available_ids[i] = id_name[0];
                        available_names[i] = id_name[1];
                    }
                    else {
                        // FIXME: should not happen but need to return an error
                    }
                }
//                businessPhone = extras.getString("businessPhone");
//                myPhone = extras.getString("myPhone");
                ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, available_names);
                listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                listView.setAdapter(adapter);

            }
        }
    }
    private void doRoute(String route_name, String route_id) {
        Intent intent = new Intent(this, ConnectChat.class);
        intent.putExtra("route-name", route_name);
        intent.putExtra("route-id", route_id);
//        intent.putExtra("businessPhone", businessPhone);
//        intent.putExtra("myPhone", myPhone);
        startActivity(intent);
    }
   /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_route, menu);
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
    */
}

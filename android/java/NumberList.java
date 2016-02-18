package com.mooo.sms_dev.smscustomer;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


import android.util.Log;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class NumberList extends AppCompatActivity {

    private static String numberFileName = "SMScustomerNumbers";
    private static String TAG = "com.mooo.sms_dev.smscustomer.NumberList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: enter");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_number_list);
/*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
*/
        final ListView listView = (ListView)findViewById(R.id.numberList);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedPhone = (String) (listView.getItemAtPosition(position));
                doConnect(selectedPhone);
            }
        });
        String[] numbers = getNumbers(this.getApplicationContext().getFilesDir());
        if (numbers == null || numbers.length == 0) { // go back if nothing to display
            Intent intent = new Intent(this, Connect.class);
            startActivity(intent);
        }
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,numbers);

        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(adapter);
    }

    private void doConnect(String selectedPhone) {
        Intent intent = new Intent(this, Connect.class);
        intent.putExtra("businessPhone", selectedPhone);
        startActivity(intent);
    }

    private static String[] getNumbers(File filesDir) {
        FileInputStream is;
        BufferedReader reader;
        Log.i(TAG, "getNumbers: open file " + numberFileName);
        File file = new File(filesDir+File.separator+numberFileName);

        List<String> numberList = new ArrayList<String>();
        try {
            if (file.exists()) {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                while (line != null) {
                    numberList.add(line);
                    Log.d(TAG, "getNumbers: " + line);
                    line = reader.readLine();
                }
            }
        }
        catch (IOException ex) {
            Log.e(TAG, "getNumbers: Caught IO exception: " + ex.toString());
        }
        String[] numberArray = new String[numberList.size()];
        for ( int i=0 ; i<numberList.size() ; i++ ) {
            Log.d(TAG, "getNumbers: " + numberList.get(i));
            numberArray[i] = new String(numberList.get(i));
        }
        return numberArray;
    }

    public static void savePhoneNumber(File filesDir, String phoneToAdd) {

        String[] numberList = getNumbers(filesDir);
        for (String phone : numberList) {
            if (phoneToAdd.trim().equals(phone)) {
                return; // already there
            }
        }
        try {
            Log.i(TAG, "savePhoneNumber: open file " + numberFileName);
            File file = new File(filesDir+File.separator+numberFileName);
            FileWriter fstream = new FileWriter(file, true);
            BufferedWriter out = new BufferedWriter(fstream);
            Log.i(TAG, "savePhoneNumber: write to file " + phoneToAdd.trim());
            out.write(phoneToAdd.trim());
            out.newLine();
            out.close();
        }
        catch (IOException ex) {
            Log.e(TAG, "savePhoneNumber: Caught IO exception: " + ex.toString());
        }
    }
}

package com.mooo.sms_dev.smsclient2;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AbsListView;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

import android.util.Log;
import java.io.*;
import java.util.*;

public class PickNumber extends AppCompatActivity {

    private static String companyFileName = "SMSclientCompanyListTemp2"; // FIXME: remove temp after tested
    private static String TAG = "com.mooo.sms_dev.smsclient2.NumberList";
    private static EditText selectedPhoneNumber;
    private static TextView errorText;
    private Company selectedCompany = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_number);
        final ListView listView = (ListView)findViewById(R.id.numberList);
        selectedPhoneNumber = (EditText)findViewById((R.id.phoneNumber));
        errorText = (TextView)findViewById(R.id.errorText);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String companyString = (String)(listView.getItemAtPosition(position));
                selectedCompany = new Company(companyString);
            }
        });
        String[] companiesAsString = getCompaniesAsString(this.getApplicationContext().getFilesDir());
        if (companiesAsString == null || companiesAsString.length == 0) { // go back if nothing to display
            Log.i(TAG,"onCreate: company list is empty");
        }
        else {
            Log.i(TAG, "onCreate: company list size is " + companiesAsString.length);
        }
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, companiesAsString);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(adapter);
    }

    public void onDone(View view) {
        Intent intent = new Intent(this, ConnectChat.class);
        if (selectedCompany == null) {
            String selectedPhone = selectedPhoneNumber.getText().toString();
            if (selectedPhone == null || selectedPhone.equals("")) {
                Log.i(TAG, "onDone: nothing selected");
                errorText.setText("Nothing selected");
                return; // i.e. stay on this activity
            }
            else {
                Log.i(TAG, "onDone: returned selected phone number " + selectedPhone);
                selectedCompany = new Company("", selectedPhone);
            }
        }
        Log.i(TAG, "onDone: returned selected company " + selectedCompany.toString());
        intent.putExtra("company", (Serializable) selectedCompany);
        startActivity(intent); // go to ConnectChat activity
    }

    private String[] getCompaniesAsString(File filesDir) {
        Company[] companies = getCompanies(filesDir);
        String[] companiesAsString = new String[companies.length];
        for ( int i=0 ; i<companiesAsString.length ; i++ ) {
//            Log.d(TAG, "getNumbers: " + numberList.get(i));
            companiesAsString[i] = companies[i].toString();
        }
        return companiesAsString;
    }

    private static Company[] getCompanies(File filesDir) {
        FileInputStream is;
        BufferedReader reader;
        File file = new File(filesDir+File.separator+companyFileName);

        List<Company> companyList = new ArrayList<Company>();
        try {
            if (file.exists()) {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                while (line != null) {
                    Company company = new Company(line);
                    companyList.add(company);
                    line = reader.readLine();
                }
            }
        }
        catch (IOException ex) {
            Log.e(TAG, "getCompanies: Caught IO exception: " + ex.toString());
        }
        Log.i(TAG, "getCompanies: found " + companyList.size() + " saved companies");
        Company[] companyArray = new Company[companyList.size()];
        for ( int i=0 ; i<companyArray.length ; i++ ) {
//            Log.d(TAG, "getNumbers: " + numberList.get(i));
            companyArray[i] = companyList.get(i);
        }
        return companyArray;
    }

    public static void saveCompany(File filesDir, Company newCompany) {

        Company[] companyList = getCompanies(filesDir);
        String name = newCompany.getName();
        newCompany.setName(name.trim());
        String phone = newCompany.getPhone();
        newCompany.setPhone(phone.trim());
        for (Company company : companyList) {
            if (newCompany.getPhone().equals(company.getPhone())) {
                return; // already there, FIXME: what if the name has changed
            }
        }
        try {
            Log.i(TAG, "saveCompany: open file " + companyFileName);
            File file = new File(filesDir+File.separator+companyFileName);
            FileWriter fstream = new FileWriter(file, true);
            BufferedWriter out = new BufferedWriter(fstream);
            Log.i(TAG, "saveCompany: write to file " + newCompany.toString());
            out.write(newCompany.toString());
            out.newLine();
            out.close();
        }
        catch (IOException ex) {
            Log.e(TAG, "savePhoneNumber: Caught IO exception: " + ex.toString());
        }
    }
}

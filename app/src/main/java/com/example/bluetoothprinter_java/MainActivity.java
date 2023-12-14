package com.example.bluetoothprinter_java;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    Activity act;
    Context context;

    public EditText edt_fullname, edt_phone, edt_occupation;
    public Button btnPrintReceipt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        act = this;
        context = this;

        edt_fullname = (EditText) findViewById(R.id.edt_fullname);
        edt_phone = (EditText) findViewById(R.id.edt_phone);
        edt_occupation = (EditText) findViewById(R.id.edt_occupation);
        btnPrintReceipt = (Button) findViewById(R.id.btnPrintReceipt);

        btnPrintReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!verified()) {
                    return;
                }

                //save records to db
                try {

                    Intent i = new Intent(getApplicationContext(), PrintingActivity.class);
                    i.putExtra("name", edt_fullname.getText().toString().trim());
                    i.putExtra("phone", edt_phone.getText().toString().trim());
                    i.putExtra("occ", edt_occupation.getText().toString().trim());
                    startActivity(i);

                    act.finish();

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
    }
    boolean verified(){

        boolean valid = true;

        if (edt_fullname.getText().toString().trim().isEmpty()){
            edt_fullname.setError("Required");
            edt_fullname.requestFocus();
            valid = false;
        }

        if (edt_occupation.getText().toString().trim().isEmpty()){
            edt_occupation.setError("Required");
            edt_occupation.requestFocus();
            valid = false;
        }

        if (edt_phone.getText().toString().trim().isEmpty()){
            edt_phone.setError("Required");
            edt_phone.requestFocus();
            valid = false;
        }

        return valid;
    }

}
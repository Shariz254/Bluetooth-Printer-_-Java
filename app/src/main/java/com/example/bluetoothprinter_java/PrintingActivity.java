package com.example.bluetoothprinter_java;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PrintingActivity extends AppCompatActivity {

    Activity act;
    Context context;
    private ProgressDialog progress;
    public Button print, scanPrinter;
    public TextView nametxt, phonetxt, occtxt;
    String name_, phone_, occ_;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    BluetoothDevice mDevice;
    OutputStream mOutputStream = null;
    InputStream mInputStream = null;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothDevice device;

    int is_copy = 0;
    String printer_address, printer_name;
    String[] permissions = new String[]{
            android.Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
    };
    public static final int MULTIPLE_PERMISSIONS = 1; // code you want.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printing);

        act = this;
        context = this;
        progress = new ProgressDialog(this);

        checkPermissions();

        Bundle bundle = getIntent().getExtras();
        name_ = bundle.getString("name");
        phone_ = bundle.getString("phone");
        occ_ = bundle.getString("occ");

        nametxt = (TextView) findViewById(R.id.nametxt);
        phonetxt = (TextView) findViewById(R.id.phonetxt);
        occtxt = (TextView) findViewById(R.id.occtxt);

        nametxt.setText("FullName:: " +name_);
        phonetxt.setText("Phone No:: " +phone_);
        occtxt.setText("Occupation:: " +occ_);

        scanPrinter = (Button) findViewById(R.id.scanPrinter);
        scanPrinter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                _dialogOpenToScanPrinters();

            }
        });


        print = (Button) findViewById(R.id.print);
        print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (mSocket == null){
                        findBT();
                        openBT();
                    } else {
                        sendDataToPrint();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

    }
    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }

        return true;
    }
    void _dialogOpenToScanPrinters(){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PrintingActivity.this);
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view_ = layoutInflaterAndroid.inflate(R.layout.settings_activity, null);
        builder.setView(view_);
        builder.setCancelable(true);

        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            final ArrayList<String> mArrayAdapter = new ArrayList<String>();

            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
            Iterator<BluetoothDevice> iterator = pairedDevices.iterator();
            String stuff = "";

            while (iterator.hasNext()) {
                device = iterator.next();

                Log.w(this.getClass().getSimpleName(),
                        "Found " + device.getAddress() + " = "
                                + device.getName());

                mArrayAdapter.add(device.getAddress()+"\n"+device.getName());
            }

            CharSequence[] cs = mArrayAdapter.toArray(new CharSequence[mArrayAdapter.size()]);
            builder.setTitle("Select Printer To Use: ");
            builder.setItems(cs, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // The 'which' argument contains the index position
                    // of the selected item
                    Log.v("!!!!!!!!!!!!!",""+mArrayAdapter.get(which));
                    String selected_device = "";
                    selected_device = mArrayAdapter.get(which);
                    String[] arrInfo=selected_device.split("\n");
                    Log.v("****", arrInfo[0]);

                    //Save to database
//                    ContentValues collect = new ContentValues();
//                    collect.put("printer_name", arrInfo[1]);
//                    collect.put("printer_address", arrInfo[0]);
//                    collect.put("status", "pending");
//                    sd.SelectPrinterSetting(collect);

                    printer_name = arrInfo[1];
                    printer_address = arrInfo[0];

                    Toast.makeText(PrintingActivity.this, "Printer Saved Successfully.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    void findBT() {
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null){
                Log.v("Bluetooth Adapter", "Adapter UnAvailable");
            }

            if (!mBluetoothAdapter.isEnabled()){
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0){
                for (BluetoothDevice device : pairedDevices){
                    Log.v("Paired device", device.getName());

                    if (device.getName().equalsIgnoreCase(printerName())){
                        mDevice = device;
                        break;
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    void openBT() throws IOException {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();

            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();

            progress.dismiss();

            try {

                sendDataToPrint();

            } catch (Exception e){
                e.printStackTrace();
            }


        } catch (NullPointerException e){
            try {
                mSocket = (BluetoothSocket) mDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}) .invoke(mDevice, 1);
                mSocket.connect();

                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();

                Log.v("Connected", "To Bluetooth");

            } catch (Exception e1){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true);
                builder.setMessage("Please Turn Off Your Printer And Put It On, Then Click On The Print Button Shown.");
                builder.setInverseBackgroundForced(true);
                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                try {
                                    closeBT();
                                } catch (IOException e3) {
                                    Log.v("unable to close(",
                                            e3.getMessage());
                                }
                                dialog.dismiss();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        } catch (Exception e){
            e.printStackTrace();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setMessage("Please Turn Off Your Printer And Put It On, Then Click On The Print Button Shown.");
            builder.setInverseBackgroundForced(true);
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            try {
                                closeBT();
                            } catch (IOException e3) {
                                Log.v("unable to close()",
                                        e3.getMessage());
                            }
                            dialog.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    void closeBT() throws IOException {
        try {
            Log.v("$$$$$$$$$$$$", "Am Closing");

            mSocket.close();
            mSocket = null;

            safeClose(this.mSocket);

        }catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void safeClose(Closeable c){
        if (c == null) {
            return;
        }

        for (int retries = 3; retries > 0; retries--){
            try {
                c.close();
            }catch (IOException e){
                e.printStackTrace();

                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e1){
                    e1.printStackTrace();
                }
            }
        }
    }

    private final BroadcastReceiver mPairingRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    int pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234);
                    byte[] pinBytes;
                    pinBytes = (""+pin).getBytes("UTF-8");
                    device.setPin(pinBytes);
                    device.setPairingConfirmation(true);

                }catch (Exception e){
                    Log.e("m", "Error occurs when trying to auto pair");
                    e.printStackTrace();
                }
            }
        }
    };


    public String printerName(){
        return printer_name;
    }

    void sendDataToPrint() throws IOException {
        try {

            String receiptNo = "";

            /** when local db is integrated in project **/
//            PrintingDataModel pp = sd.getDataToPrint(""+ GlobalVariables.printing_id);
//            String fullname = pp.getFullname();
//            String phone = pp.getPhone_no();
//            String occupation = pp.getOccupation();
            /** when local db is integrated in project **/

            receiptNo = ""+System.currentTimeMillis();

            ///////////////////////////////////
            final Calendar c = Calendar.getInstance();
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);
            ///////////////////////////////////

            ///////////////////////////////////
            String header = null, body, footer, divider, title = null, center, CRLF, sign, body1, application = "PRINTING", fullname1, phone1, occupation1;
            divider = "------------------------------";
            title = " Printer | Test | Here ";
            fullname1 = "Name :: " + "\n" + "\n";
            phone1 = "Phone :: " + "\n";
            occupation1 = "Occupation :: ";
            CRLF = "\n";

            byte[] btCenterText;
            byte[] btBoldText;
            byte[] btLeftText;

            Calendar cal= Calendar.getInstance();
            SimpleDateFormat dateformat=new SimpleDateFormat("yyyy/MM/dd HH:mm");
            String Date1 = dateformat.format(cal.getTime());

            header = "SUCCESS " + "\n" + "Date: " + Date1 + "\n" + " " + divider + "\n" + " ";
            body1 = divider + "\n" + " ";
            sign = "Sign: ......................." + "\n";
            ///////////////////////////////////


            ///////////////////////////////////
            btCenterText = hexStringToByteArray("1B6131");// 30left-31Center-32Right
            btLeftText = hexStringToByteArray("1B6130");// 30left-31Center-32Right
            btBoldText = hexStringToByteArray("1B2102");

            mOutputStream.write(btBoldText);
            mOutputStream.write(btLeftText);

            mOutputStream.write(CRLF.getBytes());
            mOutputStream.flush();
            mOutputStream.write(btLeftText);
            mOutputStream.write(header.getBytes());
            mOutputStream.flush();

            if (is_copy == 1){
                String copy = "COPY OF RECEIPT"+"\n";
                mOutputStream.write(copy.getBytes());
                mOutputStream.flush();
            }

            center = "Ticket No : "+receiptNo;
            mOutputStream.write(center.getBytes());
            mOutputStream.flush();
            mOutputStream.write(CRLF.getBytes());
            mOutputStream.flush();

            String name = "FullName :: " +name_;
            mOutputStream.write(name.getBytes());
            mOutputStream.flush();
            mOutputStream.write(CRLF.getBytes());
            mOutputStream.flush();

            String phone_no = "Phone :: " +phone_;
            mOutputStream.write(phone_no.getBytes());
            mOutputStream.flush();
            mOutputStream.write(CRLF.getBytes());
            mOutputStream.flush();

            String my_occupation = "Occupation :: " +occ_;
            mOutputStream.write(my_occupation.getBytes());
            mOutputStream.flush();
            mOutputStream.write(CRLF.getBytes());
            mOutputStream.flush();

            mOutputStream.write(divider.getBytes());
            mOutputStream.write(CRLF.getBytes());
            mOutputStream.flush();

            mOutputStream.write(sign.getBytes());
            mOutputStream.write(CRLF.getBytes());
            mOutputStream.flush();


            mOutputStream.write(CRLF.getBytes());
            mOutputStream.write(CRLF.getBytes());
            mOutputStream.write(CRLF.getBytes());
            ///////////////////////////////////

            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(i);

            is_copy = 1;
            print.setText("Print Copy");

        } catch (NullPointerException e) {
            Log.i(" &&&&&&&& ", "" + e);
        } catch (Exception e) {
            Log.i(" iiiiiii ", "" + e);
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16));
        }

        return data;
    }

}
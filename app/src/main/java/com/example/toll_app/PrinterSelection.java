package com.example.toll_app;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PrinterSelection extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    AutoCompleteTextView printer;
    List<String> devices;
    Set<BluetoothDevice> pairedDevices;
    List<BluetoothDevice> dvs;
    MaterialCardView loader, loader_connection;
    ArrayAdapter<String> adapter;
    SharedPreferences sp;
    String booth;
    int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer_selection);

        getWindow().setStatusBarColor(getResources().getColor(R.color.black));

        sp = getSharedPreferences("main", MODE_PRIVATE);
        booth = getIntent().getStringExtra("booth");

        checkForLocationPermission();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            checkForBluetoothPermission();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.EXTRA_DEVICE);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }

        mBluetoothAdapter.startDiscovery();

        String defaultPrinter = sp.getString("lastPrinter","Select the Printer");

        printer = findViewById(R.id.autoText);
        printer.setText(defaultPrinter);
        loader = findViewById(R.id.loader);
        loader.setVisibility(View.VISIBLE);
        loader_connection = findViewById(R.id.loader_connection);

        MaterialCardView next = findViewById(R.id.next);
        BluetoothDevice[] bt = new BluetoothDevice[1];

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        dvs = new ArrayList<>();
        devices = new ArrayList<>();
        i = 0;
        for(BluetoothDevice BT : pairedDevices){
            if(BT.getName().equals(defaultPrinter)){
                bt[0] = BT;
            }
            devices.add(BT.getName());
            dvs.add(BT);
            i++;
        }

        next.setOnClickListener((v) -> {
            if(!printer.getText().toString().equals(getResources().getString(R.string.select_the_printer))
                    && printer.getText().length() > 0){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    checkForBluetoothPermission();
                pairDevice(bt[0]);
            }
        });

        printer.setOnItemClickListener((adapterView, view, i, l) -> bt[0] = dvs.get(i));

        loadData();
    }

    private void checkForLocationPermission(){
        if(!(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)){
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    6
            );
        }
    }

    private void loadData(){
        adapter = new ArrayAdapter<>(this,R.layout.drop_down_item,devices);
        printer.setAdapter(adapter);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                checkForBluetoothPermission();
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.e("Device : ", "Found");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                if(!dvs.contains(device) && deviceName != null){
                    dvs.add(device);
                    devices.add(deviceName);
                    adapter.notifyDataSetChanged();
                }
            }else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
                Log.e("Connected","true");
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.e("Device : ", "Done Searching");
                loader.setVisibility(View.GONE);
            }else if(BluetoothDevice.EXTRA_DEVICE.equals(action)){
                Log.e("Device : ", "Extra Searching");
            }else{
                Log.e("Device : ", "start searching");
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkForBluetoothPermission() {
        String[] permissions = new String[2];
        permissions[0] = Manifest.permission.BLUETOOTH_CONNECT;
        permissions[1] = Manifest.permission.BLUETOOTH_SCAN;

        if(!(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED)
        ){
            ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    1
            );
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1){
            if(grantResults.length  > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Grated", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this, "We need Your Permission!", Toast.LENGTH_LONG).show();
            }
        }else if(requestCode == 6){
            if(grantResults.length  > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Location Permission Grated", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this, "We need Your Permission!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void pairDevice(BluetoothDevice device) {
        loader.setVisibility(View.GONE);

        loader_connection.setVisibility(View.VISIBLE);

        ConnectThread ct = new ConnectThread(device,this);
        ct.start();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private Context context;

        public ConnectThread(BluetoothDevice device, Context context) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            this.context = context;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                    checkForBluetoothPermission();
                }
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (Exception e) {
                Log.e("", "Socket's create() method failed", e);
                Toast.makeText(context, "Printer MAY BE OFF", Toast.LENGTH_LONG).show();
            }
            mmSocket = tmp;
        }

        public void run() {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                checkForBluetoothPermission();
            }
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Log.e("Connection : ", connectException.getMessage());
                hideLoader();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("", "Could not close the client socket", closeException);
                }

                return;
            }

            connectionEstablished(context,mmDevice,mmSocket);

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
                hideLoader();
            } catch (IOException e) {
                hideLoader();
                Log.e("", "Could not close the client socket", e);
            }
        }
    }

    private void hideLoader(){
        runOnUiThread(() -> loader_connection.setVisibility(View.GONE));
    }

    private void connectionEstablished(Context context, BluetoothDevice device, BluetoothSocket mmSocket){
        Looper.prepare();
        Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(context, AuthenticateUser.class);
        String ip = getIntent().getStringExtra("ip");
        intent.putExtra("selected",(Parcelable) device);
        intent.putExtra("ip", ip);
        intent.putExtra("booth", booth);
        hideLoader();
        if(mmSocket != null){
            Socket.setSocket(mmSocket);
            sp.edit().putString("lastPrinter",printer.getText().toString()).apply();
            context.startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
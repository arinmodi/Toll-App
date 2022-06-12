package com.example.toll_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(getResources().getColor(R.color.black));

        sp = getSharedPreferences("main", MODE_PRIVATE);

        EditText ip = findViewById(R.id.ip_input);
        RelativeLayout connect = findViewById(R.id.connect);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            checkForBluetoothPermission(true,ip);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }

        String zeroTo255
                = "(\\d{1,2}|(0|1)\\"
                + "d{2}|2[0-4]\\d|25[0-5])";

        // Regex for a digit from 0 to 255 and
        // followed by a dot, repeat 4 times.
        // this is the regex to validate an IP address.
        String regex
                = zeroTo255 + "\\."
                + zeroTo255 + "\\."
                + zeroTo255 + "\\."
                + zeroTo255;

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        ip.setText(sp.getString("lastIP",""));

        connect.setOnClickListener((view) -> {
                if(ip.getText().length() >= 7 && ip.getText().length() <= 15
                        && ip.getText() != null && p.matcher(ip.getText()).matches()){
                    checkForLocationPermission(ip);
                }else{
                    Toast.makeText(this, "please enter a valid ip", Toast.LENGTH_LONG).show();
                }
        });
    }

    private void checkForLocationPermission(EditText ip){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                checkForBluetoothPermission(false,ip);
            else
                connectToIP(ip);
        }else{
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    6
            );
        }
    }

    private void checkForBluetoothPermission(boolean init,EditText ip) {
        String[] permissions = new String[2];
        permissions[0] = Manifest.permission.BLUETOOTH_CONNECT;
        permissions[1] = Manifest.permission.BLUETOOTH_SCAN;

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
        ){
            if(!init){
                // reached here after connect
                connectToIP(ip);
            }
        }else{
            ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    1
            );
        }
    }

    private void connectToIP(EditText ip){

        StringBuilder result = new StringBuilder();
        String url = "http://" + ip.getText() + ":8000?ip=" + ip.getText().toString();
        Log.e("URL : " , url);
        final HttpURLConnection[] urlConnection = {null};

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] flag = {true};

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL requestUrl = new URL(url);
                    urlConnection[0] = (HttpURLConnection) requestUrl.openConnection();
                    urlConnection[0].connect(); // no connection is made
                    InputStream in = new BufferedInputStream(urlConnection[0].getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                } catch (Exception e) {
                    flag[0] = false;
                    e.printStackTrace();
                } finally {
                    urlConnection[0].disconnect();
                }

                handler.post(() -> {
                    if(flag[0] && !result.equals(" ")){
                        Log.e("Data : ", result.toString());
                        Intent printerSelection = new Intent(MainActivity.this,printer_selection.class);
                        try{
                            JSONObject object = new JSONObject(result.toString());
                            String booth = object.getString("booth");
                            String companyName = object.getString("companyName");
                            sp.edit().putString("company", companyName).commit();
                            Log.e("booth : ", booth);
                            printerSelection.putExtra("booth", booth);
                        }catch(Exception e){
                            Log.e("Error : ", e.getMessage());
                        }
                        sp.edit().putString("lastIP",ip.getText().toString()).commit();
                        Toast.makeText(MainActivity.this,"Connected",Toast.LENGTH_LONG).show();
                        printerSelection.putExtra("ip",ip.getText().toString());
                        startActivity(printerSelection);
                    }else{
                        Toast.makeText(MainActivity.this,"Could Not Connected",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1){
            if(grantResults.length  > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Bluetooth Permission Grated", Toast.LENGTH_LONG).show();
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

}
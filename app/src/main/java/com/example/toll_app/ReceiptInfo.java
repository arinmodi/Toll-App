package com.example.toll_app;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReceiptInfo extends AppCompatActivity {

    BluetoothDevice mmDevice;

    AutoCompleteTextView vtU, jtU, laneU;
    EditText vNo, vehicleWeight;
    TextView vWt;

    SharedPreferences sp;

    String[] lane;
    String[] vt;
    String uname;
    String booth;
    HashMap<String, String> amount = new HashMap<>();
    HashMap<String, String> weights = new HashMap<>();
    MaterialCardView loader;
    String fees;
    BluetoothSocket bs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipet_info);

        sp = getSharedPreferences("main", MODE_PRIVATE);


        getWindow().setStatusBarColor(getResources().getColor(R.color.black));

        booth = getIntent().getStringExtra("booth");
        Log.e("Booth : ", booth);

        RelativeLayout print = findViewById(R.id.print);

        uname = getIntent().getStringExtra("username");

        mmDevice = getIntent().getParcelableExtra("selected");
        lane = getIntent().getStringArrayExtra("lane");
        laneU = findViewById(R.id.lane_auto);
        vtU = findViewById(R.id.v_type_autp);
        jtU = findViewById(R.id.j_type_auto);
        vNo = findViewById(R.id.v_no);
        vWt = findViewById(R.id.v_wt);
        vehicleWeight = findViewById(R.id.v_wt_overload);
        loader = findViewById(R.id.loader_process);

        vtU.setOnItemClickListener((adapterView, view, i, l) -> {
            String key = vt[i];
            Log.e("Key : ", vt[i]);
            vWt.setText(weights.get(key));
        });

        jtU.setText(sp.getString("JT", "Select the Journey Type"));


        if (!isAPrinter(mmDevice)) {
            Toast.makeText(this, "Selected Device is not printer", Toast.LENGTH_SHORT).show();
            finish();
        }

        print.setOnClickListener((v) -> {
            if (!checkValidation()) {
                Toast.makeText(this, "Enter the Proper Data", Toast.LENGTH_LONG).show();
            } else {
                loader.setVisibility(View.VISIBLE);
                getAmount(jtU.getText().toString(), vtU.getText().toString());
            }
        });

        getData();
    }

    // print the data
    private void print_data(String ticketNo, String date) {
        bs = Socket.getSocket();
        BluPrintsPrinter pl = new BluPrintsPrinter(bs);
        System.out.println(date);
        String text = "National Highway Authority Of India\n" + sp.getString("company", "") + " Toll Plaza\n";
        try {
            bs.getOutputStream().flush();
            pl.POS_ThreeInchCENTER();
            pl.print(text);
            pl.print(" \n");
            pl.setFontType(BluPrintsPrinter.FONT_003);
            pl.setFontType(BluPrintsPrinter.TEXT_ALIGNMENT_LEFT);
            String data = "Ticket No         : \t" + ticketNo + "\n"
                        + "Lane              : \t" + laneU.getText().toString() + "\n"
                        + "Date and Time     : \t" + date + "\n"
                        + "Vehicle No        : \t" + vNo.getText().toString() + "\n"
                        + "Type of Vehicle   : \t" + vtU.getText().toString() + "\n"
                        + "Type of Journey   : \t" + jtU.getText().toString() + "\n"
                        + "Vehicle Weight    : \t" + vWt.getText().toString() + "\n"
                        + "Method of Payment : \t" + "CASH" + "\n"
                        + "Fees              : \t" + fees + "\n";
            pl.POS_FontThreeInchLEFT();
            pl.print(data);
            pl.print(" \n");
            String dots = ". . . . . . . . . . . . . . . . . . . . . . .\n";
            pl.setFontType(BluPrintsPrinter.FONT_003);
            pl.setFontType(BluPrintsPrinter.TEXT_ALIGNMENT_LEFT);
            pl.print(dots);
            pl.print(" \n");
            String data2 = " \n" + "Only For Overloaded Vehicles";
            pl.POS_ThreeInchCENTER();
            pl.print(data2);
            pl.print(" \n");
            String data3 = "Standard Weight of Vehicle : \t" + vWt.getText().toString() + " KG\n"
                    + "Class Weight of Vehicle    : \t" + "0 KG\n"
                    + "Actual Weight of Vehicle   : \t" + "0 kg\n"
                    + "Overweight of Vehicle Fees : \t" + "0 Rs\n";
            pl.setFontType(BluPrintsPrinter.FONT_003);
            pl.setFontType(BluPrintsPrinter.TEXT_ALIGNMENT_LEFT);
            pl.POS_FontThreeInchLEFT();
            pl.print(data3);
            pl.print(" \n");
            pl.setFontType(BluPrintsPrinter.FONT_003);
            pl.setFontType(BluPrintsPrinter.TEXT_ALIGNMENT_LEFT);
            pl.print(dots);
            pl.print(" \n");
            String data4 = "24 X 7 Help Line 1033 \n All Toll Payments are FastTag Only \n Note: Double Fare Charged Due to non compliance of FastTag Rule \n Wish You Happy and Safe Journey\n";
            pl.POS_ThreeInchCENTER();
            pl.print(data4);
            pl.setLineFeed(4);
        } catch (Exception e) {
            Log.e("Error : ", e.toString());
            Toast.makeText(this, "Connection Lost", Toast.LENGTH_LONG).show();
            Intent printer = new Intent(this, PrinterSelection.class);
            startActivity(printer);
            finish();
        }
        loader.setVisibility(View.GONE);
    }

    // get the fees amount from selected journey type and vehicle type
    private void getAmount(String j, String v) {
        fees = amount.get(j + "-" + v);
        if (fees == null) {
            Toast.makeText(this, "Not a valid Journey", Toast.LENGTH_LONG).show();
            loader.setVisibility(View.GONE);
        } else {
            sendAPIData();
        }
    }

    // get the vehicle data from the local server
    private void getData() {
        String ip = getIntent().getStringExtra("ip");
        String tollInfo = "http://" + ip + ":8000/api/tollplazafeerules/fetch";
        String vehicleWeight = "http://" + ip + ":8000/api/vehiclemaster/fetch";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            String[] jt;
            HttpURLConnection urlConnection, urlConnection1;
            StringBuilder result = new StringBuilder();

            @Override
            public void run() {
                try {
                    ArrayList<String> j_typeList = new ArrayList<>();
                    ArrayList<String> v_typeList = new ArrayList<>();
                    URL requestUrl = new URL(tollInfo);
                    URL weightURL = new URL(vehicleWeight);
                    urlConnection = (HttpURLConnection) requestUrl.openConnection();
                    urlConnection1 = (HttpURLConnection) weightURL.openConnection();
                    urlConnection.connect();
                    urlConnection1.connect();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    in = new BufferedInputStream(urlConnection1.getInputStream());
                    reader = new BufferedReader(new InputStreamReader(in));

                    Log.e("journey : ", result.toString());

                    if (result.length() > 0) {
                        JSONObject jsonObject = new JSONObject(result.toString());
                        JSONArray Array = jsonObject.getJSONArray("data");
                        for (int i = 0; i < Array.length(); i++) {
                            JSONObject obj = Array.getJSONObject(i);
                            String j_type = obj.getString("j_type");
                            String v_type = obj.getString("v_type");
                            String amount_ = obj.getString("amount");
                            if (!amount.containsKey(j_type + "-" + v_type)) {
                                amount.put(j_type + "-" + v_type, amount_);
                            }
                            if (!j_typeList.contains(j_type)) {
                                j_typeList.add(j_type);
                            }
                            if (!v_typeList.contains(v_type)) {
                                v_typeList.add(v_type);
                            }
                        }
                    }

                    vt = new String[v_typeList.size()];
                    jt = new String[j_typeList.size()];

                    Log.e("JT : ", j_typeList.toString());
                    Log.e("VT : ", v_typeList.toString());
                    Log.e("Amount : ", amount.toString());

                    vt = v_typeList.toArray(vt);
                    jt = j_typeList.toArray(jt);

                    String weight;
                    StringBuilder wResult = new StringBuilder();
                    while ((weight = reader.readLine()) != null) {
                        wResult.append(weight);
                    }

                    Log.e("Weight : ", wResult.toString());

                    if (wResult.length() > 0) {
                        JSONObject jsonObject = new JSONObject(wResult.toString());
                        JSONArray Array = jsonObject.getJSONArray("data");
                        for (int i = 0; i < Array.length(); i++) {
                            JSONObject obj = Array.getJSONObject(i);
                            String v_type = obj.getString("v_type");
                            String v_wt = obj.getString("v_wt");
                            if (!weights.containsKey(v_type)) {
                                weights.put(v_type, v_wt);
                            }
                        }
                    }

                    Log.e("weights", weights.toString());


                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                    urlConnection1.disconnect();
                }

                handler.post(() -> {

                    if (lane.length != 0) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(ReceiptInfo.this, R.layout.drop_down_item, lane);
                        laneU.setAdapter(adapter);
                    }

                    if (vt.length != 0) {
                        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(ReceiptInfo.this, R.layout.drop_down_item, vt);
                        vtU.setAdapter(adapter1);
                    }

                    if (jt.length != 0) {
                        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(ReceiptInfo.this, R.layout.drop_down_item, jt);
                        jtU.setAdapter(adapter2);
                    }
                });
            }
        });
    }

    // send data to local server
    private void sendAPIData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String ip = getIntent().getStringExtra("ip");
        String url = "http://" + ip + ":8000/api/boothtransaction/add";
        final Handler handler = new Handler(Looper.getMainLooper());


        executor.execute(() -> {
                    boolean flag = false;
                    String ticketNo = "";
                    String date = "";
                    OkHttpClient client = new OkHttpClient();

                    RequestBody formBody = new FormBody.Builder()
                            .add("vehicle", vNo.getText().toString())
                            .add("type", vtU.getText().toString())
                            .add("journey", jtU.getText().toString())
                            .add("s_weight", vWt.getText().toString())
                            .add("lane", laneU.getText().toString())
                            .add("username", uname)
                            .add("booth", booth)
                            .add("weight", vehicleWeight.getText().toString())
                            .add("amount", fees)
                            .build();

                    Request request = new Request.Builder()
                            .url(url)
                            .post(formBody)
                            .build();

                    try {
                        Response response = client.newCall(request).execute();
                        JSONObject object = new JSONObject(response.body().string());
                        boolean status = object.getBoolean("success");
                        date = object.getString("date");
                        ticketNo = object.getString("trxnid");
                        if (status) {
                            flag = true;
                        }
                    } catch (Exception e) {
                        flag = false;
                        e.printStackTrace();
                    }

                    boolean finalFlag = flag;
                    String finalTicketNo = ticketNo;
                    String finalDate = date;
                    handler.post(() -> {
                        if (finalFlag) {
                            print_data(finalTicketNo, finalDate);
                            Toast.makeText(ReceiptInfo.this, "Transaction Added", Toast.LENGTH_LONG).show();
                        } else {
                            loader.setVisibility(View.GONE);
                            Toast.makeText(ReceiptInfo.this, "Transaction Failed", Toast.LENGTH_LONG).show();
                        }
                    });
                }
        );
    }

    // check is a printer or not
    private boolean isAPrinter(BluetoothDevice device) {
        int printerMask = 0b000001000000011010000000;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            checkForBluetoothPermission();
        int fullCod = device.getBluetoothClass().hashCode();
        Log.d("COD", "FULL COD: " + fullCod);
        Log.d("COD", "MASK RESULT " + (fullCod & printerMask));
        return (fullCod & printerMask) == printerMask;
    }

    // validate the user input
    private boolean checkValidation() {
        EditText v_no = findViewById(R.id.v_no);
        TextView v_wt = findViewById(R.id.v_wt);
        AutoCompleteTextView v_type = findViewById(R.id.v_type_autp);
        AutoCompleteTextView j_type = findViewById(R.id.j_type_auto);
        AutoCompleteTextView lane = findViewById(R.id.lane_auto);

        if (v_no.getText().toString().equals(" ") || v_no.getText().length() <= 0) {
            return false;
        } else if (v_wt.getText().toString().equals(" ") || v_wt.getText().length() <= 0) {
            return false;
        } else if (v_type.getText().toString().equals(getResources().getString(R.string.select_vehicle_type))
                || v_type.getText().length() <= 0) {
            return false;
        } else if (j_type.getText().toString().equals(getResources().getString(R.string.select_journey_type))
                || j_type.getText().length() <= 0) {
            return false;
        } else if (lane.getText().toString().equals(getResources().getString(R.string.select_lane))
                || lane.getText().length() <= 0) {
            return false;
        } else if (vehicleWeight.getText().toString().equals(getResources().getString(R.string.enter_the_vehicle_wight))
                || vehicleWeight.getText().length() <= 0) {
            return false;
        } else {
            sp.edit().putString("JT", j_type.getText().toString()).apply();
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            bs.close();
        } catch (Exception e) {
            Log.e("Unable To close : ", e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkForBluetoothPermission() {
        String[] permissions = new String[2];
        permissions[0] = Manifest.permission.BLUETOOTH_CONNECT;
        permissions[1] = Manifest.permission.BLUETOOTH_SCAN;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED
        ) {
            //permission granted
        } else {
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

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Grated", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "We need Your Permission!", Toast.LENGTH_LONG).show();
            }
        }
    }

}
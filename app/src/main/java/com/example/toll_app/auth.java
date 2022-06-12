package com.example.toll_app;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class auth extends AppCompatActivity {

    MaterialCardView loader;
    JSONArray lane;
    Context context = this;
    String booth;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        getWindow().setStatusBarColor(getResources().getColor(R.color.black));

        EditText uname = findViewById(R.id.u_name);
        EditText pass = findViewById(R.id.pass);
        RelativeLayout submit = findViewById(R.id.submit);
        loader = findViewById(R.id.loader);

        booth = getIntent().getStringExtra("booth");

        sp = getSharedPreferences("main", MODE_PRIVATE);
        uname.setText(sp.getString("username",""));
        pass.setText(sp.getString("password",""));

        submit.setOnClickListener((v) -> {
            if (uname.getText().length() != 0 && pass.getText().length() >= 3) {
                validate(uname.getText().toString(), pass.getText().toString());
            } else {
                Toast.makeText(this, "Enter proper credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validate(String uname, String pass) {
        loader.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        final Handler handler = new Handler(Looper.getMainLooper());
        String ip = getIntent().getStringExtra("ip");
        BluetoothDevice mmDevice = getIntent().getParcelableExtra("selected");
        Log.e("Selected : ", mmDevice.toString());

        String url = "http://" + ip + ":8000/api/authenticate";

        executor.execute(() -> {

                    boolean flag = false;

                    OkHttpClient client = new OkHttpClient();

                    RequestBody formBody = new FormBody.Builder()
                            .add("username", uname)
                            .add("password", pass)
                            .build();

                    Request request = new Request.Builder()
                            .url(url)
                            .post(formBody)
                            .build();

                    try {
                        Response response = client.newCall(request).execute();
                        Log.e("Response : ", response.toString());
                        JSONObject object = new JSONObject(response.body().string());
                        boolean status = object.getBoolean("success");
                        Log.e("code", status + "");
                        if(status){
                            lane = object.getJSONArray("lane");
                        }else{
                            flag = true;
                        }
                    } catch (Exception e) {
                        flag = true;
                        e.printStackTrace();
                    }

                    boolean finalFlag = flag;
                    handler.post(() -> {
                        loader.setVisibility(View.GONE);
                        try {

                            if (!finalFlag) {
                                String[] lanes = new String[lane.length()];
                                for (int i = 0; i < lane.length(); i++) {
                                    lanes[i] = lane.getString(i);
                                }
                                Intent intent = new Intent(this, recipet_info.class);
                                intent.putExtra("selected", (Parcelable) mmDevice);
                                intent.putExtra("ip", ip);
                                intent.putExtra("lane", lanes);
                                intent.putExtra("username", uname);
                                intent.putExtra("booth", booth);
                                Toast.makeText(auth.this, "Authentication SUCCESS", Toast.LENGTH_SHORT).show();
                                sp.edit().putString("username", uname).commit();
                                sp.edit().putString("password", pass).commit();
                                startActivity(intent);
                            } else {
                                Toast.makeText(auth.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                            }
                        }catch (Exception e){
                            Toast.makeText(auth.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
        );
    }
}
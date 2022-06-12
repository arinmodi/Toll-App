package com.example.toll_app;

import android.bluetooth.BluetoothSocket;

import java.io.Serializable;

public class Socket {
    private static BluetoothSocket socket;


    public static void setSocket(BluetoothSocket socket) {
        Socket.socket = socket;
    }

    public static BluetoothSocket getSocket(){
        return socket;
    };



}

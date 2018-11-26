package com.apollo.apollo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends MainActivity implements Runnable {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private ConnectedThread connectedThread;
    private DatabaseHelper mDatabaseHelper;

    public ConnectThread(BluetoothDevice device, DatabaseHelper mDatabaseHelper) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;

        this.mDatabaseHelper = mDatabaseHelper;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            UUID uuid = UUID.fromString("0fee0450-e95f-11e5-a837-0800200c9a66");
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {
        Looper.prepare();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        connectedThread = new ConnectedThread(mmSocket, mDatabaseHelper);
        Thread thread = new Thread(connectedThread);
        thread.start();

        connectedThread.write("Connected!");

        Log.d(TAG, "Should have sent message");
    }

    public ConnectedThread getConnectedThread() {
        return connectedThread;
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}
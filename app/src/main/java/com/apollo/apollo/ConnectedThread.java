package com.apollo.apollo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.util.Log;
import android.database.Cursor;

//send sms
import android.telephony.SmsManager;

import com.here.android.mpa.common.GeoCoordinate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.os.Vibrator;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class ConnectedThread extends MainActivity implements Runnable {

    private final static String TAG = "ConnectedThread";

    private final BluetoothSocket socket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private final BluetoothDevice mmDevice;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private MapFragmentView mapFragmentView;
    private OptionSwitches optionSwitches;
    private HelmetFragment helmetFragment;
    private Activity m_activity;
    private Vibrator vibrator;
    
    // declare a database instance to use the database
    DatabaseHelper mDatabaseHelper;

    // create a sms instance
    SmsManager smsManager = SmsManager.getDefault();
    
    public ConnectedThread(Activity activity,
                           BluetoothDevice device,
                           DatabaseHelper mDatabaseHelper,
                           MapFragmentView mapFragmentView,
                           Vibrator vibrator) {

        this.m_activity = activity;
        this.mapFragmentView = mapFragmentView;
        this.mDatabaseHelper = mDatabaseHelper;
        this.vibrator = vibrator;

        BluetoothSocket tmp = null;
        mmDevice = device;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            UUID uuid = UUID.fromString("0fee0450-e95f-11e5-a837-0800200c9a66");
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }

        socket = tmp;
    }

    public void run() {
        Looper.prepare();
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            socket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;

        write("Connected!");

        Log.d(TAG, "Connected!");

        mmBuffer = new byte[1024];
        int numBytes = 0; // bytes returned from read()


        // Doesn't work currently since optionSwitches will always be null
        if (optionSwitches != null) {
            write(getOptionMessage("crashSensor", optionSwitches.getCrashSwitch().isChecked()));
            write(getOptionMessage("blindspotSensor", optionSwitches.getBlindspotSwitch().isChecked()));
            write(getOptionMessage("hud", optionSwitches.getHudSwitch().isChecked()));

            Log.d(TAG, getOptionMessage("crashSensor", optionSwitches.getCrashSwitch().isChecked()));
            Log.d(TAG, getOptionMessage("blindspotSensor", optionSwitches.getBlindspotSwitch().isChecked()));
            Log.d(TAG, getOptionMessage("hud", optionSwitches.getHudSwitch().isChecked()));
        }


        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                try {
                    numBytes = mmInStream.read(mmBuffer);
                }
                catch (NullPointerException e) {
                    Log.w(TAG, "mmInStream is null");
                }  catch (IOException e) {
//                    Log.d(TAG, "Bluetooth socket was closed");
                    mmOutStream = null;
                    mmInStream = null;
                    cancel();

                    break;
                }

                String msg = new String(mmBuffer, "UTF-8").substring(0,numBytes);

                Log.d("ConnectedThread", msg);
                // Handle message
                if ("Crash".equals(msg)){


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long[] pattern = {0, 3000, 10};
                            int[] amplitude = {255, 255, 255};

                            if (Build.VERSION.SDK_INT >= 26) {
                                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitude, 0));
                            } else {
                                vibrator.vibrate(150);
                            }

                            AlertDialog.Builder builder;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                builder = new AlertDialog.Builder(m_activity, android.app.AlertDialog.THEME_TRADITIONAL);
                            } else {
                                builder = new AlertDialog.Builder(m_activity);
                            }
                            builder.setTitle("Accident Detected")
                                    .setMessage("Send emergency message? (Will automatically send in 30 seconds)")
                                    .setPositiveButton("Yes", (dialog, which) -> {
                                        sendEmergencyMessages();
                                        vibrator.cancel();
                                    })
                                    .setNegativeButton(android.R.string.no, (dialog, which) -> {vibrator.cancel();})
                                    .setIcon(android.R.drawable.ic_dialog_alert);

                            final AlertDialog alert = builder.create();
                            alert.setCanceledOnTouchOutside(false);
                            alert.setCancelable(false);
                            alert.show();

                            Handler handler = new Handler();
                            handler.postDelayed(() -> {
                                if (alert.isShowing()) {
                                    alert.dismiss();
                                    sendEmergencyMessages();
                                    vibrator.cancel();
                                }
                            }, 30000);
                        }
                    });

                }
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                break;
            }

        }
    }

    private void sendEmergencyMessages() {
        StringBuilder sb = new StringBuilder();
        GeoCoordinate coordinate = mapFragmentView.getCoordinate();
        sb.append("EMERGENCY: I HAVE BEEN INVOLVED IN A MOTORCYCLE ACCIDENT. LATITUDE: ");
        sb.append(coordinate.getLatitude());
        sb.append(" LONGITUDE: ");
        sb.append(coordinate.getLongitude());
        sb.append(" THIS IS AN AUTOMATED MESSAGE");

        Cursor data = mDatabaseHelper.getPhoneNumber();

        while(data.moveToNext()){
            smsManager.sendTextMessage(data.getString(0), null, sb.toString(), null, null);
            Log.d(TAG, "Message sent to :" + data.getString(0) ); //data.getString(0) // this is the number
        }
    }

    // Call this from the main activity to send data to the remote device.
    public void write(String msg) {
        try {
            mmOutStream.write(msg.getBytes());

        } catch (Exception e) {
            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            Message writeErrorMsg =
                    mHandler.obtainMessage(MainActivity.MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            mHandler.sendMessage(writeErrorMsg);
        }
    }

    // Call this method from the main activity to shut down the connection.
    public void cancel() {
        try {
            socket.close();

        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }

    public void setOptionSwitches(OptionSwitches optionSwitches) {
        this.optionSwitches = optionSwitches;
    }

    private String getOptionMessage(String str, boolean on) {
        JSONObject json = new JSONObject();

        try {
            json.put(str, on);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    public void setHelmetFragment(HelmetFragment helmetFragment) {
        this.helmetFragment = helmetFragment;
    }

    public boolean isConnected() {
        return mmOutStream != null;
    }
}
package com.apollo.apollo;


import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 */
public class HelmetFragment extends androidx.fragment.app.Fragment {

    private static final String TAG = "HelmetFragment";

    private TextView connectionStatus;
    private Button connectionButton;
    private Switch rearviewSwitch, blindspotSwitch, navigationSwitch, crashSwitch;
    private boolean hasInflated = false;
    private BluetoothConnectionStatus btConnectionStatus;
    private ConnectedThreadHolder connectedThreadHolder;

    public HelmetFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_helmet, container, false);

        connectionStatus = view.findViewById(R.id.connectionStatus);
        connectionButton = view.findViewById(R.id.connectionButton);

        rearviewSwitch = view.findViewById(R.id.rearviewSwitch);
        blindspotSwitch = view.findViewById(R.id.blindspotSwitch);
        navigationSwitch = view.findViewById(R.id.navigationSwitch);
        crashSwitch = view.findViewById(R.id.crashSwitch);

        rearviewSwitch.setChecked(true);
        blindspotSwitch.setChecked(true);
        navigationSwitch.setChecked(true);
        crashSwitch.setChecked(true);

        rearviewSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (connectedThreadHolder.isConnected()) {
                connectedThreadHolder
                        .getConnectedThread()
                        .write(getOptionMessage("rearviewFeed", isChecked));
                Log.d(TAG, getOptionMessage("rearviewFeed", isChecked));
            }
            else {
                Log.d(TAG, "Bluetooth not connected");
            }
        });

        blindspotSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (connectedThreadHolder.isConnected()) {
                connectedThreadHolder
                        .getConnectedThread()
                        .write(getOptionMessage("blindspotSensor", isChecked));
                Log.d(TAG, getOptionMessage("blindspotSensor", isChecked));
            }
            else {
                Log.d(TAG, "Bluetooth not connected");
            }
        }));

        navigationSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (connectedThreadHolder.isConnected()) {
                connectedThreadHolder
                        .getConnectedThread()
                        .write(getOptionMessage("navigationFeed", isChecked));
                Log.d(TAG, getOptionMessage("navigationFeed", isChecked));
            }
            else {
                Log.d(TAG, "Bluetooth not connected");
            }
        }));

        crashSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (connectedThreadHolder.isConnected()) {
                connectedThreadHolder
                        .getConnectedThread()
                        .write(getOptionMessage("crashSensor", isChecked));
                Log.d(TAG, getOptionMessage("crashSensor", isChecked));
            }
            else {
                Log.d(TAG, "Bluetooth not connected");
            }
        }));

        handleSatus();

        if (connectedThreadHolder.getConnectedThread() != null) {
            connectedThreadHolder.getConnectedThread().setHelmetFragment(this);
        }

        hasInflated = true;
        return view;
    }
    
    public void handleDisconnect() {
        connectionButton.setEnabled(true);

        blindspotSwitch.setEnabled(false);
        rearviewSwitch.setEnabled(false);
        navigationSwitch.setEnabled(false);
        crashSwitch.setEnabled(false);

        connectionStatus.setText(R.string.helmet_disconnected);
        connectionButton.setText(R.string.connect);
    }

    public void handleScan() {
        connectionButton.setEnabled(false);

        blindspotSwitch.setEnabled(false);
        rearviewSwitch.setEnabled(false);
        navigationSwitch.setEnabled(false);
        crashSwitch.setEnabled(false);

        connectionStatus.setText(R.string.searching_for_helmet);
        connectionButton.setText(R.string.connect);
    }

    public void handleConnect() {
        connectionButton.setEnabled(true);

        blindspotSwitch.setEnabled(true);
        rearviewSwitch.setEnabled(true);
        navigationSwitch.setEnabled(true);
        crashSwitch.setEnabled(true);

        if (connectedThreadHolder.getConnectedThread() != null) {
            ConnectedThread connectedThread = connectedThreadHolder.getConnectedThread();

            connectedThread.setOptionSwitches(new OptionSwitches(rearviewSwitch,
                    navigationSwitch,
                    blindspotSwitch,
                    crashSwitch));
        }
        else {
            Log.d(TAG, "Bluetooth not connected");
        }

        connectionStatus.setText(R.string.helmet_connected);
        connectionButton.setText(R.string.disconnect);
    }


    private void handleSatus() {
        if (btConnectionStatus.getBluetoothAdapter() != null
                && btConnectionStatus.getBluetoothAdapter().isDiscovering()) {
            handleScan();
        } else if (btConnectionStatus.getConnectedThreadHolder() != null
                && btConnectionStatus.getConnectedThreadHolder().isConnected()) {
            handleConnect();
        } else if (btConnectionStatus.getConnectedThreadHolder() != null) {
            handleDisconnect();
        }
    }

    public void setBtConnectionStatus(BluetoothConnectionStatus btConnectionStatus) {
        this.btConnectionStatus = btConnectionStatus;
    }

    public void setConnectedThreadHolder(ConnectedThreadHolder connectedThreadHolder) {
        this.connectedThreadHolder = connectedThreadHolder;
    }

    private String getOptionMessage(String str, boolean on) {
        JSONObject json = new JSONObject();

        try {
            json.put("optionMessage", new JSONObject().put(str, on));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    public boolean hasInflated() {
        return hasInflated;
    }
}

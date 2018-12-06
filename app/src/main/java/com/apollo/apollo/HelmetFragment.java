package com.apollo.apollo;


import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AlertDialog;


/**
 * A simple {@link Fragment} subclass.
 */
public class HelmetFragment extends androidx.fragment.app.Fragment {

    private static final String TAG = "HelmetFragment";

    private TextView connectionStatus;
    private Button connectionButton;
    private Switch hudSwitch, blindspotSwitch, crashSwitch;
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

        hudSwitch = view.findViewById(R.id.hudSwitch);
        blindspotSwitch = view.findViewById(R.id.blindspotSwitch);
        crashSwitch = view.findViewById(R.id.crashSwitch);

        hudSwitch.setChecked(true);
        blindspotSwitch.setChecked(true);
        crashSwitch.setChecked(true);

        hudSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (connectedThreadHolder.isConnected()) {
                connectedThreadHolder
                        .getConnectedThread()
                        .write(getOptionMessage("hud", isChecked));
                Log.d(TAG, getOptionMessage("hud", isChecked));
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

        crashSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {

            if (!isChecked) {
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(getActivity(), android.app.AlertDialog.THEME_TRADITIONAL);
                } else {
                    builder = new AlertDialog.Builder(getActivity());
                }
                builder.setTitle("Warning")
                        .setMessage("In the event of an accident, emergency services and" +
                                " contacts will not be notified. Are you sure" +
                                " you want to turn this off?")
                        .setNegativeButton(android.R.string.no, (dialog, which) -> {
                            crashSwitch.setChecked(true);
                        })
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            if (connectedThreadHolder.isConnected()) {
                                connectedThreadHolder
                                        .getConnectedThread()
                                        .write(getOptionMessage("crashSensor", isChecked));
                                Log.d(TAG, getOptionMessage("crashSensor", isChecked));
                            }
                            else {
                                Log.d(TAG, "Bluetooth not connected");
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert);

                final AlertDialog alert = builder.create();
                alert.setCanceledOnTouchOutside(false);
                alert.setCancelable(false);
                alert.show();
            }
        }));

        handleStatus();

        if (connectedThreadHolder.getConnectedThread() != null) {
            connectedThreadHolder.getConnectedThread().setHelmetFragment(this);
        }

        hasInflated = true;
        return view;
    }
    
    public void handleDisconnect() {
        connectionButton.setEnabled(true);

        blindspotSwitch.setEnabled(false);
        hudSwitch.setEnabled(false);
        crashSwitch.setEnabled(false);

        connectionStatus.setText(R.string.helmet_disconnected);
        connectionButton.setText(R.string.connect);
    }

    public void handleScan() {
        connectionButton.setEnabled(false);

        blindspotSwitch.setEnabled(false);
        hudSwitch.setEnabled(false);
        crashSwitch.setEnabled(false);

        connectionStatus.setText(R.string.searching_for_helmet);
        connectionButton.setText(R.string.connect);
    }

    public void handleConnect() {
        connectionButton.setEnabled(true);

        blindspotSwitch.setEnabled(true);
        hudSwitch.setEnabled(true);
        crashSwitch.setEnabled(true);

        if (connectedThreadHolder.getConnectedThread() != null) {
            ConnectedThread connectedThread = connectedThreadHolder.getConnectedThread();

            connectedThread.setOptionSwitches(new OptionSwitches(hudSwitch,
                    blindspotSwitch,
                    crashSwitch));
        }
        else {
            Log.d(TAG, "Bluetooth not connected");
        }

        connectionStatus.setText(R.string.helmet_connected);
        connectionButton.setText(R.string.disconnect);
    }


    private void handleStatus() {
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
            json.put(str, on);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    public boolean hasInflated() {
        return hasInflated;
    }
}

package com.apollo.apollo;


import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
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
    private SeekBar hudBrightness;

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
        hudBrightness = view.findViewById(R.id.hudBrightness);

        hudSwitch.setChecked(true);
        blindspotSwitch.setChecked(true);
        crashSwitch.setChecked(true);
        hudBrightness.setMax(100);
        hudBrightness.setProgress(100);

        hudSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sendOptionMessage("hud", isChecked);

            if (!isChecked) {
                hudBrightness.setEnabled(false);
            } else {
                hudBrightness.setEnabled(true);
            }
        });

        blindspotSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            sendOptionMessage("blindspotSensor", isChecked);
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
                            sendOptionMessage("crashSensor", isChecked);
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert);

                final AlertDialog alert = builder.create();
                alert.setCanceledOnTouchOutside(false);
                alert.setCancelable(false);
                alert.show();
            }
        }));

        hudBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int brightness = 0;
            int progressChangeCount = 0; // Counts onProgressChange events

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                brightness = progress;
                progressChangeCount++;

                if (progressChangeCount == 10) {
                    sendBrightnessMessage(scaleBrightness(brightness));
                    progressChangeCount = 0;
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendBrightnessMessage(scaleBrightness(brightness));

                progressChangeCount = 0;
            }
        });



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
        hudBrightness.setEnabled(false);

        connectionStatus.setText(R.string.helmet_disconnected);
        connectionButton.setText(R.string.connect);
    }

    public void handleScan() {
        connectionButton.setEnabled(false);

        blindspotSwitch.setEnabled(false);
        hudSwitch.setEnabled(false);
        crashSwitch.setEnabled(false);
        hudBrightness.setEnabled(false);

        connectionStatus.setText(R.string.searching_for_helmet);
        connectionButton.setText(R.string.connect);
    }

    public void handleConnect() {
        connectionButton.setEnabled(true);

        blindspotSwitch.setEnabled(true);
        hudSwitch.setEnabled(true);
        crashSwitch.setEnabled(true);

        if (!blindspotSwitch.isChecked()) {
            hudBrightness.setEnabled(false);
        } else {
            hudBrightness.setEnabled(true);
        }

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

    private int scaleBrightness(int brightness) {
        if (brightness < 33) {
            return (int) Math.round(Math.pow(brightness, .6));
        }
        else if (brightness < 66) {
            return Math.round(brightness - 25);
        }
        else {
            return (int) Math.round(1.73 * brightness - 73);
        }
    }

    private String getBrightnessMessage(String str, int brightness) {
        JSONObject json = new JSONObject();

        try {
            json.put(str, scaleBrightness(brightness));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    private boolean sendOptionMessage(String option, boolean isChecked) {
        if (connectedThreadHolder.isConnected()) {
            connectedThreadHolder
                    .getConnectedThread()
                    .write(getOptionMessage(option, isChecked));
            Log.d(TAG, getOptionMessage(option, isChecked));
            return true;
        }
        else {
            Log.d(TAG, "Bluetooth not connected");
        }

        return false;
    }

    private boolean sendBrightnessMessage(int brightness) {
        if (connectedThreadHolder.isConnected()) {
            connectedThreadHolder
                    .getConnectedThread()
                    .write(getBrightnessMessage("hudBrightness", brightness));
            Log.d(TAG, getBrightnessMessage("hudBrightness", brightness));
            return true;
        }
        else {
            Log.d(TAG, "Bluetooth not connected");
        }

        return false;
    }

    public boolean hasInflated() {
        return hasInflated;
    }
}

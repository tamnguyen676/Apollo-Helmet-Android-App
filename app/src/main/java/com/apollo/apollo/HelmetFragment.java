package com.apollo.apollo;


import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class HelmetFragment extends androidx.fragment.app.Fragment {

    private TextView connectionStatus;
    private Button connectionButton;
    private Switch rearviewSwitch, blindspotSwitch, navigationSwitch, crashSwitch;
    private boolean hasInflated = false;
    private BluetoothConnectionStatus btConnectionStatus;

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
        // Inflate the layout for this fragment
        
        handleSatus();

        hasInflated = true;
        return view;
    }
    
    public void handleDisconnect() {
        connectionButton.setEnabled(true);
        connectionStatus.setText("Apollo Helmet Disconnected");
        connectionButton.setText("CONNECT");
    }

    public void handleScan() {
        connectionStatus.setText("Searching for Apollo Helmet");
        connectionButton.setText("CONNECT");
        connectionButton.setEnabled(false);
    }

    public void handleConnect() {
        connectionButton.setEnabled(true);
        connectionStatus.setText("Apollo Helmet Connected");
        connectionButton.setText("DISCONNECT");
    }

    public void setBtConnectionStatus(BluetoothConnectionStatus btConnectionStatus) {
        this.btConnectionStatus = btConnectionStatus;
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

    public boolean hasInflated() {
        return hasInflated;
    }
}

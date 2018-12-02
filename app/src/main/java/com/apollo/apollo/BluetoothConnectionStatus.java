package com.apollo.apollo;

import android.bluetooth.BluetoothAdapter;

public final class BluetoothConnectionStatus {
    private BluetoothAdapter btAdapter;
    private ConnectedThreadHolder connectedThreadHolder;

    public void setBluetoothAdapter(BluetoothAdapter btAdapter) {
        this.btAdapter = btAdapter;
    }

    public void setConnectedThreadHolder(ConnectedThreadHolder connectedThreadHolder) {
        this.connectedThreadHolder = connectedThreadHolder;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return btAdapter;
    }

    public ConnectedThreadHolder getConnectedThreadHolder() {
        return connectedThreadHolder;
    }
}

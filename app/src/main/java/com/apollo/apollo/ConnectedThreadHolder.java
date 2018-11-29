package com.apollo.apollo;

public class ConnectedThreadHolder {
    private ConnectedThread connectedThread;

    public ConnectedThread getConnectedThread() {
        return connectedThread;
    }

    public void setConnectedThread(ConnectedThread connectedThread) {
        this.connectedThread = connectedThread;
    }
}

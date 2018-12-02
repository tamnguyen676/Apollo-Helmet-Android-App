package com.apollo.apollo;

public class ConnectedThreadHolder {
    private ConnectedThread connectedThread;

    public ConnectedThread getConnectedThread() {
        return connectedThread;
    }

    public void setConnectedThread(ConnectedThread connectedThread) {
        this.connectedThread = connectedThread;
    }

    public boolean isConnected() {
        return connectedThread != null;
    }

    public void endConnection() {
        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }
    }
}

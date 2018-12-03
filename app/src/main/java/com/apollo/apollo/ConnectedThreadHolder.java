package com.apollo.apollo;

public final class ConnectedThreadHolder {
    private ConnectedThread connectedThread;

    public ConnectedThread getConnectedThread() {
        return connectedThread;
    }

    public void setConnectedThread(ConnectedThread connectedThread) {
        this.connectedThread = connectedThread;
    }

    public boolean isConnected() {
        return connectedThread != null && connectedThread.isConnected();
    }

    public void endConnection() {
        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }
    }
}

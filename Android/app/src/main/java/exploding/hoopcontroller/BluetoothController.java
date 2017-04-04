package exploding.hoopcontroller;

import android.content.Intent;
import android.graphics.Color;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Created by alexh on 4/3/2017.
 */
public class BluetoothController {

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = "90:CD:B6:35:16:4C";

    public BluetoothController(BluetoothAdapter adapter) {
        btAdapter = adapter;
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    public void resume() {
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create socket");
        }

        btAdapter.cancelDiscovery();

        Log.d(TAG, "Connecting to bluetooth adapter");
        try {
            btSocket.connect();
            Log.d(TAG, "Connected to bluetooth adapter");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException io2) {
                Log.e(TAG, io2.getMessage());
            }
        }

        Log.d(TAG, "Creating bluetooth socket");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void pause() {
        if(outStream != null) {
            try {
                outStream.flush();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        try {
            btSocket.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void setColor(int color) {
        byte red = (byte) Color.red(color);
        byte green = (byte) Color.green(color);
        byte blue = (byte) Color.blue(color);

        byte[] buff = {0, red, green, blue};
        sendData(buff);
    }

    public void setEffect(int effect) {
        byte[] buff = {1, (byte) effect};
        sendData(buff);
    }

    private void sendData(byte[] msgBuffer) {
        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            Log.e(TAG,"Fatal Error " + msg);
        }
    }
}

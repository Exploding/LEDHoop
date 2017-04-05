package exploding.hoopcontroller;

import android.graphics.Color;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Created by alexh on 4/3/2017.
 */
public class BluetoothController {

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Hoop's device names
    private ArrayList<String> deviceNames;

    public BluetoothController(BluetoothAdapter adapter) {
        btAdapter = adapter;

        deviceNames = new ArrayList<>();
        deviceNames.add("EXPLODING");
        deviceNames.add("HC-05");
        deviceNames.add("HC-12");
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

    /**
     * Attempts to re-establish the bluetooth connection after app resumes
     */
    public void resume() {
        BluetoothDevice device = null;
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        if (devices != null) {
            for (BluetoothDevice blDevice : devices) {
                if (deviceNames.contains(blDevice.getName())) {
                    device = blDevice;
                    break;
                }
            }
        }

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create bluetooth socket", e);
        }

        btAdapter.cancelDiscovery();

        Log.d(TAG, "Connecting to bluetooth adapter...");
        try {
            btSocket.connect();
            Log.d(TAG, "Connected to bluetooth adapter");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException io2) {
                Log.e(TAG, "Failed to connect to bluetooth adapter", io2);
            }
        }

        Log.d(TAG, "Creating bluetooth socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Failed to create bluetooth socket", e);
        }
    }

    /**
     * Attempts to flush the output stream and close the bluetooth connection when the app is suspended
     */
    public void pause() {
        if(outStream != null) {
            try {
                outStream.flush();
            } catch (Exception e) {
                Log.e(TAG, "Failed to flush output stream", e);
            }
        }

        try {
            btSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to close bluetooth socket", e);
        }
    }

    /**
     * Sends the set color command
     * @param color
     */
    public void setColor(int color) {
        byte red = (byte) Color.red(color);
        byte green = (byte) Color.green(color);
        byte blue = (byte) Color.blue(color);

        byte[] buff = {0, red, green, blue};
        sendData(buff);
    }

    /**
     * Sends the set effect command
     * @param effect
     */
    public void setEffect(int effect) {
        byte[] buff = {1, (byte) effect};
        sendData(buff);
    }

    /**
     * Attempts to send a message to the bluetooth module
     * @param msgBuffer
     */
    private void sendData(byte[] msgBuffer) {
        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            Log.e(TAG,"Failed to send data ", e);
        }
    }
}

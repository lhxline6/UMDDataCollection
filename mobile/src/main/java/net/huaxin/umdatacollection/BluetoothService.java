package net.huaxin.umdatacollection;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Qianyi on 7/28/2016.
 */
public class BluetoothService extends Service {

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String BROADCAST_ACTION = "com.websmithing.broadcasttest.displayevent";
    BluetoothAdapter btAdapter = null;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private boolean stopThread;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Toast.makeText(getApplicationContext(),"Service started",0).show();
//        return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(),"Service Stop",0).show();
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        if (connectThread != null) {
            connectThread.cancel();
        }
    }

    public void connectDevice(BluetoothDevice device, Context context) {
        connectThread = new ConnectThread(device, context);
        connectThread.start();
    }

    public void disconnectAll() {
        stopThread = true;
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        if (connectThread != null) {
            connectThread.cancel();
        }
    }

//    Handler mHandler = new Handler(){
//
//        @Override
//        public void handleMessage(Message msg) {
//            String readMessage = (String) msg.obj;
//            Log.v("msg", readMessage);
//            Intent broadcastIntent = new Intent(BROADCAST_ACTION);
//            broadcastIntent.putExtra("INFO", msg);
//            sendBroadcast(broadcastIntent);
//        }
//    };

    //********************************* CONNECT THREAD *********************************************
    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private Context mmContext;

        public ConnectThread(BluetoothDevice device, Context context) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            mmContext = context;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
//            if(btAdapter.isDiscovering()) {
//                btAdapter.cancelDiscovery();
//            }
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.v("SOCKET", "SOCKET GOOD");
                connectedThread = new ConnectedThread(mmSocket, mmContext);
                connectedThread.run();

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
//                Toast.makeText(getApplicationContext(), "Device Failed to Connect", Toast.LENGTH_SHORT).show();
                Log.d("CONNECT EXCEPTION", String.valueOf(connectException));
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                // Do work to manage the connection (in a separate thread)
                return;
            }

//            // Do work to manage the connection (in a separate thread)
//            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
//            connectedThread = new ConnectedThread(mmSocket);
//            connectedThread.run();
        }


        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
//********************************* END CONNECT THREAD *****************************************

    //******************************** CONNECTED THREAD ********************************************
    public class ConnectedThread extends Thread {
        private static final int MESSAGE_READ = 0;
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private Context mmContext;
        public ConnectedThread(BluetoothSocket socket, Context context) {
            mmSocket = socket;
            this.mmContext = context;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()
            Intent broadcastIntent = new Intent(BROADCAST_ACTION);
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    Log.d("Steve", "Receiving...");
                    buffer = new byte[256];
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer,0,bytes);

                    try {
                        org.json.JSONObject object = new org.json.JSONObject(new String(buffer,0,bytes));
                        broadcastIntent.putExtra("INFO", object.get("name") + ": " + object.get("value"));
                        mmContext.sendBroadcast(broadcastIntent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
//***************************** END CONNECTED THREAD *******************************************
}



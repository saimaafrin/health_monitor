package me.isty.icumonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "bluetooth";
    private ConnectedThread mConnectedThread;
    final int RECIEVE_MESSAGE = 1;
    Handler h;
    private StringBuilder sb = new StringBuilder();
    private BluetoothAdapter btAdapter = null;
    private ListView lstvw;
    private ArrayAdapter aAdapter;
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayList list = new ArrayList();
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    public static String address = "00:18:E4:0A:00:01"; //homee conecttion mac adresss
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    TextView textView,bloodText,bPressureText,tempText;

    public String MY_PREFS_NAME ="ADDRESS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = (Button)findViewById(R.id.btnGet);
        textView = (TextView) findViewById(R.id.textResult);
        bloodText = (TextView) findViewById(R.id.bloodText);
        bPressureText = (TextView) findViewById(R.id.bPressureText);
        tempText = (TextView) findViewById(R.id.tempText);
        lstvw = (ListView) findViewById(R.id.deviceList);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bAdapter==null){
                    Toast.makeText(getApplicationContext(),"Bluetooth Not Supported",Toast.LENGTH_SHORT).show();
                }
                else{

                    if(lstvw.getVisibility()==View.GONE){lstvw.setVisibility(View.VISIBLE);}else{lstvw.setVisibility(View.GONE);}

                    Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();
                    list = new ArrayList();
                    if(pairedDevices.size()>0){
                        for(BluetoothDevice device: pairedDevices){
                            String devicename = device.getName();
                            String macAddress = device.getAddress();
                            list.add("Name: "+devicename+"\nMAC Address= "+macAddress);
                        }

                        aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list){

                            @Override
                            public View getView(int position,View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView text = (TextView) view.findViewById(android.R.id.text1);
                                text.setTextColor(Color.BLACK);
                                return view;
                            }
                        };



                        lstvw.setAdapter(aAdapter);

                        lstvw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                                String itm ="";itm= list.get(i).toString();
                                if(!itm.equals("")){
                                    address = itm.split("=")[1].trim();

                                    saveToMemory(address);

                                    Toast.makeText(MainActivity.this, "Selected: "+itm.split("\n")[0].split(":")[1].trim(), Toast.LENGTH_SHORT).show();

                                    connectDevice();

                                    lstvw.setVisibility(View.GONE);

                                }
                                }
                        });

                    }
                }
            }
        });

        getAdressMemory();
        startGetting();
        btAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter
        checkBTState();
    }


    public void saveToMemory(String address){

        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("address", address);
        editor.apply();
    }

    public void getAdressMemory(){

        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String restoredText = prefs.getString("address", null);
        if (restoredText != null) {
            address = restoredText;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();


      connectDevice();

    }


    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mConnectedThread!=null){
            try{mConnectedThread.stop();}catch (Exception e){}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mConnectedThread!=null){
            try{mConnectedThread.stop();}catch (Exception e){}
        }
    }


    public void connectDevice(){

        getAdressMemory();
        if(address!=null){
            Log.d(TAG, "...onResume - try connect...");
            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) { }
            btAdapter.cancelDiscovery();
            Log.d(TAG, "...Connecting...");
            Toast.makeText(getApplicationContext(),"Connecting...",Toast.LENGTH_LONG).show();
            try {
                btSocket.connect();
                Log.d(TAG, "....Connection ok...");
                Toast.makeText(getApplicationContext(),"Connected...",Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) { }
            }
            Log.d(TAG, "...Create Socket...");

            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();
        }

    }

    public void startGetting(){

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:													// if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        System.out.println("------strIncom----------"+strIncom);
                        // create string from bytes array
                        sb.append(strIncom);												// append string
                        int endOfLineIndex = sb.indexOf("\r\n");							// determine the end-of-line
                        if (endOfLineIndex > 0) { 											// if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);				// extract string
                            sb.delete(0, sb.length());

                            // and clear

                            try {

                                JSONObject obj = new JSONObject(sbprint);

                                try{bloodText.setText(obj.getString("bpm")+"");}catch(Exception e){}
                                try{tempText.setText(obj.getString("tmp")+" °C");}catch(Exception e){}
                                try{bPressureText.setText(obj.getString("mmhg")+"");}catch(Exception e){}

                                Log.d("My App", obj.toString());

                            } catch (Throwable t) {
                                Log.e("My App", "Could not parse malformed JSON: \"" + sbprint + "\"");
                            }

                            textView.setText(sbprint);

                            System.out.println("----------------"+sbprint);

                        }

                        break;
                }
            };
        };

    }


    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            //errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);

            }
        }
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






    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }
}

package com.deathbylogic.gypsy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

interface ICallback {
    void callback();
}

public class Drive extends ActionBarActivity implements View.OnTouchListener, View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener, ICallback {
    private float cur_x, start_x;
    private float cur_y, start_y;

    private int speed_multiplier;
    private int dir_multiplier;

    private int speed;
    private int direction;

    private boolean moveEvent = false;

    MenuItem mnu_connect;
    MenuItem mnu_settings;
    MenuItem mnu_enabled;

    OurView v;
    Remote c;
    Thread t = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);

        // Populate field with default values from preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(this);

        // Get Settings
        String serverIPAddress = settings.getString("server_ip", "localhost");
        String serverPort = settings.getString("server_port", "1988");
        speed_multiplier = Integer.parseInt(settings.getString("max_speed", "100"));
        dir_multiplier = Integer.parseInt(settings.getString("turn_speed", "100"));

        // Setup onTouchListener for View
        v = new OurView(this);
        v.setOnTouchListener(this);
        setContentView(v);

        // New Remote object
        c = new Remote(this, serverIPAddress, serverPort);
        t = new Thread(c);
        t.start();

        // Check network status
        if (!isNetworkAvailable()) {
            // Toast message here
            //Toast.makeText(MainActivity.this, "We have connectivity", Toast.LENGTH_SHORT).show();

            // Alert dialog here
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("You have no network/internet access. Please try again later. This program will now exit.")
                    .setCancelable(false)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int id) {
                            Drive.this.finish();
                        }
                    });
            AlertDialog alert = builder.create();
            // builder.show();
            alert.show();
            // End alert dialog here
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_drive, menu);

        mnu_connect = menu.findItem(R.id.action_connect);
        mnu_settings = menu.findItem(R.id.action_settings);
        mnu_enabled = menu.findItem(R.id.action_enable);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                // Creates a new intent to open up our settings activity (screen/page)
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);

                return true;
            case R.id.action_connect:
                if (c.isConnected()) {
                    c.disconnect();
                } else {
                    c.connect();
                    t.run();
                }

                return true;
            case R.id.action_enable:
                if (c.isConnected()) {
                    if (c.isRemoteEnabled()) {
                        c.disableRemote();
                    } else {
                        c.enableRemote();
                    }
                } else {
                    Toast.makeText(Drive.this, "You must be connected to enable remote.", Toast.LENGTH_LONG).show();
                }

                return true;
            default:
                // Do nothing
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        c.serverIpAddress = settings.getString("server_ip", "localhost");
        c.serverPort = settings.getString("server_port", "1988");

        speed_multiplier = Integer.parseInt(settings.getString("max_speed", "100"));
        dir_multiplier = Integer.parseInt(settings.getString("turn_speed", "100"));
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                start_x = cur_x = event.getX();
                start_y = cur_y = event.getY();

                moveEvent = true;

                break;
            case MotionEvent.ACTION_UP:
                speed = 0;
                direction = 0;

                moveEvent = false;

                break;
            case MotionEvent.ACTION_MOVE:
                cur_x = event.getX();
                cur_y = event.getY();

                Float xLength = cur_x - start_x;
                Float yLength = cur_y - start_y;

                Double rawSpeed = Math.sqrt(Math.pow(cur_x - start_x, 2) + Math.pow(cur_y - start_y, 2));
                Double rawDir = Math.toDegrees(-Math.atan2(start_x - cur_x, start_y - cur_y));

                // Calculate speed based on raw speed length
                if (rawSpeed > 200) {
                    if (((rawSpeed - 200) / (v.getWidth() / 4) * 100) > 100) {
                        speed = speed_multiplier;
                    } else {
                        speed = (int)((rawSpeed - 200) / (v.getWidth() / 4) * speed_multiplier);
                    }
                } else {
                    speed = 0;
                }

                if (rawSpeed > 50) {
                    if (Math.abs(rawDir) > 100) {
                        direction = (rawDir > 0)?dir_multiplier:-dir_multiplier;
                    } else {
                        direction = (int)(rawDir * ((float)dir_multiplier / 100.0));
                    }
                } else {
                    direction = 0;
                }

                break;
        }

        if (c.isConnected()) {
            c.setSpeed((byte) speed);
            c.setDirection((byte) direction);
        }

        return true;
    }

    public void onClick(View v) {
        // TODO Auto-generated method stub
        /*if (v.getId() == R.id.btnConnect) {
            // All code from below goes here!!!!

            if (!connected) { // IF we're not connected at the moment, then let's connect!
               if (!serverIpAddress.equals("")) {
                    cThread = new Thread(c);
                    cThread.start();
                    btnConnect.setText(R.string.connecting);
                                   }
            } else {
                Log.d("ClientActivity", "Disconnect button pressed");
                myAtomicButtonInteger.getAndSet(4);
                Log.d("ClientActivity", "(After) tmpAtomic=" + myAtomicButtonInteger.get());

                //btnConnect.setText(R.string.connect); // Change the text of the connect/disconnect button back to 'Connect'

                connected = false;
            }
        }*/
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        v.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        v.pause();
    }

    @Override
    public void callback() {
        if (c.isConnected()) {
            mnu_connect.setTitle(R.string.action_disconnect);
        } else {
            mnu_connect.setTitle(R.string.action_connect);
        }

        if (c.isRemoteEnabled()) {
            mnu_enabled.setTitle(R.string.action_remote_disable);
        } else {
            mnu_enabled.setTitle(R.string.action_remote_enable);
        }
    }

    public class OurView extends SurfaceView implements Runnable {
        Thread t = null;
        SurfaceHolder holder;
        boolean isItOK = false;

        public OurView(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
            holder = getHolder();
        }

        public void run() {
            // TODO Auto-generated constructor stub
            while (isItOK == true) {
                // perform canvas drawing
                if (!holder.getSurface().isValid()) {
                    continue;
                }

                Canvas c = holder.lockCanvas();

                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(5);
                paint.setAntiAlias(true);
                paint.setTextSize(50);

                c.drawARGB(255, 150, 150, 150);
                c.drawText("Speed: " + Integer.toString(speed), 10, 50, paint);
                c.drawText("Direction: " + Integer.toString(direction), 10, 100, paint);

                if (moveEvent == true) {
                    c.drawCircle(start_x, start_y, 50, paint);

                    paint.setStyle(Paint.Style.STROKE);
                    c.drawCircle(start_x, start_y, 200, paint);

                    c.drawLine(start_x, start_y, cur_x, cur_y, paint);
                }

                holder.unlockCanvasAndPost(c);
            }
        }

        public void pause() {
            isItOK = false;
            while(true){
                try{
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                break;
            }

            t = null;
        }

        public void resume() {
            isItOK = true;
            t = new Thread(this);
            t.start();
        }
    } // End of class OurView

    public class Remote implements Runnable {
        private Socket socket;// = new Socket();
        private Handler handler = new Handler();
        private boolean connected = false;
        private boolean isItOK = false;
        public String serverIpAddress;
        public String serverPort;
        private byte config;
        private ICallback ic;

        Remote(ICallback ic, String IPAddress, String Port) {
            this.ic = ic;

            serverIpAddress = IPAddress;
            serverPort = Port;
        }

        public void run() {
            //connect();
        } // End of run()

        public void pause() {

        }

        public void resume() {
            t = new Thread(this);
            t.start();
        }

        private void updateUI() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ic.callback();
                }
            });
        }

        public void setServer(String address, String port) {
            serverIpAddress = address;
            serverPort = port;
        }

        public boolean isConnected() {
            return connected;
        }

        // Open socket
        public void connect() {
            try {
                Log.d("Remote", "Net: Connecting");

                socket = new Socket();
                SocketAddress adr = new InetSocketAddress(serverIpAddress, Integer.parseInt(serverPort));
                socket.connect(adr, 5000); // 2nd parameter is timeout!!!

                socket.setSoTimeout(100);

                connected = true;

                Log.d("Remote", "Net: Connected");

                updateUI();

                while (connected) {
                    try {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                connected = socket.isConnected();
                            }
                        });
                    } catch (Exception e) {
                        connected = false;

                        Log.e("Remote", "Error", e);
                    }
                }

                socket.close();

                Log.d("Remote", "Net: Closed");
                updateUI();

                // Timeout catch
            } catch (java.net.SocketTimeoutException ste) {
                // Do something here to notify user
                connected = false;
                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(Drive.this, "Connection timed out! Please check your internet connection, and address/port settings", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e("Remote", "Error", e);
                connected = false;
            }
        }

        // Close socket
        public void disconnect() {
            connected = false;
        }

        public void enableRemote() {
            // Enable remote in config byte
            config |= 0x01;

            setConfig();

            updateUI();
        }

        public void disableRemote() {
            config &= ~(0x01);

            setConfig();

            updateUI();
        }

        public boolean isRemoteEnabled() {
            if ((config & 0x01) > 0) {
                return true;
            } else {
                return false;
            }
        }

        public void setConfig() {
            byte[] cmd = new byte[4];

            cmd[0] = 0x42;
            cmd[1] = config;

            transmit(cmd);
        }

        public void setSpeed(byte speed) {
            byte[] cmd = new byte[4];

            cmd[0] = 0x51;
            cmd[1] = speed;

            transmit(cmd);
        }

        public void setDirection(byte dir) {
            byte[] cmd = new byte[4];

            cmd[0] = 0x52;
            cmd[1] = dir;

            transmit(cmd);
        }

        //
        // Support Functions
        //
        private void transmit(byte[] args) {
            CRC crcHandler = new CRC();

            try {
                OutputStream out = socket.getOutputStream();

                crcHandler.update(Arrays.copyOfRange(args, 0, args.length - 2));

                args[args.length - 2] = (byte)(crcHandler.getCRC() >> 8 & 0xFF);
                args[args.length - 1] = (byte)(crcHandler.getCRC()      & 0xFF);

                out.write(args, 0, args.length);
            } catch (Exception e) {
                Log.e("Remote", "Transmit Error", e);
                connected = false;
            }
        }

        private class CRC {
            public final static int polynomial = 0x1021;	// Represents x^16+x^12+x^5+1
            int crc;

            public CRC(){
                crc = 0x0000;
            }

            public int getCRC(){
                return crc;
            }

            public String getCRCHexString(){
                String crcHexString = Integer.toHexString(crc);
                return crcHexString;
            }

            public void resetCRC(){
                crc = 0xFFFF;
            }

            public void update(byte[] args) {
                for (byte b : args) {
                    for (int i = 0; i < 8; i++) {
                        boolean bit = ((b   >> (7-i) & 1) == 1);
                        boolean c15 = ((crc >> 15    & 1) == 1);
                        crc <<= 1;
                        // If coefficient of bit and remainder polynomial = 1 xor crc with polynomial
                        if (c15 ^ bit) crc ^= polynomial;
                    }
                }

                crc &= 0xffff;
            }
        }  // End of class CRC
    } // End of class Remote

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }

        return false;
    }

    private void updateMovementLabels() {
        TextView spdView = (TextView) findViewById(R.id.speedView);
        spdView.setText("Speed: " + Integer.toString(speed));

        TextView dirView = (TextView) findViewById(R.id.directionView);
        dirView.setText("Direction: " + Integer.toString(direction));
    }
}

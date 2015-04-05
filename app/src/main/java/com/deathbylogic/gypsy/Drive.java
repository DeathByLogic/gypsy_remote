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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class Drive extends ActionBarActivity implements View.OnTouchListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private float cur_x, start_x;
    private float cur_y, start_y;
    private int speed;
    private int direction;
    private boolean moveEvent = false;

    private String settings_ip_domain;
    private String serverIpAddress = "";

    private String line;

    private boolean connected = false;

    private int serverPort;
    private Handler handler = new Handler();

    OurView v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);

        // Setup onTouchListener for View
        v = new OurView(this);
        v.setOnTouchListener(this);
        setContentView(v);

        // Check network status
        boolean isNetworkConnected = isNetworkAvailable();

        if (!isNetworkConnected) {
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

        // TODO - !!!PUT THIS ALL IN A SEPARATE METHOD!!!
        // Populate field with default values from preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_drive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Creates a new intent to open up our settings activity (screen/page)
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//        myPass = settings.getString("settings_password", "default password");
//        settings_ip_domain = settings.getString("settings_ipdomain", "123.45.67.89"); // If no settings, default to the ip in the 2nd parameter
//        String settings_port = settings.getString("settings_port", "4999"); // If no settings, default to the port in the 2nd parameter
//        milsToPauseForCrack = settings.getString("settings_crack_ms", "default milliseconds");

//        myPass = myPass + "*"; // Add the end-of-password delimiter

//        defIPDomain = (EditText) findViewById(R.id.ip_domain);
//        defPort = (EditText) findViewById(R.id.port);
//        defIPDomain.setText(settings_ip_domain);
//        defPort.setText(settings_port);

        // *****************BELOW IS NEW 11-29-13
//        newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_unknown);
//        imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
        // *****************END NEW 11-29-13
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
                int rawDir = (int)Math.toDegrees(-Math.atan2(start_x - cur_x, start_y - cur_y));

                //SharedPreferences sharedPref = getDefaultSharedPreferences(Drive.this);

                //int max_speed = sharedPref.getInt("max_speed", 0);

                // Calculate speed based on raw speed length
                if (rawSpeed > 200) {
                    if (((rawSpeed - 200) / (v.getWidth() / 4) * 100) > 100) {
                        speed = 100;
                    } else {
                        speed = (int)((rawSpeed - 200) / (v.getWidth() / 4) * 100);
                    }
                } else {
                    speed = 0;
                }

                if (rawSpeed > 50) {
                    if (Math.abs(rawDir) > 100) {
                        direction = (rawDir > 0)?100:-100;
                    } else {
                        direction = (int)rawDir;
                    }
                } else {
                    direction = 0;
                }

                break;
        }

        // Update text on screen
        //updateMovementLabels();

        // Send command to robot

        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        connectNet.setText(R.string.connect_button_connect);
        tvGDStatusText.setText(R.string.status_text_unknown);
        toggleDoor.setEnabled(false);
        myAtomicButtonInteger.getAndSet(4);
        newStatusOfGarageDoor = "";
        currentStatusOfGarageDoor = "";
        crackDoor.setChecked(false);
        crackDoor.setEnabled(false);

        // *****************BELOW IS NEW 11-29-13
        newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_unknown);
        imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
        // *****************END NEW 11-29-13
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

    public class ClientThread implements Runnable {

        public void run() {
            try {

                Log.d("ClientActivity", "C: Connecting...");

                Socket socket = new Socket();
                SocketAddress adr = new InetSocketAddress(serverIpAddress, serverPort);
                socket.connect(adr, 5000); // 2nd parameter is timeout!!!

                connected = true;

                while (connected) {

                    // New handler to update connect button to say 'disconnect' because we are connected at this point!
                    handler.post(new Runnable() {
                        public void run() {
                            //connectNet.setText(R.string.connect_button_disconnect);
                        }
                    });

                    try {
                        //Log.d("ClientActivity", "C: Sending password.");
                        //Log.d("ClientActivity", "C: Password," + myPass + ", sent.");

                        // Not sure if this should go here, but we updated the connection status and enable the toggle door button here
                        handler.post(new Runnable() {
                            public void run() {
                                //tvConStatusText.setText(R.string.connection_status_connected);
                                //toggleDoor.setEnabled(true);
                            }
                        });

                        BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        final StringBuilder total = new StringBuilder(); // 'final' was added to allow this string to be able to be used below
                        //String line;
                        while ((line = r.readLine()) != null) {
                            total.append(line); // This probably isn't needed, right? We only want one line at a time

    					/* *** ALL Code should go in here ??? ****

    					Because it will catch everything , until the socket connection is closed???
    					See: http://stackoverflow.com/questions/2500107/how-should-i-read-from-a-buffered-reader
    					and: http://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
    					Note: We should read 'line' instead of 'total' as we are currently below
    					(out of this loop... - handler.post...)

    					*/

                            if (line.toString().contentEquals("incorrect password")) {
                                // Incorrect password stuff here
                                connected = false;
                                socket.close();
                                Log.d("ServerActivity", "Incorrect Password block ran");

                                handler.post(new Runnable() {
                                    public void run() {
                                        tvConStatusText.setText(R.string.connection_status_disconnected);
                                        tvGDStatusText.setText(R.string.status_text_unknown);
                                        connectNet.setText(R.string.connect_button_connect);
                                        toggleDoor.setEnabled(false);
                                        Toast.makeText(MainActivity.this, "Incorrect password. Please check your password and try again.", Toast.LENGTH_SHORT).show();
                                    }
                                });

                                break;
                            }

                            // Had to add this if statement because the status wasn't getting updated
                            // upon an immediate reconnection
                            if (connected) {
                                // We only want to execute the below code if we received a garage door status
                                // This allows us capture other kind of data below this if block later on
                                // as a feature update later
                                if (line.toString().contentEquals("status:open") || line.toString().contentEquals("status:closed")) {

                                    newStatusOfGarageDoor = line.toString();

                                    Log.d("ServerActivity", "newStatusOfGarageDoor: " + newStatusOfGarageDoor);
                                    Log.d("ServerActivity", "currentStatusOfGarageDoor: " + currentStatusOfGarageDoor);

                                    // If the new and old garage statuses don't match, let's do some stuff
                                    // like update the UI
                                    if (!newStatusOfGarageDoor.equals(currentStatusOfGarageDoor)) {
                                        handler.post(new Runnable() {
                                            public void run() {

                                                if (newStatusOfGarageDoor.equals("status:open")) {
                                                    tvGDStatusText.setText(R.string.status_text_open);
                                                    toggleDoor.setText(R.string.toggle_button_close);
                                                    crackDoor.setEnabled(false);

                                                    // *****************BELOW IS NEW 11-29-13
                                                    newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_open);
                                                    imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
                                                    // *****************END NEW 11-29-13

                                                    //Log.d("ServerActivity", "gdStateChanged!");
                                                } else if (newStatusOfGarageDoor.equals("status:closed")) {
                                                    tvGDStatusText.setText(R.string.status_text_closed);
                                                    toggleDoor.setText(R.string.toggle_button_open);
                                                    crackDoor.setEnabled(true);

                                                    // *****************BELOW IS NEW 11-29-13
                                                    newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_closed);
                                                    imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
                                                    // *****************END NEW 11-29-13

                                                    //Log.d("ServerActivity", "gdStateChanged!");
                                                } else {

                                                    // Temporary..just to see if something else comes in
                                                    tvGDStatusText.setText(newStatusOfGarageDoor);
                                                    // Temporary..just to see if something else comes in
                                                }

                                                currentStatusOfGarageDoor = newStatusOfGarageDoor;
                                            }
                                        });
                                    } // End if the new and current garage statuses don't match
                                } // End if a status request was received
                            } // End of new if statement
                            // Had to add this if statement because the status wasn't getting updated
                            // upon an immediate reconnection

                            //Log.d("Server response", line.toString());

                            //Log.d("ClientActivity", "(Still?) tmpAtomic=" + myAtomicButtonInteger.get());


                            if (myAtomicButtonInteger.get() == 2) {
                                out.println("cmd=gdToggle@");
                                //Log.d("ClientActivity", "Sent: cmd=gdToggle@");
                                myAtomicButtonInteger.set(0);
                            } else if (myAtomicButtonInteger.get() == 3) {
                                out.println("cmd=gdCrack:" + milsToPauseForCrack + "@");
                                //Log.d("ClientActivity", "Sent: cmd=gdCrack:" + milsToPauseForCrack + "@");
                                myAtomicButtonInteger.set(0);
                            } else if (myAtomicButtonInteger.get() == 4) {
                                out.println("disconnect@");
                                //Log.d("ClientActivity", "Sent: disconnect@");
                                connected = false;
                                socket.close();
                                myAtomicButtonInteger.set(0);
                                // Use a handler to update the connection status to show that we're disconnected now
                                handler.post(new Runnable() {
                                    public void run() {
                                        tvConStatusText.setText(R.string.connection_status_disconnected);
                                        tvGDStatusText.setText(R.string.status_text_unknown);
                                        crackDoor.setChecked(false);
                                        crackDoor.setEnabled(false);

                                        // *****************BELOW IS NEW 11-29-13
                                        newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_unknown);
                                        imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
                                        // *****************END NEW 11-29-13
                                    }
                                });

                                break; // If the disconnect button was pressed, break out of the current 'while' loop (disconnect)
                            }
                        }
                    } catch (Exception e) {
                        Log.e("ClientActivity", "S: Error", e);
                    }
                }

                socket.close();
                Log.d("ClientActivity", "C: Closed.");

                // Timeout catch
            } catch (java.net.SocketTimeoutException ste) {
                // Do something here to notify user
                connected = false;
                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "Connection timed out! Please check your internet connection, and address/port settings", Toast.LENGTH_LONG).show();
                        connectNet.setText(R.string.connect_button_connect);
                        // Change text on button to 'CONNECT' so user can try different ip/port!
                    }
                });

                //}
                // End timeout catch

            } catch (Exception e) {
                Log.e("ClientActivity", "C: Error", e);
                connected = false;
            }
        } // End of run()
    } // End of class ClientThread

    public boolean isNetworkAvailable() {
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

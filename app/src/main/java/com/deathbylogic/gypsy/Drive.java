package com.deathbylogic.gypsy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class Drive extends ActionBarActivity implements View.OnTouchListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private float cur_x, start_x;
    private float cur_y, start_y;
    private int speed;
    private int direction;
    private boolean moveEvent = false;

    OurView v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);

        v = new OurView(this);
        v.setOnTouchListener(this);
        setContentView(v);
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

    private void updateMovementLabels() {
        TextView spdView = (TextView) findViewById(R.id.speedView);
        spdView.setText("Speed: " + Integer.toString(speed));

        TextView dirView = (TextView) findViewById(R.id.directionView);
        dirView.setText("Direction: " + Integer.toString(direction));
    }
}

package com.github.skytek65.simplyandroidwear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
//import com.google.android.clockwork.watchfaces.WatchFaceStyle.Builder;

public class MainActivity extends Activity implements DisplayManager.DisplayListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // the minuteHand will rotate 6 degrees every minute
    private static final float MINUTE_ROTATION_PER_TICK = 6.0f;

    // the hourHand will rotate 0.5 degrees every minute
    private static final float HOUR_ROTATION_PER_TICK = 0.5f;

    // an IntentFilter with actions this app is going to listen for
    private final static IntentFilter intentFilter;

    static {
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
    }

    // what we're going to do every time we receive on of the above intents
    private BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            updateTime();
        }

    };

    // variable to hold on to the DISPLAY_SERVICE
    private DisplayManager displayManager;

    // thread handler
    private Handler handler = new Handler();

    // a background thread that will update the seconds, then schedule itself to re-execute every 1000 milliseconds
    private Runnable runnable = new Runnable() {
        @Override public void run() {
            Calendar calendar = Calendar.getInstance();
            updateSeconds(calendar);
            handler.postDelayed(this, 1000);
        }
    };

    // the hour hand ImageView
    private ImageView hourHand;

    // the minute hand ImageView
    private ImageView minuteHand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate our layout from main.xml
        setContentView(R.layout.main);

        // find the appropriate ImageViews in the layout
        hourHand = (ImageView) findViewById(R.id.hour);
        minuteHand = (ImageView) findViewById(R.id.minute);

        // get the DisplayManager background system service
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);

        // tell the DisplayManager it should call some callback methods provided by this class
        displayManager.registerDisplayListener(this, null);

        // tell the OS that we want to receive the Intents specified in the intentFilter
        registerReceiver(timeReceiver, intentFilter);

        // we're just about finished initializing the app, adjust the views according to the time
        updateTime();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(timeReceiver);
        displayManager.unregisterDisplayListener(this);
    }

    @Override public void onDisplayChanged(int displayId) {
        Display display = displayManager.getDisplay(displayId);
        if (display == null) {
            // No display found for this ID, treating this as an onScreenOff() but you could remove this line
            // and swallow this exception quietly. What circumstance means 'there is no display for this id'?
            onScreenOff();
            return;
        }
        switch (display.getState()) {
            case Display.STATE_DOZING:
                // the screen has dimmed, handle that accordingly
                onScreenDim();
                break;
            case Display.STATE_OFF:
                // the screen is off, handle that accordingly
                onScreenOff();
                break;
            default:
                //  Not really sure what to so about Display.STATE_UNKNOWN, so
                //  we'll treat it as if the screen is normal.
                onScreenAwake();
                break;
        }
    }

    @Override public void onDisplayAdded(int displayId) {
        // do nothing
    }

    @Override public void onDisplayRemoved(int displayId) {
        onWatchFaceRemoved();
    }

    public void onScreenDim() {

        //hourHand.setImageDrawable(R.drawable.aDimVersionOfTheHourHandImage);
        //minuteHand.setImageDrawable(R.drawable.aDimVersionOfTheMinuteHandImage);

        // stop executing the "updateSeconds" thread every second when the screen is dimmed,
        // otherwise it would consume too much battery
        handler.removeCallbacks(null);
    }

    public void onScreenAwake() {
        // restart executing our "updateSeconds" thread every second
        handler.post(runnable);
    }

    public void onScreenOff() {
        // do nothing, but should probably unregister the timeReceiver to save battery
    }

    public void onWatchFaceRemoved() {
        // do nothing
    }

    private void updateTime() {
        // for testing only, since the emulator is in the GMT timezone
         TimeZone tz = TimeZone.getTimeZone("America/Chicago");
         Calendar calendar = Calendar.getInstance(tz);

        // java's object that knows all about times and dates
        // Calendar calendar = Calendar.getInstance();

        // the number of hours
        int hour = calendar.get(Calendar.HOUR);

        // the number of minutes past the hour
        int minute = calendar.get(Calendar.MINUTE);

        // the hour hand moves 30 degrees every hour, plus 0.5 degrees every minute past the hour
        float hourRotation = (hour * 30) + (HOUR_ROTATION_PER_TICK * minute);

        // the minute hand moves 6 degrees every minute
        float minuteRotation = minute * MINUTE_ROTATION_PER_TICK;

        // rotate the ImageViews accordingly
        minuteHand.setRotation(minuteRotation);
        hourHand.setRotation(hourRotation);

        // for debugging purposes
        Log.d(TAG, "Hour: " + hour + " | Rotation: " + hourRotation);
        Log.d(TAG, "Minute: " + minute + " | Rotation: " + minuteRotation);

        // update the seconds separately
        updateSeconds(calendar);


    }

    private void updateSeconds(Calendar calendar) {
        int second = calendar.get(Calendar.SECOND);

        float secondRotation = second * 6.0f;

        Log.d(TAG, "Second: " + second + " | Rotation: " + secondRotation);

    }

}

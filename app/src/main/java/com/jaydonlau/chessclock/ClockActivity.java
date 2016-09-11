package com.jaydonlau.chessclock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

/**
 * ClockActivity for running the two chess clocks (settings button switches to SettingsActivity)
 * Created by jaydonlau on 16-08-31.
 */
public class ClockActivity extends AppCompatActivity {
    private static final String TAG = ClockActivity.class.getSimpleName();
    long initClock1;
    long initClock2;
    // 5 minutes in milliseconds
    long clock1Time;
    long clock2Time;
    long prevClock1;
    long prevClock2;
    long delayClock1;
    long delayClock2;
    long startMoveTimeClock1;
    long startMoveTimeClock2;
    boolean pastDelayClock1;
    boolean pastDelayClock2;
    Button clock1;
    Button clock2;

    ImageButton pauseBtn;
    ImageButton resetBtn;

    boolean clockStart = false;
    boolean clock1Running = false;
    boolean clock2Running = false;

    Handler clockHandler;
    SharedPreferences prefs;
    MediaPlayer mp;

    // 0 -> Fischer, 1 -> Bronstein delay, 2 -> Simple delay, 3 -> Overtime Penalty, 4 -> Hour Glass
    int timeControlType;
    final int FISCHER = 0;
    final int BRONSTEIN_DELAY = 1;
    final int SIMPLE_DELAY = 2;
    final int OVERTIME_PENALTY = 3;

    private void setClockTime(long clockTime, Button clock) {
        int millis = (int) clockTime % 1000;
        int seconds = (int) clockTime / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        minutes %= 60;
        seconds %= 60;
        String time;
        if (hours > 0) {
            time = String.format("%d:%d:%02d%03d", hours, minutes, seconds, millis);
        }
        else if (minutes > 0) {
            time = String.format("%d:%02d%03d", minutes, seconds, millis);
        }
        else {
            time = String.format("%02d%03d", seconds, millis);
        }
        int textSize = time.length();
        SpannableString ssTime = new SpannableString(time);
        // make milliseconds 1/4 of the font size
        ssTime.setSpan(new RelativeSizeSpan(0.25f), textSize - 3, textSize, 0);
        clock.setText(ssTime);
    }

    private void setClockInactive(Button clock) {
        clock.setTextColor(getResources().getColor(R.color.clock_inactive_text));
        clock.setBackgroundDrawable(getResources().getDrawable(R.drawable.clock_button_inactive));
    }

    private void setClockActive(Button clock) {
        clock.setTextColor(getResources().getColor(R.color.clock_active_text));
        clock.setBackgroundDrawable(getResources().getDrawable(R.drawable.clock_button_active));
    }

    private void setClockFinish(Button clock) {
        clock.setTextColor(getResources().getColor(R.color.clock_active_text));
        clock.setBackgroundDrawable(getResources().getDrawable(R.drawable.clock_button_finish));
    }

    private void createButtons() {
        pauseBtn = (ImageButton) findViewById(R.id.clock_pause_button);
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clockStart) {
                    clockHandler.removeCallbacks(clock1Runnable);
                    clockHandler.removeCallbacks(clock2Runnable);
                    clock1Running = false;
                    clock2Running = false;
                    clockStart = false;
                    setClockInactive(clock1);
                    setClockInactive(clock2);
                }
            }
        });

        resetBtn = (ImageButton) findViewById(R.id.clock_reset_button);
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ClockActivity.this);
                builder.setTitle(R.string.app_name);
                builder.setMessage("Are you sure you want to reset?");
                builder.setIconAttribute(android.R.attr.alertDialogIcon);
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        clockStart = false;
                        clock1Running = false;
                        clock2Running = false;
                        pauseBtn.setVisibility(View.GONE);
                        clockHandler.removeCallbacks(clock1Runnable);
                        clockHandler.removeCallbacks(clock2Runnable);
                        clock1Time = initClock1;
                        clock2Time = initClock2;
                        setClockTime(clock1Time, clock1);
                        setClockTime(clock2Time, clock2);
                        setClockInactive(clock1);
                        setClockInactive(clock2);
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    private void initializeClockTime() {
        String clock1hours = prefs.getString("white_hours", "0");
        String clock1mins = prefs.getString("white_minutes", "5");
        String clock1seconds = prefs.getString("white_seconds", "0");
        initClock1 = (Integer.parseInt(clock1hours) * 60 * 60 * 1000);
        initClock1 += (Integer.parseInt(clock1mins) * 60 * 1000);
        initClock1 += (Integer.parseInt(clock1seconds) * 1000);

        String clock2hours = prefs.getString("black_hours", "0");
        String clock2mins = prefs.getString("black_minutes", "5");
        String clock2seconds = prefs.getString("black_seconds", "0");
        initClock2 = (Integer.parseInt(clock2hours) * 60 * 60 * 1000);
        initClock2 += (Integer.parseInt(clock2mins) * 60 * 1000);
        initClock2 += (Integer.parseInt(clock2seconds) * 1000);

        clock1Time = initClock1;
        clock2Time = initClock2;

        timeControlType = prefs.getInt("time_control_dropdown", 0);
        delayClock1 = Integer.parseInt(prefs.getString("white_dropdown_delay", "0"));
        delayClock2 = Integer.parseInt(prefs.getString("black_dropdown_delay", "0"));
    }

    Runnable clock1Runnable = new Runnable() {
        @Override
        public void run() {
            if (clock1Time <= 0) {
                // set red animation
                setClockFinish(clock1);
                // OVERTIME penalty sets red animation and counts up
                if (timeControlType == OVERTIME_PENALTY) {
                    clock1Time -= SystemClock.elapsedRealtime() - prevClock1;
                    prevClock1 = SystemClock.elapsedRealtime();
                    setClockTime(Math.abs(clock1Time), clock1);
                }
                else {
                    clock1Running = false;
                    clock1Time = 0;
                    setClockTime(clock1Time, clock1);
                    clockHandler.removeCallbacks(clock1Runnable);
                }
            }
            // SIMPLE_DELAY stops counting down for the delayed time
            else if (timeControlType == SIMPLE_DELAY) {
                // decrease time without updating view
                clock1Time -= SystemClock.elapsedRealtime() - prevClock1;
                prevClock1 = SystemClock.elapsedRealtime();
                long difference = startMoveTimeClock1 - clock1Time;
                if (!pastDelayClock1 && difference >= (delayClock1 * 1000)) {
                    // reset to original time for updating view
                    pastDelayClock1 = true;
                    clock1Time = startMoveTimeClock1;
                }
                // only increment if past delay
                if (pastDelayClock1) {
                    setClockTime(clock1Time, clock1);
                }
            }
            else {
                clock1Time -= SystemClock.elapsedRealtime() - prevClock1;
                prevClock1 = SystemClock.elapsedRealtime();
                setClockTime(clock1Time, clock1);
            }

            if (clock1Running) {
                clockHandler.postDelayed(this, 30);
            }
        }
    };

    Runnable clock2Runnable = new Runnable() {
        @Override
        public void run() {
            if (clock2Time <= 0) {
                // set red animation
                setClockFinish(clock2);

                if (timeControlType == OVERTIME_PENALTY) {
                    clock2Time -= SystemClock.elapsedRealtime() - prevClock2;
                    prevClock2 = SystemClock.elapsedRealtime();
                    setClockTime(Math.abs(clock2Time), clock2);
                }
                else {
                    clock2Running = false;
                    clock2Time = 0;
                    setClockTime(clock2Time, clock2);
                    clockHandler.removeCallbacks(clock2Runnable);
                }
            }
            // SIMPLE_DELAY stops counting down for the delayed time
            else if (timeControlType == SIMPLE_DELAY) {
                // decrease time without updating view
                clock2Time -= SystemClock.elapsedRealtime() - prevClock2;
                prevClock2 = SystemClock.elapsedRealtime();
                long difference = startMoveTimeClock2 - clock2Time;
                if (!pastDelayClock2 && difference >= (delayClock2 * 1000)) {
                    // reset to original time for updating view
                    pastDelayClock2 = true;
                    clock2Time = startMoveTimeClock2;
                }
                if (pastDelayClock2) {
                    setClockTime(clock2Time, clock2);
                }
            }
            else {
                clock2Time -= SystemClock.elapsedRealtime() - prevClock2;
                prevClock2 = SystemClock.elapsedRealtime();
                setClockTime(clock2Time, clock2);
            }

            if (clock2Running) {
                clockHandler.postDelayed(this, 30);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mp = MediaPlayer.create(this, R.raw.click_sound);

        initializeClockTime();

        createButtons();
        clockHandler = new Handler();
        clock1 = (Button) findViewById(R.id.clock1);
        setClockTime(clock1Time, clock1);
        clock1.setRotation(180);
        clock1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mp.isPlaying()) {
                    mp.seekTo(0);
                    mp.start();
                } else {
                    mp.start();
                }
                // start clock2 if clock has not started yet
                if (!clockStart) {
                    clockHandler.postDelayed(clock2Runnable, 0);
                    clockStart = true;
                    clock2Running = true;
                    setClockActive(clock2);
                    pauseBtn.setVisibility(View.VISIBLE);

                    // keep track of starting time for BRONSTEIN_DELAY
                    startMoveTimeClock2 = clock2Time;
                    prevClock2 = SystemClock.elapsedRealtime();
                } else if (clock1Running && !clock2Running) {
                    clockHandler.removeCallbacks(clock1Runnable);
                    clock1Running = false;
                    clock2Running = true;

                    // FISCHER adds increment after move
                    if (timeControlType == FISCHER) {
                        clock1Time += (delayClock1 * 1000);
                        setClockTime(clock1Time, clock1);
                    }
                    // BRONSTEIN adds the MIN(delay, difference) after move
                    else if (timeControlType == BRONSTEIN_DELAY) {
                        // add back difference between delay and time used for clock 2
                        long difference = startMoveTimeClock1 - clock1Time;
                        if (difference < (delayClock1 * 1000)) {
                            clock1Time += difference;
                        } else {
                            clock1Time += (delayClock1 * 1000);
                        }
                        setClockTime(clock1Time, clock1);
                    } else if (timeControlType == SIMPLE_DELAY) {
                        pastDelayClock2 = false;
                    }
                    setClockInactive(clock1);
                    setClockActive(clock2);

                    startMoveTimeClock2 = clock2Time;
                    prevClock2 = SystemClock.elapsedRealtime();

                    clockHandler.postDelayed(clock2Runnable, 0);
                }
            }
        });

        clock2 = (Button) findViewById(R.id.clock2);
        setClockTime(clock2Time, clock2);
        clock2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mp.isPlaying()) {
                    mp.seekTo(0);
                    mp.start();
                } else {
                    mp.start();
                }
                // start clock 1 if clock has not started
                if (!clockStart) {
                    clockHandler.postDelayed(clock1Runnable, 0);
                    clockStart = true;
                    clock1Running = true;
                    setClockActive(clock1);
                    pauseBtn.setVisibility(View.VISIBLE);
                    startMoveTimeClock1 = clock1Time;
                    prevClock1 = SystemClock.elapsedRealtime();
                    // change clock1 color to active
                }
                else if (clock2Running && !clock1Running) {
                    clockHandler.removeCallbacks(clock2Runnable);
                    clock2Running = false;
                    clock1Running = true;

                    // FISCHER adds increment after move
                    if (timeControlType == FISCHER) {
                        clock2Time += (delayClock2 * 1000);
                        setClockTime(clock2Time, clock2);
                    }
                    // BRONSTEIN adds the MIN(delay, difference) after move
                    else if (timeControlType == BRONSTEIN_DELAY) {
                        // add back difference between delay and time used for clock 1
                        long difference = startMoveTimeClock2 - clock2Time;
                        if (difference < (delayClock2 * 1000)) {
                            clock2Time += difference;
                        }
                        else {
                            clock2Time += (delayClock2 * 1000);
                        }
                        setClockTime(clock2Time, clock2);
                    }
                    else if (timeControlType == SIMPLE_DELAY) {
                        pastDelayClock1 = false;
                    }
                    setClockActive(clock1);
                    setClockInactive(clock2);

                    startMoveTimeClock1 = clock1Time;
                    prevClock1 = SystemClock.elapsedRealtime();

                    clockHandler.postDelayed(clock1Runnable, 0);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clock1Runnable);
        clockHandler.removeCallbacks(clock2Runnable);
        // change clock1 and clock2 colors to inactive
        setClockInactive(clock1);
        setClockInactive(clock2);
    }
}

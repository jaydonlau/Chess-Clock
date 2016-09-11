package com.jaydonlau.chessclock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Settings Activity to set time control types and length for each player (saved in SharedPreferences)
 * Created by jaydonlau on 16-08-30.
 */
public class SettingsActivity extends AppCompatActivity {
    EditText white_hours;
    EditText white_minutes;
    EditText white_seconds;
    EditText black_hours;
    EditText black_minutes;
    EditText black_seconds;
    Button save_button;
    Button start_button;
    Spinner dropdown;
    TextView dropdown_desc;
    LinearLayout white_dropdown_delay_container;
    LinearLayout black_dropdown_delay_container;
    EditText white_dropdown_delay;
    EditText black_dropdown_delay;
    TextView white_dropdown_delay_label;
    TextView black_dropdown_delay_label;
    SharedPreferences prefs;
    AlertDialog.Builder builder;

    final int FISCHER = 0;
    final int BRONSTEIN_DELAY = 1;
    final int SIMPLE_DELAY = 2;
    final int OVERTIME_PENALTY = 3;
    final String FISCHER_LABEL = "Increment";

    final String FISCHER_DESC = "Increment is added after each move.";
    final String BRONSTEIN_DESC = "Used portion of the increment is added after each move.";
    final String SIMPLE_DESC = "Clock does not start until after the delay time period.";
    final String OVERTIME_DESC = "When time reaches zero, the clock begins counting up.";

    private void showButtonOnTextChange(EditText editText) {
        editText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    save_button.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setZeroIfEmpty(EditText time) {
        if (time.getText().toString().equals("")) {
            time.setText("0");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        white_hours = (EditText) findViewById(R.id.settings_white_hours_input);
        white_minutes = (EditText) findViewById(R.id.settings_white_minutes_input);
        white_seconds = (EditText) findViewById(R.id.settings_white_seconds_input);
        black_hours = (EditText) findViewById(R.id.settings_black_hours_input);
        black_minutes = (EditText) findViewById(R.id.settings_black_minutes_input);
        black_seconds = (EditText) findViewById(R.id.settings_black_seconds_input);
        save_button = (Button) findViewById(R.id.settings_save_button);
        start_button = (Button) findViewById(R.id.settings_start_button);
        white_dropdown_delay_container = (LinearLayout) findViewById(R.id.white_delay_container);
        white_dropdown_delay  = (EditText) findViewById(R.id.settings_white_delay_input);
        black_dropdown_delay_container = (LinearLayout) findViewById(R.id.black_delay_container);
        black_dropdown_delay = (EditText) findViewById(R.id.settings_black_delay_input);
        white_dropdown_delay_label = (TextView) findViewById(R.id.settings_white_delay_label);
        black_dropdown_delay_label = (TextView) findViewById(R.id.settings_black_delay_label);
        dropdown_desc = (TextView) findViewById(R.id.time_control_description);

        // set time control type dropdown
        dropdown = (Spinner)findViewById(R.id.time_control_dropdown);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(SettingsActivity.this,
                R.array.settings_time_control_dropdown, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdown.setAdapter(adapter);
        dropdown.setSelection(prefs.getInt("time_control_dropdown", 0));
        dropdown.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                switch(position) {
                    case FISCHER:
                        white_dropdown_delay_container.setVisibility(View.VISIBLE);
                        black_dropdown_delay_container.setVisibility(View.VISIBLE);
                        white_dropdown_delay_label.setText(FISCHER_LABEL);
                        black_dropdown_delay_label.setText(FISCHER_LABEL);
                        dropdown_desc.setText(FISCHER_DESC);
                        break;
                    case BRONSTEIN_DELAY:
                        white_dropdown_delay_container.setVisibility(View.VISIBLE);
                        black_dropdown_delay_container.setVisibility(View.VISIBLE);
                        dropdown_desc.setText(BRONSTEIN_DESC);
                        break;
                    case SIMPLE_DELAY:
                        white_dropdown_delay_container.setVisibility(View.VISIBLE);
                        black_dropdown_delay_container.setVisibility(View.VISIBLE);
                        dropdown_desc.setText(SIMPLE_DESC);
                        break;
                    case OVERTIME_PENALTY:
                        dropdown_desc.setText(OVERTIME_DESC);
                        white_dropdown_delay_container.setVisibility(View.GONE);
                        black_dropdown_delay_container.setVisibility(View.GONE);
                        break;
                }
                save_button.setVisibility(View.VISIBLE);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        white_dropdown_delay.setText(prefs.getString("white_dropdown_delay", "0"));
        black_dropdown_delay.setText(prefs.getString("black_dropdown_delay", "0"));

        showButtonOnTextChange(white_hours);
        showButtonOnTextChange(white_minutes);
        showButtonOnTextChange(white_seconds);
        showButtonOnTextChange(black_hours);
        showButtonOnTextChange(black_minutes);
        showButtonOnTextChange(black_seconds);

        // set textfields from shared preferences
        white_hours.setText(prefs.getString("white_hours", "0"));
        white_minutes.setText(prefs.getString("white_minutes", "5"));
        white_seconds.setText(prefs.getString("white_seconds", "0"));
        black_hours.setText(prefs.getString("black_hours", "0"));
        black_minutes.setText(prefs.getString("black_minutes", "5"));
        black_seconds.setText(prefs.getString("black_seconds", "0"));

        builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle(R.string.app_name);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setZeroIfEmpty(white_hours);
                setZeroIfEmpty(white_minutes);
                setZeroIfEmpty(white_seconds);
                setZeroIfEmpty(black_hours);
                setZeroIfEmpty(black_minutes);
                setZeroIfEmpty(black_seconds);
                setZeroIfEmpty(white_dropdown_delay);
                setZeroIfEmpty(black_dropdown_delay);

                Editor editor = prefs.edit();
                editor.putInt("time_control_dropdown", dropdown.getSelectedItemPosition());
                editor.putString("white_hours", white_hours.getText().toString());
                editor.putString("white_minutes", white_minutes.getText().toString());
                editor.putString("white_seconds", white_seconds.getText().toString());
                editor.putString("black_hours", black_hours.getText().toString());
                editor.putString("black_minutes", black_minutes.getText().toString());
                editor.putString("black_seconds", black_seconds.getText().toString());
                editor.putString("white_dropdown_delay", white_dropdown_delay.getText().toString());
                editor.putString("black_dropdown_delay", black_dropdown_delay.getText().toString());

                editor.apply();

                builder.setMessage("Settings saved");
                builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                save_button.setVisibility(View.GONE);
            }
        });

        start_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (save_button.getVisibility() == View.VISIBLE) {
                    builder.setMessage("Save your changes before starting.");
                    builder.setPositiveButton("Okay", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else {
                    Intent i = new Intent(SettingsActivity.this, ClockActivity.class);
                    startActivity(i);
                }
            }
        });
    }
}

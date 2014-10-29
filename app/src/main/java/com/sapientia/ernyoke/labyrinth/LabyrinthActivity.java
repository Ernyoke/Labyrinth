package com.sapientia.ernyoke.labyrinth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class LabyrinthActivity extends Activity {

    private LabyrinthView labView;
    private LabyrinthModel labModel;
    SharedPreferences sharedPreferences;

    private static final String TAG = "com.sapientia.ernyoke.labyrinth";
    private static final String CONTROL = "CONTROL";

    private PowerManager pm;
    private PowerManager.WakeLock wl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        MainMenu.DIFFICULTY difficulty = (MainMenu.DIFFICULTY)bundle.get(MainMenu.DIFF_ID);
        switch (difficulty) {
            case EASY: {
                readLabyrinth(R.array.labyrinthEasy);
                break;
            }
            case MEDIUM: {
                readLabyrinth(R.array.labyrinthMedium);
                break;
            }
            case HARD: {
                readLabyrinth(R.array.labyrinthDifficult);
                break;
            }
        }
        labView = new LabyrinthView(this, labModel, difficulty);
        setContentView(labView);
        sharedPreferences = getSharedPreferences(TAG, MODE_PRIVATE);

        pm = (PowerManager) getSystemService(this.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        String inputControl = sharedPreferences.getString(CONTROL, "TOUCH");
        if(inputControl.equals("TOUCH")) {
            MenuItem item =  menu.findItem(R.id.action_touch);
            item.setChecked(true);
        }
        else {
            if(inputControl.equals("ACCELEROMETER")) {
                MenuItem item =  menu.findItem(R.id.action_accelerometer);
                item.setChecked(true);
            }
            else {
                if(inputControl.equals("GRAVITY")) {
                    MenuItem item =  menu.findItem(R.id.action_gravity);
                    item.setChecked(true);
                }
                else {
                    if(inputControl.equals("SPEECH")) {
                        MenuItem item =  menu.findItem(R.id.action_speech);
                        item.setChecked(true);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        switch (id) {
            case R.id.action_accelerometer: {
                if(!labView.setInputControl(LabyrinthView.CONTROL.ACCELEROMETER)) {
                    Toast.makeText(this, "Accelerometer is not available!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Accelerometer input set!", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                }
                break;
            }
            case R.id.action_touch: {
                if(!labView.setInputControl(LabyrinthView.CONTROL.TOUCH)) {
                    Toast.makeText(this, "Dafuq???", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Touch input set!", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                }
                break;
            }
            case R.id.action_gravity: {
                if(!labView.setInputControl(LabyrinthView.CONTROL.GRAVITY)) {
                    Toast.makeText(this, "Gravity sensor is not available!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Gravity sensor set!", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                }
                break;
            }
            case R.id.action_speech: {
                if(!labView.setInputControl(LabyrinthView.CONTROL.SPEECH)) {
                    Toast.makeText(this, "Speech recognition is not available!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Speech recognition input set!", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                }
                break;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void readLabyrinth(int resId) {
        Resources res = this.getResources();
        String[] labyrinthRows = res.getStringArray(resId);
        labModel = new LabyrinthModel();
        labModel.initLabyrinth(labyrinthRows);
    }

    @Override
    protected void onPause() {
        super.onPause();
        wl.release();
        labView.unregisterSensors();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CONTROL, labView.getInputControl().toString());
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wl.acquire();
        String inputControl = sharedPreferences.getString(CONTROL, "TOUCH");
        if(inputControl.equals("TOUCH")) {
            labView.setInputControl(LabyrinthView.CONTROL.TOUCH);
        }
        else {
            if(inputControl.equals("ACCELEROMETER")) {
                labView.setInputControl(LabyrinthView.CONTROL.ACCELEROMETER);

            }
            else {
                if(inputControl.equals("GRAVITY")) {
                    labView.setInputControl(LabyrinthView.CONTROL.GRAVITY);

                }
                else {
                    if(inputControl.equals("SPEECH")) {
                        labView.setInputControl(LabyrinthView.CONTROL.SPEECH);

                    }
                }
            }
        }
    }
}

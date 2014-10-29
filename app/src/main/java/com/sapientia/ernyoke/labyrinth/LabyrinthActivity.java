package com.sapientia.ernyoke.labyrinth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
                }
                break;
            }
            case R.id.action_touch: {
                if(!labView.setInputControl(LabyrinthView.CONTROL.TOUCH)) {
                    Toast.makeText(this, "Dafuq???", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Touch input set!", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.action_gravity: {
                if(!labView.setInputControl(LabyrinthView.CONTROL.GRAVITY)) {
                    Toast.makeText(this, "Gravity sensor is not available!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Gravity sensor set!", Toast.LENGTH_SHORT).show();
                }
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
        labView.unregisterSensors();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CONTROL, labView.getInputControl().toString());
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            }
        }
    }
}

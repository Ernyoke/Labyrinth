package com.sapientia.ernyoke.labyrinth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


public class LabyrinthActivity extends Activity implements SensorEventListener, RecognitionListener {

    private LabyrinthView labView;
    private LabyrinthModel labModel;
    SharedPreferences sharedPreferences;

    private static final String TAG = "com.sapientia.ernyoke.labyrinth";
    private static final String CTRL = "CONTROL";

    private PowerManager pm;
    private PowerManager.WakeLock wl;

    private float motionStart_x;
    private float motionStart_y;

    private static final int TIME = 300;
    private static final double G = 4;

    private long lastDetection;
    private long currentDetection;

    public enum CONTROL {ACCELEROMETER, TOUCH, GRAVITY, SPEECH, MI};

    private CONTROL inputControl;
    private SensorManager sensorManager;
    private Sensor activeSensor;
    private List<Sensor> sensorList;

    private boolean isEnd = false;

    private MainMenu.DIFFICULTY currentDiff;

    private SpeechRecognizer sr;

    private Intent listenIntent;

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
        this.getAvailableSensors();
        labView = new LabyrinthView(this, labModel);
        setContentView(labView);
        sharedPreferences = getSharedPreferences(TAG, MODE_PRIVATE);
        pm = (PowerManager) getSystemService(this.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        String input = sharedPreferences.getString(CTRL, "TOUCH");
        if(input.equals("TOUCH")) {
            MenuItem item =  menu.findItem(R.id.action_touch);
            item.setChecked(true);
            inputControl = CONTROL.TOUCH;
        }
        else {
            if(input.equals("ACCELEROMETER")) {
                MenuItem item =  menu.findItem(R.id.action_accelerometer);
                item.setChecked(true);
                inputControl = CONTROL.ACCELEROMETER;
            }
            else {
                if(input.equals("GRAVITY")) {
                    MenuItem item =  menu.findItem(R.id.action_gravity);
                    item.setChecked(true);
                    inputControl = CONTROL.GRAVITY;
                }
                else {
                    if(input.equals("SPEECH")) {
                        MenuItem item =  menu.findItem(R.id.action_speech);
                        item.setChecked(true);
                        inputControl = CONTROL.SPEECH;
                    }
                    else {
                        if(input.equals("MI")) {
                            MenuItem item =  menu.findItem(R.id.action_gravity);
                            item.setChecked(true);
                            inputControl = CONTROL.MI;
                        }
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
                if(!setInputControl(CONTROL.ACCELEROMETER)) {
                    Toast.makeText(this, "Accelerometer is not available!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Accelerometer input set!", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                }
                break;
            }
            case R.id.action_touch: {
                if(!setInputControl(CONTROL.TOUCH)) {
                    Toast.makeText(this, "Dafuq???", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Touch input set!", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                }
                break;
            }
            case R.id.action_gravity: {
                if(!setInputControl(CONTROL.GRAVITY)) {
                    Toast.makeText(this, "Gravity sensor is not available!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Gravity sensor set!", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                }
                break;
            }
            case R.id.action_speech: {
                if(!setInputControl(CONTROL.SPEECH)) {
                    Toast.makeText(this, "Speech recognition is not available!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Speech recognition input set!", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                }
                break;
            }

            case R.id.action_mi: {
                if(!setInputControl(CONTROL.MI)) {
                    Toast.makeText(this, "Not today!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Self solve set!", Toast.LENGTH_SHORT).show();
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
        unregisterSensors();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CTRL, inputControl.toString());
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wl.acquire();
        String input = sharedPreferences.getString(CTRL, "TOUCH");
        CONTROL control = this.stringToControl(input);
        if(checkIfHasSensor(control)) {
            inputControl = control;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(inputControl == CONTROL.TOUCH && !isEnd) {
            float eventX = event.getX();
            float eventY = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    motionStart_x = eventX;
                    motionStart_y = eventY;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int edge = labView.getBlockSize();
                    if (Math.abs(eventX - motionStart_x) > edge) {
                        if (motionStart_x < eventX) {
                            labModel.right();
                        } else {
                            labModel.left();
                        }
                        motionStart_x = eventX;
                    }
                    if (Math.abs(eventY - motionStart_y) > edge) {
                        if (motionStart_y < eventY) {
                            labModel.down();
                        } else {
                            labModel.up();
                        }
                        motionStart_y = eventY;
                    }
                    break;
                default:
                    return false;
            }
            labView.invalidate();
            if(labModel.isWinner()) {
                winner();
            }
        }
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch(inputControl) {
            case ACCELEROMETER: {
                currentDetection = System.currentTimeMillis();
                if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && currentDetection - lastDetection > TIME) {
                    float[] acceleration;
                    acceleration = sensorEvent.values;

                    if(Math.abs(acceleration[0]) > G) {
                        if(acceleration[0] > 0) {
                            labModel.left();
                        }
                        else {
                            labModel.right();
                        }
                    }

                    if(Math.abs(acceleration[1]) > G) {
                        if(acceleration[1] > 0) {
                            labModel.down();
                        }
                        else {
                            labModel.up();
                        }
                    }

                    lastDetection = System.currentTimeMillis();
                    labView.invalidate();

                    if(labModel.isWinner()) {
                        winner();
                    }
                }
                break;
            }
            case GRAVITY: {
                currentDetection = System.currentTimeMillis();
                if(sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY && currentDetection - lastDetection > TIME) {
                    float[] acceleration;
                    acceleration = sensorEvent.values;

                    if(Math.abs(acceleration[0]) > G) {
                        if(acceleration[0] > 0) {
                            labModel.left();
                        }
                        else {
                            labModel.right();
                        }
                    }

                    if(Math.abs(acceleration[1]) > G) {
                        if(acceleration[1] > 0) {
                            labModel.down();
                        }
                        else {
                            labModel.up();
                        }
                    }

                    lastDetection = System.currentTimeMillis();
                    labView.invalidate();

                    if(labModel.isWinner()) {
                        winner();
                    }

                }

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private boolean setInputControl(CONTROL control) {
        if(checkIfHasSensor(control)) {
            this.inputControl = control;
            return true;
        }
        return false;
    }

    private void getAvailableSensors() {
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
    }

    private boolean checkIfHasSensor(CONTROL control) {
        switch (control) {
            case ACCELEROMETER: {
                this.unregisterSensors();
                for (int i = 0; i< sensorList.size(); i++) {
                    if (sensorList.get(i).getType() == Sensor.TYPE_ACCELEROMETER) {
                        sensorManager.unregisterListener(this);
                        activeSensor = sensorList.get(i);
                        sensorManager.registerListener(this,activeSensor, SensorManager.SENSOR_DELAY_NORMAL);
                        return true;
                    }
                }
                break;
            }
            case GRAVITY: {
                this.unregisterSensors();
                for (int i = 0; i< sensorList.size(); i++) {
                    if (sensorList.get(i).getType() == Sensor.TYPE_GRAVITY) {
                        sensorManager.unregisterListener(this);
                        activeSensor = sensorList.get(i);
                        sensorManager.registerListener(this,activeSensor, SensorManager.SENSOR_DELAY_NORMAL);
                        return true;
                    }
                }
                break;
            }
            case TOUCH: {
                this.unregisterSensors();
                return  true;
            }

            case SPEECH: {
                Log.d("TAG", "speech");
                this.unregisterSensors();
                sr = SpeechRecognizer.createSpeechRecognizer(this);
                sr.setRecognitionListener(this);
                listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                listenIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");

                listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,15);
                sr.startListening(listenIntent);
                return true;
            }

            case MI: {
                Log.d("TAG", "MI");
                    this.unregisterSensors();
                    solveThis();
                break;
            }

        }
        return false;
    }


    public void unregisterSensors() {
        if(sr != null) {
            sr.stopListening();
            sr.cancel();
            sr.destroy();
        }
        sensorManager.unregisterListener(this);
    }

    private void winner() {
        unregisterSensors();
        isEnd = true;
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setTitle(getString(R.string.dialog_title));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.go_to_mainmenu), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();

            }
        });
        if(currentDiff != MainMenu.DIFFICULTY.HARD) {
            alertDialog.setMessage(getString(R.string.dialog_text_next));
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.go_to_next), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("requestCode", MainMenu.REQ_CODE);
                    setResult(MainMenu.NEXT_DIFF);
                    finish();
                    isEnd = false;
                    return;
                }
            });
        }
        else {
            alertDialog.setMessage(getString(R.string.dialog_text_last));
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.retry), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("requestCode", MainMenu.REQ_CODE);
                    setResult(MainMenu.NEXT_DIFF);
                    finish();
                    isEnd = false;
                    return;
                }
            });
        }

        alertDialog.show();
    }

    //speech recognizer
    //--------------------------------------------------
    @Override
    public void onReadyForSpeech(Bundle bundle) {
        //Log.d("TAG", "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        //Log.d("TAG", "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float v) {
        //Log.d("TAG", "onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        //Log.d("TAG", "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        //Log.d("TAG", "onEndofSpeech");
    }

    @Override
    public void onError(int i) {
        //Log.d("TAG",  "error " + i);
        sr.startListening(listenIntent);
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < data.size(); i++)
        {
            Log.d("TAG", data.get(i));
            if(data.get(i).equals("left")) {
                labModel.left();
                break;
            }
            if(data.get(i).equals("right")) {
                labModel.right();
                break;
            }
            if(data.get(i).equals("up")) {
                labModel.up();
                break;
            }
            if(data.get(i).equals("down")) {
                labModel.down();
                break;
            }
        }
        if(labModel.isWinner()) {
            winner();
            labView.invalidate();
        }
        else {
            labView.invalidate();
            sr.startListening(listenIntent);
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }

    //---------------------------------------------

    private CONTROL stringToControl(String input) {
        if(input.equals("TOUCH")) {
            return CONTROL.TOUCH;
        }
        else {
            if(input.equals("ACCELEROMETER")) {
                return CONTROL.ACCELEROMETER;
            }
            else {
                if(input.equals("GRAVITY")) {
                    return CONTROL.GRAVITY;
                }
                else {
                    if(input.equals("SPEECH")) {
                        return CONTROL.SPEECH;
                    }
                    else {
                        if(input.equals("MI")) {
                            return CONTROL.MI;
                        }
                    }
                }
            }
        }
        return CONTROL.TOUCH;
    }

    private void solveThis() {
        final ArrayList<LabyrinthModel.Position> path = labModel.solveSelf();
        for(LabyrinthModel.Position actual : path) {
            Log.d("TAG", actual.getX() + " " + actual.getY());
        }

        new AsyncTask<Integer, Integer, Void>(){
            protected Void doInBackground(Integer... val) {
                int count = val.length;
                for (int i = 0; i < val[0]; i++) {
                    publishProgress(i);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (isCancelled()) break;
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                labModel.setBallrow(path.get(values[0]).getX());
                labModel.setBallcol(path.get(values[0]).getY());
                labView.invalidate();
            }

        }.execute(path.size());


    }

}

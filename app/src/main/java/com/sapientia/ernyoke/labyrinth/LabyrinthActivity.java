package com.sapientia.ernyoke.labyrinth;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class LabyrinthActivity extends Activity implements SensorEventListener, RecognitionListener {

    private LabyrinthView labView;
    private LabyrinthModel labModel;
    SharedPreferences sharedPreferences;

    private PowerManager pm;
    private PowerManager.WakeLock wl;

    private float motionStart_x;
    private float motionStart_y;

    private static final int TIME = 300;
    private static final double G = 9.81;
    private double sensitivity = 0;

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

    private int netLevel;
    private int finalNetLevel;

    private AsyncTask solver;

    private Menu optionsMenu;

    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        currentDiff = (MainMenu.DIFFICULTY)bundle.get(Constants.DIFF_ID);
        switch (currentDiff) {
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
            case NET: {
                String labyrinth = bundle.getString(Constants.LAB);
                netLevel = bundle.getInt(Constants.NET_LEVEL);
                finalNetLevel = bundle.getInt(Constants.FINAL_NET_LEVEL);
                readLabyrinth(labyrinth);
                break;
            }
        }
        this.getAvailableSensors();
        labView = new LabyrinthView(this, labModel);
        setContentView(labView);
        //sharedPreferences = getSharedPreferences(Constants.TAG, MODE_PRIVATE);
        pm = (PowerManager) getSystemService(this.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, Constants.TAG);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        startTime = System.currentTimeMillis();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        optionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        SharedPreferences.Editor editor = sharedPreferences.edit();

        int id = item.getItemId();
        switch (id) {
            case R.id.action_accelerometer: {
                if(!setInputControl(CONTROL.ACCELEROMETER)) {
                    Toast.makeText(this, "Accelerometer is not available!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Accelerometer input set!", Toast.LENGTH_SHORT).show();
//                    editor.putString(Settings.INPUT_TYPE, Constants.ACCELEROMETER);
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
//                    editor.putString(Settings.INPUT_TYPE, Constants.TOUCH);
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
//                    editor.putString(Settings.INPUT_TYPE, Constants.GRAVITY);
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
//                    editor.putString(Settings.INPUT_TYPE, Constants.SPEECH);
                    item.setChecked(true);
                }
                break;
            }

            case R.id.action_mi: {
                if(!setInputControl(CONTROL.MI)) {
                    Toast.makeText(this, "Not today!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Self solve set! You can cancel self solving anytime by touching the screen!", Toast.LENGTH_SHORT).show();
//                    editor.putString(Settings.INPUT_TYPE, Constants.MI);
                    item.setChecked(true);
                }
                break;
            }

            case R.id.action_settings: {
                Intent settingsIntent = new Intent(LabyrinthActivity.this, Settings.class);
                //cancel SELF SOLVER if it is still running
                if(inputControl == CONTROL.MI) {
                    if(!solver.isCancelled()) {
                        solver.cancel(true);
                    }
                }
                this.unregisterSensors();
                startActivity(settingsIntent);
            }

        }
//        editor.commit();
        return super.onOptionsItemSelected(item);
    }

    private void readLabyrinth(int resId) {
        Resources res = this.getResources();
        String[] labyrinthRows = res.getStringArray(resId);
        labModel = new LabyrinthModel();
        labModel.initLabyrinth(labyrinthRows);
    }

    private void readLabyrinth(String labyrinth) {
        String[] labyrinthRows = labyrinth.split("#");

        int rows = Integer.parseInt(labyrinthRows[0]);
        int cols = Integer.parseInt(labyrinthRows[0]);
        String[] finalLab = new String[rows];
        System.arraycopy(labyrinthRows, 2, finalLab, 0, rows);
        labModel = new LabyrinthModel();
        labModel.initLabyrinth(finalLab);
    }

    @Override
    protected void onPause() {
        super.onPause();
        wl.release();
        unregisterSensors();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Settings.INPUT_TYPE, inputControl.toString());
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wl.acquire();
        String input = sharedPreferences.getString(Settings.INPUT_TYPE, Constants.TOUCH);
        int sensitivityPercent = sharedPreferences.getInt(Settings.INPUT_SENSITIVITY, 50);
        sensitivity = (G * (100.0 - (double)sensitivityPercent)) / 100.0;
        CONTROL control = this.stringToControl(input);
        if(checkIfHasSensor(control)) {
            inputControl = control;
            this.invalidateOptionsMenu();
        }
        String ballColor = sharedPreferences.getString(Settings.BALL_COLOR, "");
        String labColor = sharedPreferences.getString(Settings.LAB_COLOR, "");
        labView.setBallColor(ballColor);
        labView.setLabyrinthColor(labColor);
        labView.invalidate();
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
        else {
            Log.d(Constants.TAG, "MI_TOUCH");
            if(inputControl == CONTROL.MI) {
                if(solver != null) {
                    if (!solver.isCancelled()) {
                        solver.cancel(true);
                    }
                }
                setInputControl(CONTROL.TOUCH);
                optionsMenu.getItem(1).setChecked(true);
                Toast.makeText(this, "Self solving canceled!", Toast.LENGTH_SHORT ).show();
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

                    if(Math.abs(acceleration[0]) > sensitivity) {
                        if(acceleration[0] > 0) {
                            labModel.left();
                        }
                        else {
                            labModel.right();
                        }
                    }

                    if(Math.abs(acceleration[1]) > sensitivity) {
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

                    if(Math.abs(acceleration[0]) > sensitivity) {
                        if(acceleration[0] > 0) {
                            labModel.left();
                        }
                        else {
                            labModel.right();
                        }
                    }

                    if(Math.abs(acceleration[1]) > sensitivity) {
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
                //TODO
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
                Log.d(Constants.TAG, "MI");
                    this.unregisterSensors();
                    solveThis();
                return true;
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
        long endTime = System.currentTimeMillis();
        long interval = endTime - startTime;
        double ellapsed = interval / 1000.0;
        isEnd = true;
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setTitle(getString(R.string.dialog_title));
        if(currentDiff != MainMenu.DIFFICULTY.HARD && currentDiff != MainMenu.DIFFICULTY.NET) {
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.go_to_mainmenu), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();

                }
            });
            alertDialog.setMessage(getString(R.string.dialog_text_next) + "\n Finished in: " + ellapsed + " seconds!");
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.go_to_next), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("requestCode", Constants.REQ_CODE);
                    setResult(Constants.NEXT_DIFF);
                    finish();
                    isEnd = false;
                    return;
                }
            });
        }
        else {
            if(currentDiff != MainMenu.DIFFICULTY.NET) {
                alertDialog.setMessage("Finished in: " + ellapsed + " seconds!");
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.retry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("requestCode", Constants.REQ_CODE);
                        setResult(Constants.NEXT_NET_LEVEL);
                        finish();
                        isEnd = false;
                        return;
                    }
                });
            }
            else {
                alertDialog.setMessage("Finished in: " + ellapsed + " seconds!");
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.retry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("requestCode", Constants.REQ_CODE);
                        setResult(Constants.NEXT_NET_LEVEL);
                        finish();
                        isEnd = false;
                        return;
                    }
                });
            }
        }

        alertDialog.show();
    }

    //speech recognizer
    //--------------------------------------------------
    @Override
    public void onReadyForSpeech(Bundle bundle) {
        //Log.d(TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        //Log.d(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float v) {
        //Log.d(TAG, "onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        //Log.d(TAG", "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        //Log.d(TAG", "onEndofSpeech");
    }

    @Override
    public void onError(int i) {
        Log.d(Constants.TAG,  "error " + i);
        switch(i) {
            case SpeechRecognizer.ERROR_AUDIO: {
                Log.d(Constants.TAG, "Audio Error!");
                break;
            }
            case SpeechRecognizer.ERROR_CLIENT: {
                Log.d(Constants.TAG, "Client error!");
                break;
            }
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: {
                Log.d(Constants.TAG, "Insufficient permissions!");
                break;
            }
            case SpeechRecognizer.ERROR_NETWORK: {
                Log.d(Constants.TAG, "Network error!");
                break;
            }
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: {
                Log.d(Constants.TAG, "Network timeout!");
                break;
            }
            case SpeechRecognizer.ERROR_NO_MATCH: {
                Log.d(Constants.TAG, "No match found!");
                break;
            }
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: {
                Log.d(Constants.TAG, "Speech timeout!");
                break;
            }

        }
        sr.startListening(listenIntent);
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        //float[] confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

        Log.d(Constants.TAG, "RESULTS:");

        for (int i = 0; i < data.size(); i++)
        {
            //Log.d(TAG, data.get(i));
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

        for (int i = 0; i < data.size(); i++) {
            Log.d(Constants.TAG, data.get(i));
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
        if(input.equals(Constants.TOUCH)) {
            return CONTROL.TOUCH;
        }
        else {
            if(input.equals(Constants.ACCELEROMETER)) {
                return CONTROL.ACCELEROMETER;
            }
            else {
                if(input.equals(Constants.GRAVITY)) {
                    return CONTROL.GRAVITY;
                }
                else {
                    if(input.equals(Constants.SPEECH)) {
                        return CONTROL.SPEECH;
                    }
                    else {
                        if(input.equals(Constants.MI)) {
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
            Log.d(Constants.TAG, actual.getX() + " " + actual.getY());
        }

        solver = new AsyncTask<Integer, Integer, Void>(){
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

            @Override
            protected void onPostExecute(Void aVoid) {
                if(labModel.isWinner()) {
                    winner();
                }
                super.onPreExecute();
            }
        }.execute(path.size());
//        this.winner();

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        Log.d(Constants.TAG, "onPrepareOptionsMenu");
        switch (inputControl) {
            case ACCELEROMETER: {
                MenuItem item = menu.findItem(R.id.action_accelerometer);
                item.setChecked(true);
                break;
            }
            case GRAVITY: {
                MenuItem item = menu.findItem(R.id.action_gravity);
                item.setChecked(true);
                break;
            }
            case TOUCH: {
                MenuItem item = menu.findItem(R.id.action_touch);
                item.setChecked(true);
                break;
            }
            case SPEECH: {
                MenuItem item = menu.findItem(R.id.action_speech);
                item.setChecked(true);
                break;
            }
            case MI: {
                MenuItem item = menu.findItem(R.id.action_mi);
                item.setChecked(true);
                break;
            }
        }
        return true;
    }

}

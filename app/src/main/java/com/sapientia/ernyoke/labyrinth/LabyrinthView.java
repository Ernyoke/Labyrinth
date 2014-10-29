package com.sapientia.ernyoke.labyrinth;

/**
 * Created by Ernyoke on 10/27/2014.
 */
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

public class LabyrinthView extends View implements SensorEventListener{
    private LabyrinthModel model;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int height;
    private int width;
    private int blocks_in_a_col, blocks_in_a_row;

    private Rect[][] Rectangles;

    private Canvas canvas;
    private int green = Color.GREEN;
    private int black = Color.BLACK;
    private int yellow = Color.YELLOW;

    private boolean init = false;

    private float motionStart_x;
    private float motionStart_y;

    private static final int TIME = 300;
    private static final double G = 4;

    private long lastDetection;
    private long currentDetection;

    public enum CONTROL {ACCELEROMETER, TOUCH, GRAVITY};

    private CONTROL inputControl;

    private Context context;
    private SensorManager sensorManager;
    private Sensor activeSensor;
    private List<Sensor> sensorList;

    private boolean isEnd = false;

    private MainMenu.DIFFICULTY currentDiff;


    public LabyrinthView(Context context, LabyrinthModel model, MainMenu.DIFFICULTY currentDiff) {
        super(context);
        this.model = model;
        blocks_in_a_col = model.getRows();
        blocks_in_a_row = model.getCols();
        this.context = context;
        inputControl = CONTROL.TOUCH;
        this.currentDiff = currentDiff;
        getAvailableSensors();
    }

    protected void onDraw(Canvas c) {
        super.onDraw(c);
        canvas = c;

        if (!init) {
            initialize();
            init = true;
        } else {

            paint.setColor(green);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setColor(black);
            canvas.drawRect(4, 4, width - 4, height - 4, paint);
            paint.setColor(green);
            for (int i = 0; i < blocks_in_a_col; i++)
                for (int j = 0; j < blocks_in_a_row; j++)
                    if (model.getElement(i,j) == 1)
                        canvas.drawRect(Rectangles[i][j], paint);
            paint.setColor(yellow);
            canvas.drawRect(
                    Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1], paint);
            paint.setColor(black);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(
                    "EXIT",
                    (Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1].left + Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1].right) / 2,
                    (Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1].top + Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1].bottom) / 2 + 5,
                    paint);

            drawMyCircle(model.getBallrow(), model.getBallcol(), yellow);
        }

    }

    private void initialize() {

        paint.setColor( green );
        height = this.getHeight();// canvas.getHeight();
        width = this.getWidth();// canvas.getWidth();
        canvas.drawRect(0, 0, width, height, paint);
        paint.setColor( black );
        canvas.drawRect(4, 4, width - 4, height - 4, paint);
        paint.setColor( green );

        Rectangles = new Rect[ blocks_in_a_col][ blocks_in_a_row ];

        int last_bottom = 4, new_bottom, last_right, new_right;

        for (int i = 0; i <blocks_in_a_col; i++) {
            last_right = 4;
            new_bottom = (height - 5) * (i + 1) / blocks_in_a_col;
            for (int j = 0; j < blocks_in_a_row; j++) {
                new_right = (width - 5) * (j + 1) / blocks_in_a_row;
                Rectangles[i][j] = new Rect();
                Rectangles[i][j].left = last_right + 1;
                Rectangles[i][j].top = last_bottom + 1;
                Rectangles[i][j].right = new_right;
                Rectangles[i][j].bottom = new_bottom;
                if (model.getElement(i, j) == 1)
                    canvas.drawRect(Rectangles[i][j], paint);
                last_right = new_right;
            }
            last_bottom = new_bottom;

        }

        paint.setColor(yellow);
        canvas.drawRect(Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1],
                paint);
        paint.setColor(black);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(12);
        canvas.drawText(
                "EXIT",
                (Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1].left + Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1].right) / 2,
                (Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1].top + Rectangles[blocks_in_a_col - 1][blocks_in_a_row - 1].bottom) / 2 + 5,
                paint);
        drawMyCircle(model.getBallrow(), model.getBallcol(), yellow);
    }


    private void drawMyCircle(int x, int y, int color) {
        paint.setColor(color);
        canvas.drawCircle((Rectangles[x][y].left + Rectangles[x][y].right) / 2,
                (Rectangles[x][y].top + Rectangles[x][y].bottom) / 2,
                (Rectangles[x][y].right - Rectangles[x][y].left) / 2, paint);
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
                    int edge = width / blocks_in_a_col;
                    if (Math.abs(eventX - motionStart_x) > edge) {
                        if (motionStart_x < eventX) {
                            model.right();
                        } else {
                            model.left();
                        }
                        motionStart_x = eventX;
                    }
                    if (Math.abs(eventY - motionStart_y) > edge) {
                        if (motionStart_y < eventY) {
                            model.down();
                        } else {
                            model.up();
                        }
                        motionStart_y = eventY;
                    }
                    break;
                default:
                    return false;
            }
            invalidate();
            if(model.isWinner()) {
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
                            model.left();
                        }
                        else {
                            model.right();
                        }
                    }

                    if(Math.abs(acceleration[1]) > G) {
                        if(acceleration[1] > 0) {
                            model.down();
                        }
                        else {
                            model.up();
                        }
                    }

                    lastDetection = System.currentTimeMillis();
                    invalidate();

                    if(model.isWinner()) {
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
                            model.left();
                        }
                        else {
                            model.right();
                        }
                    }

                    if(Math.abs(acceleration[1]) > G) {
                        if(acceleration[1] > 0) {
                            model.down();
                        }
                        else {
                            model.up();
                        }
                    }

                    lastDetection = System.currentTimeMillis();
                    invalidate();

                    if(model.isWinner()) {
                        winner();
                    }

                }

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public boolean setInputControl(CONTROL control) {
        if(checkIfHasSensor(control)) {
            this.inputControl = control;
            return true;
        }
        return false;
    }

    public CONTROL getInputControl() {
        return inputControl;
    }

    private void getAvailableSensors() {
        sensorManager = (SensorManager)context.getSystemService(context.SENSOR_SERVICE);
        sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
    }

    private boolean checkIfHasSensor(CONTROL control) {
        switch (control) {
            case ACCELEROMETER: {
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
                sensorManager.unregisterListener(this);
                return  true;
            }
        }
        return false;
    }


    public void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }

    private void winner() {
        unregisterSensors();
        context  = getContext();
        isEnd = true;
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setTitle(context.getString(R.string.dialog_title));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.go_to_mainmenu), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((LabyrinthActivity) context).finish();

            }
        });
        if(currentDiff != MainMenu.DIFFICULTY.HARD) {
            alertDialog.setMessage(context.getString(R.string.dialog_text_next));
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.go_to_next), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("requestCode", MainMenu.REQ_CODE);
                    ((LabyrinthActivity) context).setResult(MainMenu.NEXT_DIFF);
                    ((LabyrinthActivity) context).finish();
                    isEnd = false;
                    return;
                }
            });
        }
        else {
            alertDialog.setMessage(context.getString(R.string.dialog_text_last));
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.retry), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("requestCode", MainMenu.REQ_CODE);
                    ((LabyrinthActivity) context).setResult(MainMenu.NEXT_DIFF);
                    ((LabyrinthActivity) context).finish();
                    isEnd = false;
                    return;
                }
            });
        }

        alertDialog.show();
    }

}

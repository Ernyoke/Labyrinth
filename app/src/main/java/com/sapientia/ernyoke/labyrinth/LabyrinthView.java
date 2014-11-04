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
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class LabyrinthView extends View {
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


    public LabyrinthView(Context context, LabyrinthModel model) {
        super(context);
        this.model = model;
        blocks_in_a_col = model.getRows();
        blocks_in_a_row = model.getCols();
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

    public int getBlockSize() {
        return  width / blocks_in_a_col;
    }


}

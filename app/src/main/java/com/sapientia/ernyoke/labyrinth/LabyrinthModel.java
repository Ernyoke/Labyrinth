package com.sapientia.ernyoke.labyrinth;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.List;

/**
 * Created by Ernyoke on 10/27/2014.
 */
public class LabyrinthModel {
    private int [][]table;
    private int ballcol;
    private int ballrow;
    private int cols;
    private int rows;

    public static final String TAG = "LabyrinthModel";


    public LabyrinthModel() {
        //
        table = null;
    }

    public LabyrinthModel(int rows, int cols) {
        table = new int[rows][cols];
        this.rows = rows;
        this.cols = cols;
    }

    public int getBallcol() {
        return this.ballcol;
    }

    public int getBallrow() {
        return this.ballrow;
    }

    public int getCols() {
        return this.cols;
    }

    public int getRows() {
        return this.rows;
    }

    public int getElement(int x, int y) {
        return table[x][y];
    }

    public void initLabyrinth(String[] lab) {
        rows = lab.length;
        cols = lab[0].length();
        if(table == null) {
            table = new int[rows][cols];
        }
        for(int i = 0; i < rows; ++i) {
            for(int j = 0; j < cols; ++j) {
                if(lab[i].charAt(j) == '0') {
                    table[i][j] = 0;
                }
                else {
                    table[i][j] = 1;
                }
            }
        }

    }

    public void left() {
        if(this.ballcol > 0 && table[ballrow][ballcol - 1] == 0) {
            this.ballcol--;
        }
    }

    public void right() {
        if(this.ballcol < cols - 1 && table[ballrow][ballcol + 1] == 0) {
            this.ballcol++;
        }
    }

    public void up() {
        if(this.ballrow > 0 && table[ballrow - 1][ballcol] == 0) {
            this.ballrow--;
        }
    }

    public void down() {
        if(this.ballrow < rows - 1 && table[ballrow + 1][ballcol] == 0) {
            this.ballrow++;
        }
    }

    public void setBallcol(int ballcol) {
        this.ballcol = ballcol;
    }

    public void setBallrow(int ballrow) {
        this.ballrow = ballrow;
    }

    public void setElement(int x, int y, int element) {
        table[x][y] = element;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public boolean isWinner() {
        if(ballcol == cols - 1 && ballrow == rows - 1) {
            return true;
        }
        return false;
    }

    public void resetBall() {
        ballcol = 0;
        ballrow = 0;
    }

}

package com.sapientia.ernyoke.labyrinth;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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

    public class Position {
        private int x, y;
        private double d;
        private Position prev;
        public Position(int x, int y, double d, Position prev) {
            this.x = x;
            this.y = y;
            this.d = d;
            this.prev = prev;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void setPrev(Position prev) {
            this.prev = prev;
        }

        public Position getPrev() {
            return prev;
        }

        public void setD(double d) {
            this.d = d;
        }

        public double getD() {
            return this.d;
        }

        @Override
        public String toString() {
            return x + " " + y + " ";
        }
    }


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


    public ArrayList<Position> solveSelf() {
        ArrayList<Position> path = new ArrayList<Position>();
        ArrayList<Position> touched = new ArrayList<Position>();
        int stepCount = 0;
        int cur_x = this.ballrow;
        int cur_y = this.ballcol;
        int win_x = this.rows - 1;
        int win_y = this.cols - 1;


        int path_table[][] = new int[rows][cols];

        for(int i = 0; i < rows; ++i) {
            for(int j = 0; j < cols; ++j) {
                path_table[i][j] = table[i][j];
            }
        }

        //set start position
        Double last_key = null;
        path.add(new Position(cur_x, cur_y, pithagoras(cur_x, cur_y) + stepCount,  null));
        path_table[cur_x][cur_y] = 1;

        stepCount++;

        int it = 0;
        while(true) {
            if(path.size() == 0) {
                break;
            }
            Position currentPos = path.get(0);
            cur_x = currentPos.getX();
            cur_y = currentPos.getY();
//            Log.d(TAG", cur_x + " " + cur_y);
            touched.add(currentPos);
            path.remove(0);
            if(cur_x == win_x && cur_y == win_y) {
                break;
            }
            //calculate up
            if(cur_x - 1 >= 0 && path_table[cur_x - 1][cur_y] == 0) {
                Position newPos = new Position(cur_x - 1, cur_y, pithagoras(cur_x - 1, cur_y) + stepCount, currentPos);
                path_table[cur_x - 1][cur_y] = 1;
                addToPath(newPos, path);
            }
            //calculate down
            if(cur_x + 1 < rows && path_table[cur_x + 1][cur_y] == 0) {
                Position newPos = new Position(cur_x + 1, cur_y, pithagoras(cur_x + 1, cur_y) + stepCount, currentPos);
                path_table[cur_x + 1][cur_y] = 1;
                addToPath(newPos, path);
            }
            //calculate left
            if(cur_y - 1 >= 0 && path_table[cur_x][cur_y - 1] == 0) {
                Position newPos = new Position(cur_x, cur_y - 1, pithagoras(cur_x, cur_y - 1) + stepCount, currentPos);
                path_table[cur_x][cur_y - 1] = 1;
                addToPath(newPos, path);
            }
            //calculate right
            if(cur_y + 1 < cols && path_table[cur_x][cur_y + 1] == 0) {
                Position newPos = new Position(cur_x, cur_y + 1, pithagoras(cur_x, cur_y + 1) + stepCount,  currentPos);
                path_table[cur_x][cur_y + 1] = 1;
                addToPath(newPos, path);
            }

            stepCount++;

        }

        Position last =  touched.get(touched.size() - 1);
        ArrayList<Position> result_path = new ArrayList<Position>();

        while(last != null) {
            result_path.add(0, last);
            last = last.getPrev();
        }

        return result_path;

    }

    private void addToPath(Position pos, ArrayList<Position> path) {
        for (int i = 0; i < path.size(); ++i ) {
            Position it = path.get(i);
            if(it.getD() > pos.getD()) {
                path.add(i, pos);
                return;
            }
        }
        path.add(pos);
    }

    private double pithagoras(int x, int y) {
        int end_x = rows - 1;
        int end_y = cols - 1;

        int a = Math.abs(end_x - x);
        int b = Math.abs(end_y - y);

        int c = a * a + b * b;

        double res = Math.sqrt(c);

        return res;
    }

}

package Model;

import Model.BaseStation;
import Model.Cell;

import java.util.ArrayList;

public class Query {

    private int delta_t;
    private int x_left;
    private int T;
    private int x_right;
    private int y_left;
    private int y_right;
    private double errorBound;
    public ArrayList<Cell> intersecting;
    public ArrayList<Cell> covered;
    public double error;
    public int dataSize;
    public int id;
    public boolean success;
    public Cell bottleneck;
    public ArrayList<BaseStation> arr;


    public Query(int T, int delta_t, int x_left, int x_right, int y_left, int y_right, double errorBound, int id) {
        this.T = T;
        this.delta_t = delta_t;
        this.x_left = x_left;
        this.x_right = x_right;
        this.y_left = y_left;
        this.y_right = y_right;
        this.errorBound = errorBound;
        this.intersecting = new ArrayList<>();
        this.covered = new ArrayList<>();
        this.error = 0;
        this.dataSize = 0;
        this.id = id;
        this.success = true;
        this.arr = new ArrayList<>();
    }

    public int getDelta_t() {
        return delta_t;
    }

    public int getT() {
        return T;
    }

    public int getX_left() {
        return x_left;
    }

    public int getX_right() {
        return x_right;
    }

    public int getY_left() {
        return y_left;
    }

    public int getY_right() {
        return y_right;
    }

    public double getErrorBound() {
        return errorBound;
    }
}
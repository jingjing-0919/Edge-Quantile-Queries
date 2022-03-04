package Query;

import java.util.ArrayList;
import java.util.HashMap;

public class Cell {
    private int x_left;
    private int x_right;
    private int y_left;
    private int y_right;
    private int id;
    public ArrayList<BaseStation> arr;
    public int dataVolume;
    public ArrayList<Query> set;
    public double error;
    public double delay;
    public HashMap<BaseStation,Double> yita;
    public int temp_dataVolume;
    public double minError;
    public ArrayList<Integer> quantile;
    public ArrayList<Cell> transferable;



    public Cell(int x_left, int x_right, int y_left, int y_right, int id){
        this.arr = new ArrayList<>();
        this.x_left = x_left;
        this.x_right = x_right;
        this.y_left = y_left;
        this.y_right = y_right;
        this.dataVolume = 0;
        this.set = new ArrayList<>();
        this.error = 0;
        this.id = id;
        this.delay = 0;
        this.yita = new HashMap<>();
        this.temp_dataVolume = 0;
        this.quantile = new ArrayList<>();
        this.transferable = new ArrayList<>();

    }

    public int getY_right() {
        return y_right;
    }

    public int getY_left() {
        return y_left;
    }

    public int getX_right() {
        return x_right;
    }

    public int getX_left() {
        return x_left;
    }

    public int getId() {
        return id;
    }
}
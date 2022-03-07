package Model;

public class BaseStation {
    private double e;
    private double UTC;
    private int id;
    private int delayPer100;//simulate different device's performance
    private int longitude;
    private int latitude;
    private int radius;


    public BaseStation(double e,double UTC,int id,int delayPer100,int longitude,int latitude,int radius){
        this.e = e;
        this.UTC = UTC;
        this.id = id;
        this.delayPer100 = delayPer100;
        this.longitude = longitude;
        this.latitude = latitude;
        this.radius = radius;

    }



    public double getE() {
        return e;
    }

    public int getId() {
        return id;
    }

    public double getUTC() {
        return UTC;
    }

    public int getDelayPer100() {
        return delayPer100;
    }

    public int getLongitude() {
        return longitude;
    }

    public int getLatitude() {
        return latitude;
    }

    public int getRadius() {
        return radius;
    }
}

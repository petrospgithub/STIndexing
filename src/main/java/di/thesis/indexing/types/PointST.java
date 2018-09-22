package di.thesis.indexing.types;

import java.io.Serializable;

public class PointST implements Serializable {

    private static final long serialVersionUID = -8872390899949562146L;
    private double longitude;
    private double latitude;
    private long timestamp;

    public PointST(double longitude, double latitude, long timestamp) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.timestamp = timestamp;
    }

    public PointST () {}

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double distanceEuclidean (PointST p) {
        double xcoord = Math.abs (this.getLongitude() - p.getLongitude());
        double ycoord = Math.abs (this.getLatitude()- p.getLatitude());
        long tcoord = Math.abs (this.getTimestamp() - p.getTimestamp());

        return Math.sqrt((xcoord)*(xcoord) +(ycoord)*(ycoord) +(tcoord)*(tcoord));
    }

    public double distanceManhattan (PointST p) {
        double xcoord = Math.abs (this.getLongitude() - p.getLongitude());
        double ycoord = Math.abs (this.getLatitude()- p.getLatitude());
        long tcoord = Math.abs (this.getTimestamp() - p.getTimestamp());

        return xcoord+ycoord+tcoord;
    }
}

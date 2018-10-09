package di.thesis.indexing.distance;

public class PointManhattan implements PointDistance {
    @Override
    public double calculate(double lat1, double lon1, double lat2, double lon2) {

        double xcoord = Math.abs (lon1 - lon2);
        double ycoord = Math.abs (lat1- lat2);
        return xcoord+ycoord;
    }
}

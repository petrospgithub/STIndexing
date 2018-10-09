package di.thesis.indexing.distance;

public interface PointDistance {
    public double calculate(double lat1, double lon1, double lat2, double lon2);
}

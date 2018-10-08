package di.thesis.indexing.types;

public class Triplet {

    private long id;
    private PointST[] trajectory;
    private double distance;

    public Triplet(long id, PointST[] trajectory) {
        this.id = id;
        this.trajectory = trajectory;
        this.distance = distance;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public PointST[] getTrajectory() {
        return trajectory;
    }

    public void setTrajectory(PointST[] trajectory) {
        this.trajectory = trajectory;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}

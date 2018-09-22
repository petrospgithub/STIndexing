package di.thesis.indexing.octree;

import com.vividsolutions.jts.geom.Envelope;
import di.thesis.indexing.stOperators.Contains;
import di.thesis.indexing.types.EnvelopeST;
import di.thesis.indexing.types.PointST;
import di.thesis.indexing.stOperators.Intersects;

public class OcRectangle {

    private double minX;
    private double maxX;

    private double minY;
    private double maxY;

    private long minT;
    private long maxT;

    private long pid;

    public long getPid() {
        return pid;
    }

    public OcRectangle(EnvelopeST envelope) {
        //super(envelope);
        this.minX=envelope.getMinX();
        this.maxX=envelope.getMaxX();

        this.minY=envelope.getMinY();
        this.maxY=envelope.getMaxY();

        this.minT=envelope.getMinT();
        this.maxT=envelope.getMaxT();

        this.pid=envelope.getGid();
    }

    public double getMaxX() {
        return maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    public long getMinT() {
        return minT;
    }

    public void setMinT(long minT) {
        this.minT = minT;
    }

    public long getMaxT() {
        return maxT;
    }

    public void setMaxT(long maxT) {
        this.maxT = maxT;
    }

    public double getMinX() {
        return this.minX;
    }
    public double getMinY() {
        return this.minY;
    }


    public boolean contains(PointST r) {
        return Contains.apply(this.getMinX(), this.getMaxX(), this.getMinY(), this.getMaxY(), this.getMinT(), this.getMaxT(),
                r.getLongitude(), r.getLatitude(), r.getTimestamp());
    }

    public boolean intersects(PointST r) {
        return Intersects.apply(this.getMinX(), this.getMaxX(), this.getMinY(), this.getMaxY(), this.getMinT(), this.getMaxT(),
                r.getLongitude(), r.getLatitude(), r.getTimestamp());
    }


    public boolean contains(double x, double y, long t) {
        return Contains.apply(this.getMinX(), this.getMaxX(), this.getMinY(), this.getMaxY(), this.getMinT(), this.getMaxT(),
                x, y, t);
    }

    public boolean contains(EnvelopeST r) {
        return Contains.apply(this.getMinX(), this.getMaxX(), this.getMinY(), this.getMaxY(), this.getMinT(), this.getMaxT(),
                r.getMinX(), r.getMaxX(), r.getMinY(), r.getMaxY(), r.getMinT(), r.getMaxT());
    }

    public Envelope getEnvelope() {
        return new EnvelopeST(this.getMinX(), this.getMaxX(), this.getMinY(), this.getMaxY(), this.getMinT(), this.getMaxT());
    }



}

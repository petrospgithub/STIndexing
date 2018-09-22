package di.thesis.indexing.types;

import com.vividsolutions.jts.geom.Envelope;
import org.geotools.geometry.jts.JTS;
import di.thesis.indexing.stOperators.Contains;
import di.thesis.indexing.stOperators.Intersects;

import java.io.Serializable;

public class EnvelopeST extends GidEnvelope implements Serializable {
    private static final long serialVersionUID = -5160997032156330337L;
    private long MinT;
    private long MaxT;
    //private boolean build=false;

    public long getMinT() {
        return MinT;
    }

    public void setMinT(long minT) {
        MinT = minT;
    }

    public long getMaxT() {
        return MaxT;
    }

    public void setMaxT(long maxT) {
        MaxT = maxT;
    }

    public EnvelopeST(double x1, double x2, double y1, double y2, long minT, long maxT) {

        super(x2, x1, y2, y1);
       // this.build=true;
        MinT = minT;
        MaxT = maxT;
    }

    @Override
    public void init(double x1, double x2, double y1, double y2) {
        super.init(x1, x2, y1, y2);
        //this.build=true;
    }

    public void init(EnvelopeST init) {
        super.init(init.getMinX(), init.getMaxX(), init.getMinY(), init.getMaxY());
        this.setMinT(init.getMinT());
        this.setMaxT(init.getMaxT());
        //this.build=true;
    }

    @Override
    public String toString() {
        return "Env[ id: "+this.getGid()+", "+this.getMinX()+" : " +this.getMaxX()+", " +this.getMinY()+" : "+ this.getMaxY()+", " +this.getMinT()+" : "+ this.getMaxT()+"]";
    }

    public PointST center () {
        return new PointST((this.getMaxX()+this.getMinX())/2,
                (this.getMaxY()+this.getMinY())/2,
                (this.getMaxT()+this.getMinT())/2
                );
    }

    public void expandToInclude(EnvelopeST other) {

      double minx=Math.min(this.getMinX(), other.getMinX());
        double maxx=Math.max(this.getMaxX(), other.getMaxX());

        double miny=Math.min(this.getMinY(), other.getMinY());
        double maxy=Math.max(this.getMaxY(), other.getMaxY());

        long mint=Math.min(this.getMinT(), other.getMinT());
        long maxt=Math.max(this.getMaxT(), other.getMaxT());

       //this.init(other.getMinX(), other.getMaxX(), other.getMinY(), other.getMaxY());
//        this.expandToInclude(other);
       this.init(minx,maxx,miny,maxy);
        this.setMinT(mint);
        this.setMaxT(maxt);
    }

    public String wkt(){
        return JTS.toGeometry(new Envelope(this.getMinX(),this.getMaxX(), this.getMinY(), this.getMaxY())).toText();
    }

    public boolean intersects(EnvelopeST env) {
        return Intersects.apply(this.getMinX(), this.getMaxX(), this.getMinY(), this.getMaxY(), this.getMinT(), this.getMaxT(),
                env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(), env.getMinT(), env.getMaxT());
    }

    public boolean intersects(PointST r) {
        return Intersects.apply(this.getMinX(), this.getMaxX(), this.getMinY(), this.getMaxY(), this.getMinT(), this.getMaxT(),
                r.getLongitude(), r.getLongitude(), r.getLatitude(), r.getLatitude(), r.getTimestamp(), r.getTimestamp());
    }

    public boolean intersects(double x, double y, long t) {
        return Intersects.apply(this.getMinX(), this.getMaxX(), this.getMinY(), this.getMaxY(), this.getMinT(), this.getMaxT(),
                x, x, y, y, t, t);
    }

    public boolean contains(PointST r) {
        return Contains.apply(this.getMinX(), this.getMaxX(), this.getMinY(), this.getMaxY(), this.getMinT(), this.getMaxT(),
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

    //public boolean check() {
      //  return build;
    //}
}

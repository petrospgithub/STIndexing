package di.thesis.indexing.types;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.Boundable;

public class GidEnvelope extends Envelope implements Boundable {

    private static final long serialVersionUID = -7578008199641931757L;

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }

    private long gid=-1;

    @Override
    public String toString() {
        return "Env[ id: "+this.getGid()+", "+this.getMinX()+" : " +this.getMaxX()+", " +this.getMinY()+" : "+ this.getMaxY()+"]";
    }

    public GidEnvelope(double x1, double x2, double y1, double y2) {
        super(x1, x2, y1, y2);
    }

    public GidEnvelope(Envelope r) {
        super(r.getMinX(), r.getMaxX(), r.getMinY(), r.getMaxY());
    }

    @Override
    public Object getBounds() {
        return this;
    }

}

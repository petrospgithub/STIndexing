package di.thesis.indexing.types;

import com.vividsolutions.jts.index.strtree.ItemBoundable;

public class STItemBoundable extends ItemBoundable{

    private static final long serialVersionUID = 4918779883156485650L;
    private EnvelopeST mbb;
    private PointST[] traj;
    private PointST p=null;

    public STItemBoundable(Object bounds, Object item) {
        super(bounds, item);
    }

    public STItemBoundable(EnvelopeST bounds) {
        super(bounds, null);
        mbb=bounds;
    }

    public STItemBoundable(EnvelopeST bounds, EnvelopeST item) {
        super(bounds, item);
       // System.out.println(bounds);
        mbb=bounds;
    }

    public STItemBoundable(EnvelopeST bounds, PointST[] item) {
        super(bounds,item);
        mbb=bounds;
        traj=item;
    }

    public PointST getMean() {
        if (p==null) {
            p=meanpoint();
            return p;
        } else {
            return p;
        }
    }

    private PointST meanpoint() {
        double sumx=0.0;
        double sumy=0.0;
        long sumt=0L;

        int i=0;

        while(i<traj.length) {
            sumx=sumx+traj[i].getLongitude();
            sumy=sumy+traj[i].getLatitude();
            sumt=sumt+traj[i].getTimestamp();
            i=i+1;
        }
        PointST pointst=new PointST();

        pointst.setLongitude(sumx/traj.length);
        pointst.setLatitude(sumy/traj.length);
        pointst.setTimestamp(sumt/traj.length);

        return pointst;
    }

    @Override
    public EnvelopeST getBounds() {
        return mbb;
    }


    public PointST[] getTraj() {
        return traj;
    }
}

package di.thesis.indexing.spatiotemporaljts;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.*;
import com.vividsolutions.jts.util.Assert;
import di.thesis.indexing.distance.BoxLineDist;
import di.thesis.indexing.distance.DTW;
import di.thesis.indexing.distance.LCSS;
import di.thesis.indexing.stOperators.Intersects;
import di.thesis.indexing.types.EnvelopeST;
import di.thesis.indexing.types.PointST;
import di.thesis.indexing.types.STItemBoundable;
import di.thesis.indexing.types.Triplet;
import di.thesis.indexing.utils.STtoS;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.SettableStructObjectInspector;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import scala.Tuple2;

import java.util.*;

public class STRtree3D extends STRtree {

    private static final long serialVersionUID = 5410215627654546951L;

    protected STRtree3DNode root;

    private boolean built = false;
    private EnvelopeST datasetMBB;
    private ArrayList<Boundable> itemBoundables = new ArrayList();

    private int nodeCapacity = 10;

    private static final int DEFAULT_NODE_CAPACITY = 10;

    public STRtree3D() {
        this(DEFAULT_NODE_CAPACITY);
    }

    public STRtree3D(int nodeCapacity) {
        Assert.isTrue(nodeCapacity > 1, "Node capacity must be greater than 1");
        this.nodeCapacity = nodeCapacity;
    }

    public EnvelopeST getDatasetMBB() {
        return datasetMBB;
    }

    public void setDatasetMBB(EnvelopeST datasetMBB) {
        this.datasetMBB = datasetMBB;
    }

    public double normX(double x) {
        return (x - datasetMBB.getMinX()) / (datasetMBB.getMaxX() - datasetMBB.getMinX());
    }

    public double normY(double y) {
        return (y - datasetMBB.getMinY()) / (datasetMBB.getMaxY() - datasetMBB.getMinY());
    }

    public double normT(Long t) {
        //System.out.println(t);
        return (t - datasetMBB.getMinT()) / (datasetMBB.getMaxT() - datasetMBB.getMinT());
    }

    @Override
    public int size() {
        if (!built) {
            return 0;
        }
        build();
        return size(root);
    }

    @Override
    protected int size(AbstractNode node)
    {
        int size = 0;
        for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
            Boundable childBoundable = (Boundable) i.next();
            if (childBoundable instanceof AbstractNode) {
                size += size((AbstractNode) childBoundable);
            }
            else if (childBoundable instanceof ItemBoundable) {
                size += 1;
            }
        }
        return size;
    }

    @Override
    public int depth() {
        if (!built) {
            return 0;
        }
        build();
        return depth(root);
    }

    @Override
    protected int depth(AbstractNode node) {
        int maxChildDepth = 0;
        for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
            Boundable childBoundable = (Boundable) i.next();
            if (childBoundable instanceof AbstractNode) {
                int childDepth = depth((AbstractNode) childBoundable);
                if (childDepth > maxChildDepth)
                    maxChildDepth = childDepth;
            }
        }
        return maxChildDepth + 1;
    }
    /*
        public List<EnvelopeST> getLeafNodes() {
            List childBoundables=root.getChildBoundables();
            Stack stack=new Stack();
            ArrayList<EnvelopeST>
            while (stack has next) {
    //TODO
            }

            return null;
        }
    */
    @Override
    public void insert(Object bounds, Object item) {
        Assert.isTrue(!built, "Cannot insert items into an STR packed R-tree after it has been built.");
        itemBoundables.add(new STItemBoundable(bounds, item));
    }

    public void insert(EnvelopeST bounds) {
        Assert.isTrue(!built, "Cannot insert items into an STR packed R-tree after it has been built.");
        itemBoundables.add(new STItemBoundable(bounds));
    }

    public void insert(EnvelopeST bounds, PointST[] item) {
        Assert.isTrue(!built, "Cannot insert items into an STR packed R-tree after it has been built.");
        itemBoundables.add(new STItemBoundable(bounds, item));
    }

    public void insert(EnvelopeST bounds, EnvelopeST item) {
        //System.out.println("insert(EnvelopeST bounds, EnvelopeST item)");
        Assert.isTrue(!built, "Cannot insert items into an STR packed R-tree after it has been built.");
        itemBoundables.add(new STItemBoundable(bounds, item));
    }

    public void insert(EnvelopeST bounds, Object item) {
        Assert.isTrue(!built, "Cannot insert items into an STR packed R-tree after it has been built.");
        itemBoundables.add(new STItemBoundable(bounds, item));
    }

    @Override
    public synchronized void build() {

        if (built) return;

        root = itemBoundables.isEmpty()
                ? createNode(0)
                : createHigherLevels(itemBoundables, -1);
        // the item list is no longer needed
        itemBoundables = null;
        built = true;
    }

    private STRtree3DNode createHigherLevels(List<Boundable> boundablesOfALevel, int level) {
        Assert.isTrue(!boundablesOfALevel.isEmpty());

        //boundablesOfALevel.forEach(x->System.out.println(x));
        //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        List parentBoundables = createParentBoundables(boundablesOfALevel, level + 1);
        if (parentBoundables.size() == 1) {
            return (STRtree3DNode) parentBoundables.get(0);
        }
        return createHigherLevels(parentBoundables, level + 1);
    }

    /**
     * Creates the parent level for the given child level. First, orders the items
     * by the x-values of the midpoints, and groups them into vertical slices.
     * For each slice, orders the items by the y-values of the midpoints, and
     * group them into runs of size M (the node capacity). For each run, creates
     * a new (parent) node.
     */

    protected List createParentBoundables(List childBoundables, int newLevel) {
        //System.out.println("createParentBoundables()");
        Assert.isTrue(!childBoundables.isEmpty());
        //System.out.println(this.nodeCapacity);
        int minLeafCount = (int) Math.ceil((childBoundables.size() / (double) this.nodeCapacity));
        ArrayList sortedChildBoundables=null;
        if (childBoundables.get(0) instanceof Boundable) {
            sortedChildBoundables = new ArrayList<Boundable>(childBoundables);
/* spatial first
            Collections.sort(sortedChildBoundables,
                    Comparator.<Boundable>comparingDouble(arr -> ((EnvelopeST)(arr.getBounds())).getMinX())
                            //.thenComparing(arr -> ((EnvelopeST)(arr.getBounds())).getMinX(), Comparator.naturalOrder())
                            .thenComparing(arr -> ((EnvelopeST)(arr.getBounds())).getMinY(), Comparator.naturalOrder())
            );
*/

/* time first */

            //System.out.println(sortedChildBoundables);

            //System.exit(0);
            Collections.sort(sortedChildBoundables,
                    Comparator.<Boundable>comparingDouble(arr ->
                           normT( ((EnvelopeST)((Boundable)arr).getBounds()).center().getTimestamp() ) )
                            .thenComparing(arr -> normX ( ((EnvelopeST)((Boundable)arr).getBounds()).center().getLongitude() ) , Comparator.naturalOrder() )
                            //.thenComparing(arr -> ((EnvelopeST)(arr.getBounds())).getMinX(), Comparator.naturalOrder())
                            //.thenComparing(arr -> ((EnvelopeST)(arr.getBounds())).getMinY(), Comparator.naturalOrder())
            );

        } else {
            throw new RuntimeException("Illegal Node Type");
        }

        List[] verticalSlices = verticalSlices(sortedChildBoundables,
                (int) Math.ceil(Math.sqrt(minLeafCount)));

        //System.out.println((int) Math.ceil(Math.sqrt(minLeafCount)));
        //System.out.println(verticalSlices[0]);

        return createParentBoundablesFromVerticalSlices(verticalSlices, newLevel);
    }

    private List createParentBoundablesFromVerticalSlices(List[] verticalSlices, int newLevel) {
        //System.out.println("createParentBoundablesFromVerticalSlices()");
        Assert.isTrue(verticalSlices.length > 0);
        List parentBoundables = new ArrayList();
        for (int i = 0; i < verticalSlices.length; i++) {
            parentBoundables.addAll(
                    createParentBoundablesFromVerticalSlice(verticalSlices[i], newLevel));
        }
        return parentBoundables;
    }

    protected List createParentBoundablesFromVerticalSlice(List childBoundables, int newLevel) {
        Assert.isTrue(!childBoundables.isEmpty());
        ArrayList parentBoundables = new ArrayList();
        parentBoundables.add(createNode(newLevel));
        ArrayList sortedChildBoundables = null;

        if (childBoundables.get(0) instanceof Boundable) {
            sortedChildBoundables = new ArrayList<Boundable>(childBoundables);
/*time first */
            Collections.sort(sortedChildBoundables,
                    Comparator.<Boundable>comparingDouble(arr -> normT ( ((EnvelopeST)((Boundable)arr).getBounds()).center().getTimestamp() ) )
                            .thenComparing(arr -> normY( ((EnvelopeST)((Boundable)arr).getBounds()).center().getLatitude() ) )
           );
/*
            Collections.sort(sortedChildBoundables,
                    Comparator.<Boundable>comparingDouble(arr -> ((EnvelopeST)(arr.getBounds())).getMinT())
                            .thenComparing(arr -> ((EnvelopeST)(arr.getBounds())).getMaxT())
            );
  */
        } else {
            throw new RuntimeException("Illegal Node Type");
        }
/*
        sortedChildBoundables.forEach(x->{
            if (x instanceof STItemBoundable) {
                //System.out.println(((STItemBoundable) x).getBounds().wkt());
            } else if (x instanceof STRtree3DNode) {
                //System.out.println(((STRtree3DNode) x).getBounds().wkt());
                System.out.println(((STRtree3DNode) x).getBounds());
            }        });
*/
        //EnvelopeST envelopeST=new EnvelopeST();
        //int count=1;
        for (Iterator i = sortedChildBoundables.iterator(); i.hasNext(); ) {
            Boundable childBoundable = (Boundable) i.next();
            if (lastNode(parentBoundables).getChildBoundables().size() == (double) this.nodeCapacity) {
                parentBoundables.add(createNode(newLevel));
            }

            lastNode(parentBoundables).addChildBoundable(childBoundable);
            //System.out.println(count);
            //count++;
        }
        //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        return parentBoundables;
    }

    /**
     * @param childBoundables Must be sorted by the x-value of the envelope midpoints
     */
    protected List[] verticalSlices(List childBoundables, int sliceCount) {

        //System.out.println(childBoundables);

        int sliceCapacity = (int) Math.ceil(childBoundables.size() / (double) sliceCount);
        List[] slices = new List[sliceCount];
        Iterator i = childBoundables.iterator();
        for (int j = 0; j < sliceCount; j++) {
            slices[j] = new ArrayList();
            int boundablesAddedToSlice = 0;
            while (i.hasNext() && boundablesAddedToSlice < sliceCapacity) {
                Boundable childBoundable = (Boundable) i.next();
                slices[j].add(childBoundable);
                boundablesAddedToSlice++;
            }
        }
        return slices;
    }

    public STRtree3DNode getRoot()
    {
        build();
        return root;
    }

    protected STRtree3DNode createNode(int level) {
        return new STRtree3DNode(level);
    }

    /*
    TODO query
     */

    //add knn for EnvelopeST, mean

    public List<Triplet> knn(PointST[] traj, double threshold, String similarity_function, int k, int minT_tolerance, int maxT_tolerance, String pointDistFunc, int w, double eps, int delta) throws Exception {
        if (root.isEmpty()) {
            return null;
        } else {
            ArrayList<Triplet> matches = new ArrayList<Triplet>();

            long traj_start_t=traj[0].getTimestamp();
            long traj_end_t=traj[traj.length-1].getTimestamp();

            long bound_min_t=root.getBounds().getMinT() - minT_tolerance;
            long bound_max_t=root.getBounds().getMaxT() + maxT_tolerance;

            if (bound_min_t<=traj_end_t && bound_max_t >= traj_start_t) {

                Stack stack = new Stack();

                stack.push(root);

                while (!stack.empty()) {
                    STRtree3DNode node = (STRtree3DNode) stack.pop();
                    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
                        Boundable childBoundable = (Boundable) i.next();

                        if( !( ((EnvelopeST)childBoundable.getBounds()).getMinT() <= traj_end_t &&
                                ((EnvelopeST)childBoundable.getBounds()).getMaxT() >=  traj_start_t) ) {
                            continue;
                        }

                        if (childBoundable instanceof STRtree3DNode) {
                            stack.push(childBoundable);
                        } else if (childBoundable instanceof STItemBoundable) {

                            EnvelopeST envelopeST=((EnvelopeST) childBoundable.getBounds());
                            PointST[] trajectoryB=((STItemBoundable) childBoundable).getTraj();

                            long item_minT=envelopeST.getMinT() - minT_tolerance;
                            long item_maxT=envelopeST.getMinT() + maxT_tolerance;

                            if (item_minT<=traj_end_t && item_maxT >= traj_start_t) {
                                //an intersects add kai min dist 0
                                // alliws an mindist mikrotero apo threshold
                                double dist_result=Double.MAX_VALUE;

                                for (int j=0; j<traj.length; j++) {

                                    if (Intersects.spatial(envelopeST.getMinX(), envelopeST.getMaxX(), envelopeST.getMinY(), envelopeST.getMaxY(),
                                            traj[j].getLongitude(), traj[j].getLatitude())
                                    ) {
                                        dist_result=0;
                                        break;
                                    }

                                }

                                if (dist_result>0) {
                                    LineString line = STtoS.trajectory_transformation(traj);
                                    Polygon poly = envelopeST.jtsGeom();
                                    dist_result= BoxLineDist.minDist(poly,line);
                                }

//TODO check!!!
                                if (dist_result<=threshold) {
                                   // matches.add(envelopeST.getGid());
//todo!!!
                                    Triplet triplet=new Triplet(envelopeST.getGid(), trajectoryB);
                                    matches.add(triplet);
                                }

                            }

                            //todo add min dist and...

                        } else {
                            Assert.shouldNeverReachHere();
                        }
                    }
                }
            }

            if (similarity_function=="DTW") {
                DTW dtw = new DTW(traj);

                for (int i=0; i<matches.size(); i++) {
                    matches.get(i).setDistance(dtw.similarity(matches.get(i).getTrajectory(), w, pointDistFunc, minT_tolerance,maxT_tolerance));
                }


            } else if (similarity_function=="LCSS") {
                LCSS lcss= new LCSS(traj);

                for (int i=0; i<matches.size(); i++) {
                    matches.get(i).setDistance(lcss.similarity(matches.get(i).getTrajectory(), pointDistFunc, eps, delta));
                }

            } else {
                Assert.shouldNeverReachHere();
            }

            matches.sort(Comparator.comparing(Triplet::getDistance));

            return matches.subList(0,k+1);
        }
    }

    public List knn(Object trajectory, ListObjectInspector listOI, SettableStructObjectInspector structOI, double threshold, int minT_tolerance, int maxT_tolerance) {
        if (root.isEmpty()) {
            return null;
        } else {
            ArrayList matches = new ArrayList();

            int last = listOI.getListLength(trajectory)-1;
            int length = listOI.getListLength(trajectory);

            long traj_start_t=((LongWritable) (structOI.getStructFieldData(listOI.getListElement(trajectory, 0), structOI.getStructFieldRef("timestamp")))).get();
            long traj_end_t=((LongWritable) (structOI.getStructFieldData(listOI.getListElement(trajectory, last), structOI.getStructFieldRef("timestamp")))).get();

            long bound_min_t=root.getBounds().getMinT() - minT_tolerance;
            long bound_max_t=root.getBounds().getMaxT() + maxT_tolerance;

            if (bound_min_t<=traj_end_t && bound_max_t >= traj_start_t) {

                Stack stack = new Stack();

                stack.push(root);

                while (!stack.empty()) {
                    STRtree3DNode node = (STRtree3DNode) stack.pop();
                    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
                        Boundable childBoundable = (Boundable) i.next();

                        if( !( ((EnvelopeST)childBoundable.getBounds()).getMinT() <= traj_end_t &&
                                ((EnvelopeST)childBoundable.getBounds()).getMaxT() >=  traj_start_t) ) {
                            continue;
                        }

                        if (childBoundable instanceof STRtree3DNode) {
                            stack.push(childBoundable);
                        } else if (childBoundable instanceof STItemBoundable) {

                            EnvelopeST envelopeST=((EnvelopeST) childBoundable.getBounds());

                            long item_minT=envelopeST.getMinT() - minT_tolerance;
                            long item_maxT=envelopeST.getMinT() + maxT_tolerance;

                            if (item_minT<=traj_end_t && item_maxT >= traj_start_t) {
                                //an intersects add kai min dist 0
                                // alliws an mindist mikrotero apo threshold
                                double dist_result=Double.MAX_VALUE;

                                double lon;
                                double lat;

                                for (int j=0; j<length; j++) {

                                    lon  = ((DoubleWritable) (structOI.getStructFieldData(listOI.getListElement(trajectory, j), structOI.getStructFieldRef("longitude")))).get();
                                    lat= ((DoubleWritable) (structOI.getStructFieldData(listOI.getListElement(trajectory, j), structOI.getStructFieldRef("latitude")))).get();

                                    if (Intersects.spatial(envelopeST.getMinX(), envelopeST.getMaxX(), envelopeST.getMinY(), envelopeST.getMaxY(),
                                            lon, lat)
                                    ) {
                                        dist_result=0;
                                        break;
                                    }

                                }

                                if (dist_result>0) {
                                    LineString line = STtoS.trajectory_transformation(trajectory,listOI,structOI);
                                    Polygon poly = envelopeST.jtsGeom();
                                    dist_result= BoxLineDist.minDist(poly,line);
                                }

//TODO check!!!
                                if (dist_result<=threshold) {
                                    matches.add(envelopeST.getGid());
                                }

                            }

                            //todo add min dist and...

                        } else {
                            Assert.shouldNeverReachHere();
                        }
                    }
                }
            }
            return matches; //return possible id for matches!!!
        }
    }

    public List knn(PointST[] trajectory, double threshold, int minT_tolerance, int maxT_tolerance) {
        if (root.isEmpty()) {
            return null;
        } else {
            ArrayList matches = new ArrayList();

            int length = trajectory.length;

            long traj_start_t=trajectory[0].getTimestamp();
            long traj_end_t=trajectory[length-1].getTimestamp();

            long bound_min_t=root.getBounds().getMinT() - minT_tolerance;
            long bound_max_t=root.getBounds().getMaxT() + maxT_tolerance;

            if (bound_min_t<=traj_end_t && bound_max_t >= traj_start_t) {

                Stack stack = new Stack();

                stack.push(root);

                while (!stack.empty()) {
                    STRtree3DNode node = (STRtree3DNode) stack.pop();
                    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
                        Boundable childBoundable = (Boundable) i.next();

                        if( !( ((EnvelopeST)childBoundable.getBounds()).getMinT() <= traj_end_t &&
                                ((EnvelopeST)childBoundable.getBounds()).getMaxT() >=  traj_start_t) ) {
                            continue;
                        }

                        if (childBoundable instanceof STRtree3DNode) {
                            stack.push(childBoundable);
                        } else if (childBoundable instanceof STItemBoundable) {

                            EnvelopeST envelopeST=((EnvelopeST) childBoundable.getBounds());

                            long item_minT=envelopeST.getMinT() - minT_tolerance;
                            long item_maxT=envelopeST.getMinT() + maxT_tolerance;

                            if (item_minT<=traj_end_t && item_maxT >= traj_start_t) {
                                //an intersects add kai min dist 0
                                // alliws an mindist mikrotero apo threshold
                                double dist_result=Double.MAX_VALUE;

                                double lon;
                                double lat;

                                for (int j=0; j<length; j++) {

                                    lon=trajectory[j].getLongitude();
                                    lat=trajectory[j].getLongitude();

                                    if (Intersects.spatial(envelopeST.getMinX(), envelopeST.getMaxX(), envelopeST.getMinY(), envelopeST.getMaxY(),
                                            lon, lat)
                                    ) {
                                        dist_result=0;
                                        break;
                                    }

                                }

                                if (dist_result>0) {
                                    LineString line = STtoS.trajectory_transformation(trajectory);
                                    Polygon poly = envelopeST.jtsGeom();
                                    dist_result= BoxLineDist.minDist(poly,line);
                                }

                                if (dist_result<=threshold) {
                                    matches.add(envelopeST.getGid());
                                }
                            }

                        } else {
                            Assert.shouldNeverReachHere();
                        }
                    }
                }
            }
            return matches; //return possible id for matches!!!
        }
    }

    public List queryID(EnvelopeST searchBounds) {
        if (root.isEmpty()) {
            return null;
        } else {
            ArrayList matches = new ArrayList();
            if (root.getBounds().intersects(searchBounds)) {

                Stack stack = new Stack();

                stack.push(root);

                while (!stack.empty()) {
                    STRtree3DNode node = (STRtree3DNode) stack.pop();
                    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
                        Boundable childBoundable = (Boundable) i.next();

                        if( !((EnvelopeST)childBoundable.getBounds()).intersects(searchBounds) ) {
                            continue;
                        }

                        if (childBoundable instanceof STRtree3DNode) {
                            stack.push(childBoundable);
                        } else if (childBoundable instanceof STItemBoundable) {
                            //todo mono an intersects add
                            EnvelopeST envelope = (EnvelopeST) (childBoundable).getBounds();

                            if (envelope.intersects(searchBounds)) {
                                matches.add(envelope.getGid());
                            }

                        } else {
                            Assert.shouldNeverReachHere();
                        }
                    }
                }
            }
            //todo kati epipleon gia na elegxw to trajectory!!!
            return matches;
        }

    }

    public List queryIDTrajectory(EnvelopeST searchBounds) {
        if (root.isEmpty()) {
            return null;
        } else {
            ArrayList matches = new ArrayList();
            if (root.getBounds().intersects(searchBounds)) {

                Stack stack = new Stack();

                stack.push(root);

                while (!stack.empty()) {
                    STRtree3DNode node = (STRtree3DNode) stack.pop();
                    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
                        Boundable childBoundable = (Boundable) i.next();

                        if( !((EnvelopeST)childBoundable.getBounds()).intersects(searchBounds) ) {
                            continue;
                        }

                        if (childBoundable instanceof STRtree3DNode) {
                            stack.push(childBoundable);
                        } else if (childBoundable instanceof STItemBoundable) {
                            //todo mono an intersects add
                            PointST[] traj = ((STItemBoundable) childBoundable).getTraj();
                            long rowId = ((EnvelopeST) childBoundable.getBounds()).getGid();

                            for (int iji=0; iji<traj.length; iji++) {
                                if (searchBounds.intersects(traj[iji])) {
                                    matches.add(new Tuple2<Long, PointST[]>(rowId, traj));
                                    break;
                                }
                            }
                        } else {
                            Assert.shouldNeverReachHere();
                        }
                    }
                }
            }
            //todo kati epipleon gia na elegxw to trajectory!!!
            return matches;
        }

    }

    public List getPartitions(int numPartitions) {
        List partitions=null;

        List list_curr=root.getChildBoundables();

        /*
        todo testing
        ti trexei twra: parinw kombo-riza kai koitaw an exei arketa paidia
        an den exei pairnw ola ta paidia apo to epomeno epipedo
        dld eggonia apo riza kok
         */

        int depth=0;

        while (depth<depth()) {
            if (list_curr.size()>=numPartitions) {
                partitions=list_curr;
                return partitions;
            } else {
                if (((STRtree3DNode) list_curr.get(0)).getChildBoundables().get(0) instanceof STRtree3DNode) {
                    List temp = new ArrayList();
                    for (int i = 0; i < list_curr.size(); i++) {
                        temp.addAll(((STRtree3DNode) list_curr.get(i)).getChildBoundables());
                    }
                    list_curr = new ArrayList(temp);
                }
                else {
                    throw new RuntimeException("Invalid num of partitions");
                }
            }

            /* TODO!!!
            if(list_curr.size()>=numPartitions) {
                partitions=list_curr;
            }
            */
            depth++;
        }

        return partitions;
    }

    /*
    //check implementation stark vs jtplus
    @Override
    public Object nearestNeighbour(Envelope env, Object item, ItemDistance itemDist) {
        return super.nearestNeighbour(env, item, itemDist);
    }
*/
    /*
    @Override
    public Object[] kNearestNeighbour(Envelope env, Object item, ItemDistance itemDist, int k) {
        return super.kNearestNeighbour(env, item, itemDist, k);
    }
*/
    public static final class STRtree3DNode extends AbstractNode {
        //todo na balw mbb ka8e kombou!!!
//change addchildboundable implementation!!!!
        //change getChildBoundables implementaion!!!
        private EnvelopeST nodeMBB = null;

        @Override
        public void addChildBoundable(Boundable childBoundable) {
            super.addChildBoundable(childBoundable);

            //System.out.println(childBoundable.getBounds());

            //if (!nodeMBB.check()) {
                //nodeMBB.init((EnvelopeST) childBoundable.getBounds());
            //} else {
            if (nodeMBB==null) {
                EnvelopeST temp=(EnvelopeST) childBoundable.getBounds();
                nodeMBB=new EnvelopeST(//temp.getGid(),
                        temp.getMinX(),
                        temp.getMaxX(),
                        temp.getMinY(),
                        temp.getMaxY(),
                        temp.getMinT(),
                        temp.getMaxT());
            } else {
                nodeMBB.expandToInclude((EnvelopeST) childBoundable.getBounds());
            }
            //}
            //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~");

            //System.out.println(nodeMBB);
            //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~");

        }

        @Override
        public EnvelopeST getBounds() {
            return nodeMBB;
        }

        public STRtree3DNode(int level)
        {
            super(level);
        }

        protected EnvelopeST computeBounds() {
            return getBounds();
        }
    }


    //todo add knn


}
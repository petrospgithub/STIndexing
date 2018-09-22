package di.thesis.indexing.spatiotemporaljts;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.*;
import com.vividsolutions.jts.util.Assert;
import di.thesis.indexing.types.EnvelopeST;
import di.thesis.indexing.types.PointST;
import di.thesis.indexing.types.STItemBoundable;

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


    public List queryID(EnvelopeST searchBounds) {
        if (root.isEmpty()) {
            return null;
        } else {
            ArrayList matches = new ArrayList();
            if (getIntersectsOp().intersects(root.getBounds(), searchBounds)) {

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

    //check implementation stark vs jtplus
    @Override
    public Object nearestNeighbour(Envelope env, Object item, ItemDistance itemDist) {
        return super.nearestNeighbour(env, item, itemDist);
    }

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


}
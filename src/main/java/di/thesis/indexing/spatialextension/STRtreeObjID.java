package di.thesis.indexing.spatialextension;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.AbstractNode;
import com.vividsolutions.jts.index.strtree.Boundable;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.util.Assert;
import di.thesis.indexing.types.GidEnvelope;

import java.util.*;

public class STRtreeObjID extends STRtree {

    private static final long serialVersionUID = 1113799434508676095L;

    private Envelope datasetMBB;

    public Envelope getDatasetEnvelope() {
        return datasetMBB;
    }

    public void setDatasetEnvelope(Envelope datasetEnvelope) {
        this.datasetMBB = datasetEnvelope;
    }

    public double normX(double x) {
        return (x - datasetMBB.getMinX()) / (datasetMBB.getMaxX() - datasetMBB.getMinX());
    }

    public double normY(double y) {
        return (y - datasetMBB.getMinY()) / (datasetMBB.getMaxY() - datasetMBB.getMaxY());
    }

    public STRtreeObjID() {
    }

    public STRtreeObjID(int nodeCapacity) {
        super(nodeCapacity);
    }


    //TODO
    protected List createParentBoundables(List childBoundables, int newLevel) {
        Assert.isTrue(!childBoundables.isEmpty());
        int minLeafCount = (int) Math.ceil((childBoundables.size() / (double) getNodeCapacity()));
        ArrayList sortedChildBoundables = new ArrayList(childBoundables);

        //Collections.sort(sortedChildBoundables, xComparator);

        Collections.sort(sortedChildBoundables,
                Comparator.<Boundable>comparingDouble(arr ->
                        normX(((Envelope) ((arr.getBounds()))).centre().x))
                //.thenComparing(arr -> normX ( ((EnvelopeST)(arr.getBounds())).getMinX() ) , Comparator.naturalOrder() )
                //.thenComparing(arr -> ((EnvelopeST)(arr.getBounds())).getMinX(), Comparator.naturalOrder())
                //.thenComparing(arr -> ((EnvelopeST)(arr.getBounds())).getMinY(), Comparator.naturalOrder())
        );


        List[] verticalSlices = verticalSlices(sortedChildBoundables,
                (int) Math.ceil(Math.sqrt(minLeafCount)));
        return createParentBoundablesFromVerticalSlices(verticalSlices, newLevel);
    }

    private List createParentBoundablesFromVerticalSlices(List[] verticalSlices, int newLevel) {
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
        ArrayList sortedChildBoundables = new ArrayList(childBoundables);

        Collections.sort(sortedChildBoundables,
                Comparator.<Boundable>comparingDouble(arr ->
                        normY(((Envelope) ((arr.getBounds()))).centre().y))
                //.thenComparing(arr -> normX ( ((EnvelopeST)(arr.getBounds())).getMinX() ) , Comparator.naturalOrder() )
                //.thenComparing(arr -> ((EnvelopeST)(arr.getBounds())).getMinX(), Comparator.naturalOrder())
                //.thenComparing(arr -> ((EnvelopeST)(arr.getBounds())).getMinY(), Comparator.naturalOrder())
        );

        for (Iterator i = sortedChildBoundables.iterator(); i.hasNext(); ) {
            Boundable childBoundable = (Boundable) i.next();
            if (lastNode(parentBoundables).getChildBoundables().size() == getNodeCapacity()) {
                parentBoundables.add(createNode(newLevel));
            }
            lastNode(parentBoundables).addChildBoundable(childBoundable);
        }
        return parentBoundables;
    }

    /**
     * @param childBoundables Must be sorted by the x-value of the envelope midpoints
     */
    protected List[] verticalSlices(List childBoundables, int sliceCount) {
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

    public List queryID(Envelope searchBounds) {

        if (super.getRoot().isEmpty()) {
            return null;
        } else {
            ArrayList matches = new ArrayList();
            if (getIntersectsOp().intersects(root.getBounds(), searchBounds)) {

                Stack stack = new Stack();

                stack.push(root);

                while (!stack.empty()) {
                    AbstractNode node = (AbstractNode) stack.pop();
                    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
                        Boundable childBoundable = (Boundable) i.next();
                        if (!getIntersectsOp().intersects(childBoundable.getBounds(), searchBounds)) {
                            continue;
                        }
                        if (childBoundable instanceof AbstractNode) {
                            stack.push(childBoundable);
                        } else if (childBoundable instanceof ItemBoundable) {
                            //todo mono an intersects add

                            GidEnvelope envelope = (GidEnvelope) ((ItemBoundable) childBoundable).getBounds();

                            if (envelope.intersects(searchBounds)) {
                                matches.add(envelope.getGid());
                            }

                        } else {
                            Assert.shouldNeverReachHere();
                        }
                    }
                }
            }
            //todo kati epipleon gia na elegxw tin geometria!!!
            return matches;
            //see queryIDGeom
        }
    }

    public List queryIDGeom(Envelope searchBounds) {
        if (super.getRoot().isEmpty()) {
            return null;
        } else {
            ArrayList matches = new ArrayList();
            if (getIntersectsOp().intersects(root.getBounds(), searchBounds)) {

                Stack stack = new Stack();

                stack.push(root);

                while (!stack.empty()) {
                    AbstractNode node = (AbstractNode) stack.pop();
                    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
                        Boundable childBoundable = (Boundable) i.next();
                        if (!getIntersectsOp().intersects(childBoundable.getBounds(), searchBounds)) {
                            continue;
                        }
                        if (childBoundable instanceof AbstractNode) {
                            stack.push(childBoundable);
                        } else if (childBoundable instanceof ItemBoundable) {
                            //TODO!!!!!!!!!
                            GidEnvelope envelope = (GidEnvelope) ((ItemBoundable) childBoundable).getBounds();
                            Geometry geom = (Geometry) ((ItemBoundable) childBoundable).getItem();

                            matches.add(new AbstractMap.SimpleImmutableEntry<>(envelope.getGid(), geom));
                        } else {
                            Assert.shouldNeverReachHere();
                        }
                    }
                }

            }
            //todo kati epipleon gia na elegxw tiw geometries!!!
            return matches;
        }

    }

    public List getPartitions(int numPartitions) {
        List partitions = null;

        List list_curr = root.getChildBoundables();

        /*
        todo testing
        ti trexei twra: parinw kombo-riza kai koitaw an exei arketa paidia
        an den exei pairnw ola ta paidia apo to epomeno epipedo
        dld eggonia apo riza kok
         */

        int depth = 0;

        while (depth < depth()) {
            if (list_curr.size() >= numPartitions) {
                partitions = list_curr;
                return partitions;
            } else {
                //if (((AbstractNode) list_curr.get(0)).getChildBoundables().get(0) instanceof STRtree3D.STRtree3DNode) {
                List temp = new ArrayList();
                for (int i = 0; i < list_curr.size(); i++) {
                    temp.addAll(((AbstractNode) list_curr.get(i)).getChildBoundables());
                }
                list_curr = new ArrayList(temp);
                //}
                /*
                else {
                    throw new RuntimeException("Invalid num of partitions");
                }
                */
            }

            //TODO
            /*
            if(list_curr.size()>=numPartitions) {
                partitions=list_curr;
            }
             */

            depth++;
        }

        return partitions;
    }
}

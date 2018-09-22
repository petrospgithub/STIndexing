package di.thesis.indexing.octree;

import di.thesis.indexing.types.EnvelopeST;
import di.thesis.indexing.types.PointST;

import java.util.*;

public class StandardOcTree /*extends StandardQuadTree*/ {

    private static final int REGION_SELF = -1;

    private ArrayList<OcNode> mynodes = new ArrayList<>();
    private static int mymaxItemByNode = 5;
    private static int mymaxLevel = 10;

    private int mylevel;
    private int mynodeNum=0;

    private EnvelopeST myzone;

    private StandardOcTree[] regions;

    StandardOcTree(EnvelopeST definition, int level, int maxItemByNode, int maxLevel) {
        //super(definition, level);
        mylevel=level;
        //System.out.println(mylevel);
        mymaxItemByNode=maxItemByNode;
        mymaxLevel=maxLevel;
        this.myzone= definition;
    }
/*
    @Override
    public void forceGrowUp(int minLevel) {
        super.forceGrowUp(minLevel);
    }
*/
    private void split() {
        //System.out.println("mpike split");
        //System.exit(0);
        regions = new StandardOcTree[8];

        int newLevel = mylevel + 1;
        //System.out.println(newLevel);
        //System.exit(0);

        PointST center = new PointST();

        center.setLongitude( (this.myzone.getMaxX()+this.myzone.getMinX())/2 );
        center.setLatitude( (this.myzone.getMaxY()+this.myzone.getMinY())/2 );
        center.setTimestamp( ((this.myzone.getMaxT()+this.myzone.getMinT())/2)+1 ) ;

        regions[0] = new StandardOcTree(
                new EnvelopeST(
                this.myzone.getMinX(),
                center.getLongitude(),
                this.myzone.getMinY(),
                center.getLatitude(),
                this.myzone.getMinT(),
                center.getTimestamp()

        ), newLevel, mymaxItemByNode,mymaxLevel);

        regions[1] = new StandardOcTree(
                new EnvelopeST(
                        center.getLongitude(),
                        this.myzone.getMaxX(),
                        this.myzone.getMinY(),
                        center.getLatitude(),
                        this.myzone.getMinT(),
                        center.getTimestamp()
                )
        , newLevel, mymaxItemByNode,mymaxLevel);

        regions[2] = new StandardOcTree(
                new EnvelopeST(
                        this.myzone.getMinX(),
                        center.getLongitude(),
                        center.getLatitude(),
                        this.myzone.getMaxY(),
                        this.myzone.getMinT(),
                        center.getTimestamp()
                )
        , newLevel, mymaxItemByNode,mymaxLevel);

        regions[3] = new StandardOcTree(
                new EnvelopeST(
                        this.myzone.getMinX(),
                        center.getLongitude(),
                        this.myzone.getMinY(),
                        center.getLatitude(),
                        center.getTimestamp(),
                        this.myzone.getMaxT()
                )
        , newLevel, mymaxItemByNode,mymaxLevel);

        regions[4] = new StandardOcTree(
                new EnvelopeST(
                        center.getLongitude(),
                        this.myzone.getMaxX(),
                        center.getLatitude(),
                        this.myzone.getMaxY(),
                        this.myzone.getMinT(),
                        center.getTimestamp()
                )
        , newLevel, mymaxItemByNode,mymaxLevel);

        regions[5] = new StandardOcTree(
                new EnvelopeST(
                        center.getLongitude(),
                        this.myzone.getMaxX(),
                        this.myzone.getMinY(),
                        center.getLatitude(),
                        center.getTimestamp(),
                        this.myzone.getMaxT()
                )
        , newLevel, mymaxItemByNode,mymaxLevel);

        regions[6] = new StandardOcTree(
                new EnvelopeST(
                        this.myzone.getMinX(),
                        center.getLongitude(),
                        center.getLatitude(),
                        this.myzone.getMaxY(),
                        center.getTimestamp(),
                        this.myzone.getMaxT()
                )
        , newLevel, mymaxItemByNode,mymaxLevel);

        regions[7] = new StandardOcTree(
                new EnvelopeST(
                        center.getLongitude(),
                        this.myzone.getMaxX(),
                        center.getLatitude(),
                        this.myzone.getMaxY(),
                        center.getTimestamp(),
                        this.myzone.getMaxT()
                )
        , newLevel, mymaxItemByNode,mymaxLevel);
        //System.exit(0);
    }

    private int findRegion(EnvelopeST r, boolean split) {
        //todo
        int region = REGION_SELF;
        /*
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        System.out.println("mynodeNum: "+mynodeNum);
        System.out.println("mymaxItemByNode: "+mymaxItemByNode);
        System.out.println("mylevel: "+mylevel);
        System.out.println("mymaxLevel: "+mymaxLevel);
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        */
        if (mynodeNum >= mymaxItemByNode && mylevel < mymaxLevel) {
            // we don't want to split if we just need to retrieve
            // the region, not inserting an element
            if (regions == null && split) {
                // then create the subregions
                this.split();
            }

            // can be null if not splitted
            if (regions != null) {

                if (regions[0].getZone().contains(r.center())) { //todo
                    region = 0;
                } else if (regions[1].getZone().contains(r.center())) {
                    region = 1;
                } else if (regions[2].getZone().contains(r.center())) {
                    region = 2;
                } else if (regions[3].getZone().contains(r.center())) {
                    region = 3;
                } else if (regions[4].getZone().contains(r.center())) {
                    region = 4;
                } else if (regions[5].getZone().contains(r.center())) {
                    region = 5;
                } else if (regions[6].getZone().contains(r.center())) {
                    region = 6;
                } else if (regions[7].getZone().contains(r.center())) {
                    region = 7;
                }
            }
        }

        return region;
    }

    public void ocinsert(EnvelopeST r, int element) {
        //super.insert(r, element);
        int region = this.findRegion(r, true);

        //gia performance gia na min krataw ta elements isws  exw mono to prwto if else kai oxi to redispatch!!!!

        if (region == REGION_SELF || this.mylevel == mymaxLevel) {
            mynodes.add(new OcNode(r, element));
            mynodeNum++;
            return;
        } else {
            regions[region].ocinsert(r, element);
        }

        /*
        if (mynodeNum >= mymaxItemByNode && this.mylevel < mymaxLevel) {
            // redispatch the elements
            ArrayList<OcNode> tempNodes = new ArrayList<OcNode>();
            int length = mynodes.size();
            for (int i = 0; i < length; i++) {
                tempNodes.add(mynodes.get(i));
            }
            mynodes.clear();

            for (OcNode node : tempNodes) {
                this.ocinsert(node.r, node.element);
            }
        }
        */
    }

    //@Override
    protected EnvelopeST getZone() {
        return this.myzone;
    }

    public void getLeafNodeOctree(List uniqueIdList)
    {
        //System.out.println(this.myzone.getEnvelope());
        if (regions!= null) {
            //System.out.println("if (regions!= null)");
            regions[0].getLeafNodeOctree(uniqueIdList);
            regions[1].getLeafNodeOctree(uniqueIdList);
            regions[2].getLeafNodeOctree(uniqueIdList);
            regions[3].getLeafNodeOctree(uniqueIdList);

            regions[4].getLeafNodeOctree(uniqueIdList);
            regions[5].getLeafNodeOctree(uniqueIdList);
            regions[6].getLeafNodeOctree(uniqueIdList);
            regions[7].getLeafNodeOctree(uniqueIdList);
        } else {
            // This is a leaf node
            //System.out.println("else");
            //Tuple2 tuple2=new Tuple2(myzone.hashCode(),myzone.getEnvelope());
            //System.out.println("mpike");
            //System.out.println(tuple2);
            //System.out.print(myzone);
            //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            //System.out.println(((EnvelopeST) myzone.getEnvelope()).wkt());
            //System.out.println(myzone.getEnvelope());
            //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            uniqueIdList.add(myzone);
        }
    }

    public long search(PointST pointST) {

        Stack<StandardOcTree[]> stack = new Stack();

        long partition_id=Long.MAX_VALUE;

        boolean done=false;

        stack.push(regions);

        while (!stack.empty() && !done) {

            StandardOcTree[] current= stack.pop();

            //System.out.println(Arrays.asList(current));

            for (int i=0; i<current.length; i++) {
                if (current[i].myzone.contains(pointST)) {

                    //System.out.println(current[i].myzone.getEnvelope());

                    if (current[i].regions!=null) {
                        stack.push(current[i].regions);
                    } else {

                        partition_id=current[i].myzone.hashCode();
                        System.out.println("zone:"+current[i].myzone);
                        current[i].mynodes.forEach(f->System.out.println(f.r.hashCode()));

                        //System.out.println("zone:"+);
                        done=true;
                        break;

                    }
                }
            }
        }
        return partition_id;
    }

/*
    public EnvelopeST getPartitionNode(PointST p)
    {
        //System.out.println(Arrays.toString(regions));
        if (regions!= null) {

            if (regions[0].myzone.contains(p)) {
                regions[0].getPartitionNode(p);
            } else if (regions[1].myzone.contains(p)) {
                regions[1].getPartitionNode(p);
            } else if (regions[2].myzone.contains(p)) {
                regions[2].getPartitionNode(p);
            } else if (regions[3].myzone.contains(p)) {
                regions[3].getPartitionNode(p);
            } else if (regions[4].myzone.contains(p)) {
                regions[4].getPartitionNode(p);
            } else if (regions[5].myzone.contains(p)) {
                regions[5].getPartitionNode(p);
            } else if (regions[6].myzone.contains(p)) {
                regions[6].getPartitionNode(p);
            } else if (regions[7].myzone.contains(p)) {
                regions[7].getPartitionNode(p);
            } else {
                return (EnvelopeST) this.myzone.getEnvelope();
            }

            /*
            regions[0].getAllLeafNodeUniqueId(uniqueIdList);
            regions[1].getAllLeafNodeUniqueId(uniqueIdList);
            regions[2].getAllLeafNodeUniqueId(uniqueIdList);
            regions[3].getAllLeafNodeUniqueId(uniqueIdList);

            regions[4].getAllLeafNodeUniqueId(uniqueIdList);
            regions[5].getAllLeafNodeUniqueId(uniqueIdList);
            regions[6].getAllLeafNodeUniqueId(uniqueIdList);
            regions[7].getAllLeafNodeUniqueId(uniqueIdList);

        } else {
            return (EnvelopeST) myzone.getEnvelope();
            // This is a leaf node
            //uniqueIdList.add(new Tuple2(myzone.hashCode(),myzone));
        }
        return null;
    }
*/
}

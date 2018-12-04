package di.thesis.indexing.octree;

import com.vividsolutions.jts.geom.Envelope;
import di.thesis.indexing.types.EnvelopeST;
import di.thesis.indexing.types.PointST;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class OctreePartitioning implements Serializable {

    private StandardOcTree partitionTree;
    //private static int code=0;

    public OctreePartitioning(List<EnvelopeST> SampleList, EnvelopeST boundary, int maxItemByNode, int maxLevel) {
        //System.out.println(boundary.wkt());
        //QuadRectangle foo=new OcRectangle(boundary);
        partitionTree = new StandardOcTree(boundary, 0, maxItemByNode, maxLevel);
        int count=1;
        for (EnvelopeST aSampleList : SampleList) {
            //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            //System.out.print(count+"\t");
            //System.out.println(aSampleList.wkt());
            partitionTree.ocinsert(aSampleList, 1);
            //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            count++;
        }
        //HashSet uniqueIdList = new HashSet();
        //partitionTree.getLeafNodeUniqueIdOctree(uniqueIdList);
        //System.out.println(uniqueIdList.size());
        //System.out.println(uniqueIdList);
        //System.exit(0);
    }

    //public StandardOcTree getPartitionTree() {
        //return partitionTree;
    //}

    //public void setPartitionTree(StandardOcTree partitionTree) {
        //this.partitionTree = partitionTree;
    //}

    public List<EnvelopeST> getLeadfNodeList() {
        List<EnvelopeST> uniqueIdList = new ArrayList();
        partitionTree.getLeafNodeOctree(uniqueIdList);
        //System.out.println(uniqueIdList.size());
/*
        for (EnvelopeST t:uniqueIdList) {
            System.out.println(t.wkt());
        }
*/
        return uniqueIdList;
    }
/*
    public Tuple2<Object, EnvelopeST> Partitioning(di.thesis.indexing.type.PointST p) {
        EnvelopeST env=partitionTree.getPartitionNode(p);
        if (env!=null) {

            synchronized (this) {
                return new Tuple2<>(code + 1, env);
            }
            //return new Tuple2<>(Objects.hashCode(env), env);
        } else {
            return new Tuple2<>(null, null);
        }
    }
    */

    public StandardOcTree getPartitionTree() {
        return partitionTree;
    }
}

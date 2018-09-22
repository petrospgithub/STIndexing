package di.thesis.indexing.octree;

import di.thesis.indexing.types.EnvelopeST;


public class OcNode { //xreiazetai to T?
    EnvelopeST r;
    int element;

    public OcNode(EnvelopeST r, int element) {
        this.r=r;
        this.element=element;
    }
}

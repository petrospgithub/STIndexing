package di.thesis.indexing.distance;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class BoxLineDist {

    public static double minDist(Polygon poly, LineString line) {

        double result=Double.MAX_VALUE;

        for (int i=0; i<4; i++) {
            Point p=(Point)poly.getGeometryN(i);

            double temp=DistanceOp.distance(p,line);

            double temp1=DistanceOp.distance(p,line.getStartPoint());

            double temp2=DistanceOp.distance(p,line.getEndPoint());

            result=Math.min(Math.min(Math.min(result,temp),temp1), temp2);

        }

        return result;
    }

}

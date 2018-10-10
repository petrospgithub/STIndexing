package di.thesis.indexing.distance;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class BoxLineDist {

    public static double minDist(Polygon poly, LineString line) {

        double result=Double.MAX_VALUE;
        Coordinate[] arr=poly.getCoordinates();

        GeometryFactory gf=new GeometryFactory();

        for (int i=0; i<4; i++) {
            Coordinate coord=arr[i];

            Point p=gf.createPoint(coord);

            double temp=DistanceOp.distance(p,line);

            double temp1=DistanceOp.distance(p,line.getStartPoint());

            double temp2=DistanceOp.distance(p,line.getEndPoint());

            result=Math.min(Math.min(Math.min(result,temp),temp1), temp2);

        }

        return result;
    }

}

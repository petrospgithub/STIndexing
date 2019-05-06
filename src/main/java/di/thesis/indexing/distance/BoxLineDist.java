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



            //result=Math.min(Math.min(Math.min(result,temp),temp1), temp2);
            result=Math.min(result,temp);
        }

        double temp1=DistanceOp.distance(line.getStartPoint(),poly);

        double temp2=DistanceOp.distance(line.getEndPoint(), poly);

        return Math.min(Math.min(result,temp1), temp2);
    }

}

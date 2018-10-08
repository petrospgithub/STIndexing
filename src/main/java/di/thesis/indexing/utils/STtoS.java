package di.thesis.indexing.utils;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import di.thesis.indexing.types.PointST;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.SettableStructObjectInspector;

public class STtoS {

    public static LineString trajectory_transformation(PointST[] traj) {

        GeometryFactory gf=new GeometryFactory();
        Coordinate[] coord=new Coordinate[traj.length];

        int i=0;

        while (i<traj.length) {
            coord[i]=new Coordinate(traj[i].getLongitude(), traj[i].getLatitude());
            i++;
        }

        return gf.createLineString(coord);
    }

    public static LineString trajectory_transformation(Object trajectory, ListObjectInspector listOI, SettableStructObjectInspector structOI) {

        int length = listOI.getListLength(trajectory);


        GeometryFactory gf=new GeometryFactory();
        Coordinate[] coord=new Coordinate[length];

        int i=0;

        double lon;
        double lat;

        while (i<length) {

            lon  = (double) (structOI.getStructFieldData(listOI.getListElement(trajectory, i), structOI.getStructFieldRef("longitude")));
            lat= (double) (structOI.getStructFieldData(listOI.getListElement(trajectory, i), structOI.getStructFieldRef("latitude")));

            coord[i]=new Coordinate(lon, lat);
            i++;
        }

        return gf.createLineString(coord);
    }

    public static LineString trajectory_transformation(String geojson) {

        LineString geometry = new GsonBuilder()
                .registerTypeAdapterFactory(new GeometryAdapterFactory())
                .create()
                .fromJson(geojson, LineString.class);

        return geometry;
    }
}

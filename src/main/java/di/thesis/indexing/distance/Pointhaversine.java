package di.thesis.indexing.distance;

public class Pointhaversine implements PointDistance{
    @Override
    public double calculate(double lat1, double lon1, double lat2, double lon2) {

        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(deltaLat / 2.0D), 2) + Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(lat1)) * Math.pow(Math.sin(deltaLon / 2.0D), 2);
        double greatCircleDistance = 2.0D * Math.atan2(Math.sqrt(a), Math.sqrt(1.0D - a));
        //3958.761D * greatCircleDistance           //Return value in miles
        //3440.0D * greatCircleDistance             //Return value in nautical miles
        return 6371000.0D * greatCircleDistance;            //Return value in meters, assuming Earth radius is 6371 km

    }
}

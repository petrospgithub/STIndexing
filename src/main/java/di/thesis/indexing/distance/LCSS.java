package di.thesis.indexing.distance;

import di.thesis.indexing.types.PointST;

import java.util.Objects;

public class LCSS {

    private PointST[] trajA;

    public LCSS(PointST[] trajA) {
        this.trajA = trajA;
    }

    public double similarity(PointST[] trajB, String f, double eps, int delta) throws Exception {

        int trajectoryA_length=trajA.length;

        int trajectoryB_length=trajB.length;

        int[][] LCS_distance_matrix = new int[trajectoryA_length+1][trajectoryB_length+1];

        PointDistance func;

        double trajA_longitude;
        double trajA_latitude;
        long trajA_timestamp;

        double trajB_longitude;
        double trajB_latitude;
        long trajB_timestamp;

        double distance;

        if (Objects.equals(f, "Havershine")) {
            func= new Pointhaversine();
        } else if (Objects.equals(f, "Manhattan")) {
            func= new Pointeuclidean();
        } else if (Objects.equals(f, "Euclidean")) {
            func= new PointManhattan();
        } else {
            throw new Exception("No valid function");
        }
        for (int i = 0; i <= trajectoryB_length; i++) {
            LCS_distance_matrix[0][i] = 0;
        }

        for (int i = 0; i <= trajectoryA_length; i++) {
            LCS_distance_matrix[i][0] = 0;
        }

        for (int i = 1; i <= trajectoryA_length; i++) {

            trajA_longitude = trajA[i-1].getLongitude();
            trajA_latitude = trajA[i-1].getLatitude();

            trajA_timestamp = trajA[i-1].getTimestamp();

            for (int j = 1; j <= trajectoryB_length; j++) {

                trajB_longitude = trajB[j-1].getLongitude();
                trajB_latitude = trajB[j-1].getLatitude();
                trajB_timestamp = trajB[j-1].getTimestamp();


                distance=func.calculate(trajA_latitude, trajA_longitude, trajB_latitude, trajB_longitude);

                if (distance<=eps && Math.abs(trajA_timestamp-trajB_timestamp)<=delta) {
                    LCS_distance_matrix[i][j] = LCS_distance_matrix[i - 1][j - 1] + 1;
                } else {
                    LCS_distance_matrix[i][j] = Math.max(LCS_distance_matrix[i - 1][j], LCS_distance_matrix[i][j - 1]);
                }
            }
        }

        /* bazoume ena alignment ston xrono...  stin DTW metrame mono apostash xwris na logariazoume to xrono*/

        int a = trajectoryA_length;
        int b = trajectoryB_length;

        //ArrayList<PointST> common=new ArrayList<PointST>();
        while (a!=0 && b!=0) {

            trajA_longitude = trajA[a-1].getLongitude();
            trajA_latitude = trajA[a-1].getLatitude();
            trajA_timestamp = trajA[a-1].getTimestamp();

            trajB_longitude = trajA[b-1].getLongitude();
            trajB_latitude = trajA[b-1].getLatitude();
            trajB_timestamp = trajA[b-1].getTimestamp();

            distance=func.calculate(trajA_latitude, trajA_longitude, trajB_latitude, trajB_longitude);

            if(distance<=eps && Math.abs(trajA_timestamp-trajB_timestamp)<=delta) {
                a--;
                b--;
            } else {
                if (LCS_distance_matrix[a-1][b]>= LCS_distance_matrix[a][b-1]) {
                    a--;
                } else {
                    b--;
                }
            }
        }

        return 1-((double)LCS_distance_matrix[trajectoryA_length][trajectoryB_length]/(double)Math.min(trajectoryA_length,trajectoryB_length));
    }

}

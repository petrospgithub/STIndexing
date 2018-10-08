package di.thesis.indexing.distance;

import di.thesis.indexing.types.PointST;

import java.util.Objects;

public class DTW {
    private PointST[] trajA;

    public DTW(PointST[] trajA) {
        this.trajA = trajA;
    }

    public double similarity(PointST[] trajB, int w, String f, double d, int minTSext, int maxTSext) throws Exception {

        int trajectoryA_length=trajA.length;

        int trajectoryB_length=trajB.length;

        PointDistance func;

        if (Objects.equals(f, "Havershine")) {
            func= new Pointhaversine();
        } else if (Objects.equals(f, "Manhattan")) {
            func= new Pointeuclidean();
        } else if (Objects.equals(f, "Euclidean")) {
            func= new PointManhattan();
        } else {
            throw new Exception("No valid function");
        }

        long min_tsA = trajA[0].getTimestamp();

        long max_tsA = trajA[trajectoryA_length-1].getTimestamp();

        long min_tsB = trajB[0].getTimestamp();
        long max_tsB = trajB[trajectoryA_length-1].getTimestamp();

        //an einai arnhtika kai ta 2 sigoura DTW
        //an einai 8etika kai ta 2 koitaw an kanoun overlap

        if (minTSext<0 || maxTSext<0) {
            double calc=calculate(trajectoryA_length, trajectoryB_length, trajA, trajB, func, w);

            double traj_dist=(calc / (double)Math.min(trajectoryA_length,trajectoryB_length));

            if (traj_dist<=d) {
                return traj_dist;
            } else {
                return -1;
            }

        } else if ( (min_tsA-minTSext)<=max_tsB && min_tsB<=(max_tsA+maxTSext) ) {
            double calc=calculate(trajectoryA_length, trajectoryB_length, trajA, trajB, func, w);

            double traj_dist=(calc / (double)Math.min(trajectoryA_length,trajectoryB_length));

            if (traj_dist<=d) {
                return traj_dist;
            } else {
                return -1;
            }
        } else {
            return -1;
        }

       // return 0.0;
    }

    private double calculate (int trajectoryA_length, int trajectoryB_length, PointST[] trajA, PointST[] trajB, PointDistance func, int w) {

        double[][] DTW_distance_matrix=new double[trajectoryA_length][trajectoryB_length];

        double trajA_longitude;
        double trajA_latitude;

        double trajB_longitude;
        double trajB_latitude;

        double distance;

        trajA_longitude=trajA[0].getLongitude();
        trajA_latitude=trajA[0].getLatitude();

        trajB_longitude=trajB[0].getLongitude();
        trajB_latitude=trajB[0].getLatitude();

        DTW_distance_matrix[0][0]=func.calculate(trajA_latitude, trajA_longitude, trajB_latitude, trajB_longitude);
        for (int i = 0; i < trajectoryB_length; i++) {

            trajB_longitude=trajB[i].getLongitude();
            trajB_latitude=trajB[i].getLatitude();

            DTW_distance_matrix[0][i] = func.calculate(trajA_latitude, trajA_longitude, trajB_latitude, trajB_longitude);
            //LOGGER.log(Level.WARNING, String.valueOf(func.distance(trajA_latitude, trajA_longitude, trajB_longitude, trajB_latitude)));
        }

        trajB_longitude=trajB[0].getLongitude();
        trajB_latitude=trajB[0].getLatitude();

        for (int i = 0; i < trajectoryA_length; i++) {
            trajA_longitude=trajA[i].getLongitude();
            trajA_latitude=trajA[i].getLatitude();

            DTW_distance_matrix[i][0] = DTW_distance_matrix[0][0]=func.calculate(trajA_latitude, trajA_longitude, trajB_latitude, trajB_longitude);
        }

        w = Math.max(w, Math.abs(trajectoryA_length-trajectoryB_length)); // adapt window size (*)
        for (int i = 1; i < trajectoryA_length; i++) {

            trajA_longitude=trajA[i-1].getLongitude();
            trajA_latitude=trajA[i-1].getLatitude();

            for (int j = Math.max(1, i-w); j < Math.min(trajectoryB_length, i+w); j++) {

                trajB_longitude=trajB[j-1].getLongitude();
                trajB_latitude=trajB[j-1].getLatitude();


                distance=func.calculate(trajA_latitude, trajA_longitude, trajB_latitude, trajB_longitude);

                DTW_distance_matrix[i][j]=distance+
                        Math.min(Math.min(DTW_distance_matrix[i-1][j], DTW_distance_matrix[i][j-1]), DTW_distance_matrix[i-1][j-1]);

            }
        }

        return DTW_distance_matrix[trajectoryA_length-1][trajectoryB_length-1];
    }

}

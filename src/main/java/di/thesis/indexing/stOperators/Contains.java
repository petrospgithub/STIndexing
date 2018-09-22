package di.thesis.indexing.stOperators;

public class Contains {

    public static boolean apply(double a_minx, double a_maxx, double a_miny, double a_maxy, long a_mint, long a_maxt,
                         double b_x, double b_y, long b_t) {

       return a_mint <= b_t && b_t <= a_maxt &&
                a_minx <= b_x && b_x <= a_maxx &&
                a_miny <= b_y && b_y <= a_maxy;

    }

    public static boolean apply(double a_minx, double a_maxx, double a_miny, double a_maxy, long a_mint, long a_maxt,
                                double b_minx, double b_maxx, double b_miny, double b_maxy, long b_mint, long b_maxt) {

        return  a_minx <= b_minx && b_maxx<= a_maxx &&
                a_miny <= b_miny && b_maxy <= a_maxy &&
                a_mint <= b_mint && b_maxt <= a_maxt;

    }
}

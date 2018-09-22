package di.thesis.indexing.stOperators;

public class Intersects {
    public static boolean apply(double a_minx, double a_maxx, double a_miny, double a_maxy, long a_mint, long a_maxt,
                                double b_minx, double b_maxx, double b_miny, double b_maxy, long b_mint, long b_maxt) {

        return b_maxx >= a_minx && b_minx<= a_maxx &&
                b_maxy >= a_miny && b_miny <= a_maxy &&
                b_maxt >= a_mint && b_mint <= a_maxt;
    }

    public static boolean apply(double a_minx, double a_maxx, double a_miny, double a_maxy, long a_mint, long a_maxt,
                                double b_x, double b_y, long b_t) {

        return b_x >= a_minx && b_x<= a_maxx &&
                b_y >= a_miny && b_y <= a_maxy &&
                b_t >= a_mint && b_t <= a_maxt;
    }
}

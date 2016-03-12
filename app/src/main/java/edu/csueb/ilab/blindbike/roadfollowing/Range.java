package edu.csueb.ilab.blindbike.roadfollowing;

/**
 * Created by williamoverell on 2/28/16.
 */
public class Range {
    /**
     * Minimum of Range
     */
    int r_min;
    int g_min;
    int b_min;

    /**
     * Maximum of Range
     */
    int r_max;
    int g_max;
    int b_max;

    /**
     * Center of Range
     */
    float r_center;
    float g_center;
    float b_center;

    /**
     * Creates a range
     * @param r1 - red min
     * @param r2 - red max
     * @param rc - red center
     * @param g1 - green min
     * @param g2 - green max
     * @param gc - green center
     * @param b1 - blue min
     * @param b2 - blue max
     * @param bc - blue center
     */
    public Range (int r1, int r2, float rc, int g1, int g2, float gc, int b1, int b2, int bc){
        this.r_min = r1;
        this.r_max = r2;
        this.r_center = rc;
        this.g_min = g1;
        this.g_max = g2;
        this.g_center = gc;
        this.b_min = b1;
        this.b_max = b2;
        this.b_center = bc;
    }

    /**
     * Takes an r,g,b values and returns true if this is within the range
     * false if it is not
     * @param r - red value
     * @param g - green value
     * @param b - blue value
     * @return boolean value
     */
    public boolean is_in_range(int r, int g, int b)
    {
        if(r >= this.r_min && r <= this.r_max &&
                g >= this.g_min && g <= this.g_max &&
                b >= this.b_min && b <= this.b_max){
            return true;
        }
        else
            return false;
    }
}

package edu.csueb.ilab.blindbike.roadfollowing;

import java.util.Vector;

/**
 * Created by williamoverell on 2/28/16.
 */
public class ClassedMultipleFixedRangeModel {

    Vector<Range> range_vector;

    // Pseudocolor used to represent this class. 4 = (r,g,b,a)
    double pseudocolor[];

    public ClassedMultipleFixedRangeModel(){
        range_vector = new Vector<>();
        pseudocolor = new double[4];
    }

    /**
     * Add a range to the object
     * @param range_to_add
     */
    public void add_Range(Range range_to_add){
        this.range_vector.add(range_to_add);
    }

    /**
     * Returns array of -1 if not in one of the ranges, otherwise returns the centers
     * of the ranges of r,g,b it is inside and is closest to its center.
     * @param r
     * @param g
     * @param b
     * @return cur_centers
     */
    public float[] is_In_Ranges(int r, int g, int b){
        float cur_smallest_center_diff = -1;
        float cur_center_diff;
        float[] cur_centers = {-1.0f,-1.0f,-1.0f}; // 0 = red, 1 = green, 2 = blue
        for(Range cur_range: this.range_vector){
            if(cur_range.is_in_range(r, g, b)){
                cur_center_diff = ((r - cur_range.r_center)*(r - cur_range.r_center) + (g - cur_range.g_center)*(g - cur_range.g_center) + (b - cur_range.b_center)*(b - cur_range.b_center));
                if(cur_smallest_center_diff == -1) {
                    cur_smallest_center_diff = cur_center_diff;
                    cur_centers[0] = cur_range.r_center;
                    cur_centers[1] = cur_range.g_center;
                    cur_centers[2] = cur_range.b_center;
                }
                else if (cur_center_diff <= cur_smallest_center_diff) {
                    cur_smallest_center_diff = cur_center_diff;
                    cur_centers[0] = cur_range.r_center;
                    cur_centers[1] = cur_range.g_center;
                    cur_centers[2] = cur_range.b_center;
                }
            }
        }
        return cur_centers;
    }


    /**
     * returns true if r,g,b   is closer to the point center (an rgb point) than point best_center (also rgb point)
     * @param r
     * @param g
     * @param b
     * @param center
     * @param best_center
     * @return
     */

    static boolean betterCenter(double r,double g, double b, float center[],float best_center[]){

        float distance =  (float) ((r-center[0])*(r-center[0]) + (g-center[1])*(g-center[1]) + (b-center[2])*(b-center[2]));

        float distance_best =  (float) ((r-best_center[0])*(r-best_center[0]) + (g-best_center[1])*(g-best_center[1]) + (b-best_center[2])*(b-best_center[2]));


        if(distance < distance_best)
            return true;
        else
            return false;


    }

}

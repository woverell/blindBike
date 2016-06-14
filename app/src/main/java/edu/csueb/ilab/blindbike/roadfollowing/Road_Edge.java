package edu.csueb.ilab.blindbike.roadfollowing;

import android.util.Log;

import org.opencv.core.Mat;

import edu.csueb.ilab.blindbike.blindbike.BB_Parameters;

/**
 * Created by Lynne on 5/15/2016.
 */
public class Road_Edge {


    /**
     * This method fits a single Vertical line  simply by taking the average of the column values of
     * the right most contour points in the binary image inputData (the 255 values) going from
     * rows top_row down to bottom_row
     * @param inputData  binary Countour image where contour points are 255 and 0 is background
     * @param row_top   top row of horizontal slice of image want to examine
     * @param row_bottom  bottom row of horizontal slice of image want to examine
     * @return topLineParams[i][j]  where i = 0 for 1 line (to be compatibale with follow on multi-line processing
     * algorithm and j = 0 for theta value of polar line description (theta = 0 always as looking for fit to
     * vertical line --really average column values determine placement of line)
     * and j = 1for rho value of polar line description and
     * j=2 for number of contour points that were used in the calculation of the Vertical Edge
     * j=3 for certainy --is a metric from 0 to 100% giving a sense of how close the right most contour points
     * are in our horizontal slice to this
     */
    public static double[][] fitVerticalEdgeFromLowestRightCountourPoints(Mat inputData, int row_top, int row_bottom) {

        int width = inputData.width();
        int height = inputData.height();
        double rho;  //this represents the average column value of the right most countour points
        // in our horizontal Image slice we are lookin at
        // which is EQUIVALENT to the distance to the polar perfect vertical line(theta = 0)
        // running through these right most countour points

        double outOfImageRho = -20.0 * Math.sqrt((width*width)+(height*height));

        double numVotes = 0; // represents number of contour points used in calculating our vertical line edge
        double certainty = 0.0; // represents the certainty associated with this vertical line edge fit
        // is normalized sum of distances of right most countour points from vertical line edge

        int[] columnValuesRightMostCountourPoints = new int[height]; //column values for each right most point
        // for the countour point in that row
        // set to -1 if row not used or outside Image Slize

        //initialize array to -1
        for (int i = 0; i < height; i++)
            columnValuesRightMostCountourPoints[i] = -1;

        //Will return a single hence 1 line rather than multiple lines
        double[][] topLineParams = new double[1][4]; // theta, rho, numvotes, certainty




        //cycle through the HORIZONTAL image SLICE from row_top to row_bottom and calculate
        // the average column value for our "BEST" fit vertical line thrugh the Right Most
        // contour points in each Row.


        //Saftey Checks row_top should be less than half the image and > row_bottom
        //              row_bottom should be less than row_top and less than half the image and > height
        //DO not process the EDGE of the image (last column or last row)

        int row_min, row_max, col_max;
        row_min = row_top;
        row_max = row_bottom;

        if (row_min < height / 2)  // we only want to process the LOWER right most contour
            row_min = height / 2;
        if (row_min >= height-2) // too big  --> use the default
            row_min = height /2;

        if  (row_max <= row_min) // no vertical slice  Max smaller than min --> use default
        {
            row_max = height - 2;
        }

        if (row_max >= height) //do not process edge of image
        {
            row_max = height - 2;
        }



        // do not process any countour points in the last column as it is not known if this is truely
        //  a contour point or simply boundary of image.
        col_max = width - 2; // do not want to process last column as edge of image not necessarily contour edge


        int sum_column_value = 0;
        //Visit each pixel in our Horizontal Slice
        for (int y = row_max; y >= row_min; y--) {
            for (int x = col_max; x >= 0; x--)  // find right most countour point in row y
            {
                if (inputData.get(y, x)[0] > 0) // if we have Countour Point at this pixel
                {   //we have hit the RIGHT MOST CONTOUR
                    columnValuesRightMostCountourPoints[y] = x;
                    sum_column_value += x;
                    numVotes++;
                    x = -1; // exit out of this ROW
                }
            }
        }


        //Calculate rho value as average of column value
        if(numVotes > 0)
            rho = (double)sum_column_value/(double)numVotes;
        else   //no contour points in this Horizontal Slice
            rho = outOfImageRho;  // make the vertical line run to the left Image
                                         // which we will ignore


        //calculate Certainty = 100 - 100 * (SumDistanceCountourPoint from VerticalEdge  ) / (MaxSumPossible)
        //Visit rightMost Contour Column value in our Horizontal Slice
        double sum_distances = 0;
        if(numVotes > 0) {
            for (int y = row_max; y >= row_min; y--) {

                if (columnValuesRightMostCountourPoints[y] > 0)
                    sum_distances += Math.abs(rho - columnValuesRightMostCountourPoints[y]);
            }
        }


        double ave_distance;
        if(numVotes > 0) {
            ave_distance = sum_distances /(double) numVotes;  //average distance from the column

            certainty = 100.0 - 100.0 * (ave_distance /(double) col_max); //normalize factor = max average distance possible is the col_max of image
        }
        else
            certainty = 0.0;


        //this algorithm fits bets perfect Vertical line, so Theta is 0
        topLineParams[0][0] = 0.0;
        topLineParams[0][1] = rho;
        topLineParams[0][2] = numVotes;
        topLineParams[0][3] = certainty;
        Log.i("Road_Edge", " theta =0,  rho=" + Double.toString(rho) + " With numVotes =" + Double.toString(numVotes) +"  certainty= " +Double.toString(certainty));

        return topLineParams;
    }




}

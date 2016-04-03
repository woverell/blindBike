package edu.csueb.ilab.blindbike.roadfollowing;

import org.opencv.core.Mat;

/**
 * Created by williamoverell on 2/27/16.
 */
public class Hough_Lines {
    public static void houghTransform(Mat inputData, ArrayData outputData,  int thetaAxisSize, int rAxisSize, boolean ignoreEdges)
    {
        houghTransformVerticalLines(inputData, outputData, thetaAxisSize,rAxisSize, 90, 90, ignoreEdges); // WILL: Think this is wrong, would process no lines not all lines
    }


    /**
     * computes HoughTransform where the polar space is partintioned for r into rAxisSize bins and for theta into thetaAxisSize bins
     *   for the binary inputData where 255 are the pixels of interest.  Note, does this in limited theta ranges to in essence find only
     *    "close" to vertical ranges of the Hough Space --- this in particular will calculate for the 0 to theta1 (which translates from Vertical to theta1
     *      angled lines in cartesian coordinates)  and AGAIN for the range of theta2 to 180degrees (which translates from -theta1 angled lines to Vertical in
     *      cartesian coordinates)
     */
    public static void houghTransformVerticalLines(Mat inputData, ArrayData outputData, int thetaAxisSize, int rAxisSize, float theta1, float theta2, boolean ignoreEdges)
    {
        int width = inputData.width();
        int height = inputData.height();
        int maxRadius = (int)Math.ceil(Math.hypot(width, height));
        int halfRAxisSize = rAxisSize >>> 1;
        // x output ranges from 0 to pi
        // y output ranges from -maxRadius to maxRadius
        double[] sinTable = new double[thetaAxisSize];
        double[] cosTable = new double[thetaAxisSize];

        // populate the sin and cosine tables for faster lookup
        for (int theta = thetaAxisSize - 1; theta >= 0; theta--)
        {
            double thetaRadians = theta * Math.PI / thetaAxisSize;
            sinTable[theta] = Math.sin(thetaRadians);
            cosTable[theta] = Math.cos(thetaRadians);
        }

        if(theta1 >= 180.0 || theta1 < 0  || theta2<0 || theta2>180 || theta2<theta1)
        {theta1 = 90;  theta2 = 180; } //saftey check for bad parameter specification

        int theta1Bin = Math.round((float)((theta1 / 180) * (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        int theta2Bin = Math.round((float)((theta2 / 180) * (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        if(theta1Bin == theta2Bin)
            theta1Bin = Math.max(0, theta1Bin - 1); // reduce theta1 by one or set to 0
        if(theta1Bin ==0 && theta2Bin==0) // if both are 0 then ...
            theta2Bin = thetaAxisSize / 2; // WILL: set theta2 bin to half??

        System.out.println("theta1Bin: " + theta1Bin + " theta2Bin: " +theta2Bin);

        int rows, cols;
        if(ignoreEdges){
            rows = height - 5;
            cols = width - 5;
        }else{
            rows = height - 1;
            cols = width - 1;
        }

        for (int y = rows; y >= 0; y--)
        {
            for (int x = cols; x >= 0; x--)
            {
                if (inputData.get(y,x)[0] > 0) //point want to analyize OLD CODE: if (inputData.contrast(x, y, minContrast))
                {
                    for(int theta = 0; theta <= theta1Bin; theta++)
                    {
                        double r = cosTable[theta] * x + sinTable[theta] * y;
                        int rScaled = (int)Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                        outputData.accumulate(theta, rScaled, 1);
                    }
                    for(int theta = theta2Bin; theta <= thetaAxisSize - 1; theta ++)
                    {
                        double r = cosTable[theta] * x + sinTable[theta] * y;
                        int rScaled = (int)Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                        outputData.accumulate(theta, rScaled, 1);
                    }
                }
            }
        }
    }

    public static class ArrayData
    {
        public final int[] dataArray;
        public final int width;
        public final int height;

        public ArrayData(int width, int height)
        {
            this(new int[width * height], width, height);
        }

        public ArrayData(int[] dataArray, int width, int height)
        {
            this.dataArray = dataArray;
            this.width = width;
            this.height = height;
        }

        public int get(int x, int y)
        {  return dataArray[y * width + x];  }

        public void set(int x, int y, int value)
        {  dataArray[y * width + x] = value;  }

        public void accumulate(int x, int y, int delta)
        {  set(x, y, get(x, y) + delta);  }

        public boolean contrast(int x, int y, int minContrast)
        {
            int centerValue = get(x, y);
            for (int i = 8; i >= 0; i--)
            {
                if (i == 4)
                    continue;
                int newx = x + (i % 3) - 1;
                int newy = y + (i / 3) - 1;
                if ((newx < 0) || (newx >= width) || (newy < 0) || (newy >= height))
                    continue;
                if (Math.abs(get(newx, newy) - centerValue) >= minContrast)
                    return true;
            }
            return false;
        }

        public int getMax()
        {
            int max = dataArray[0];
            for (int i = width * height - 1; i > 0; i--)
                if (dataArray[i] > max)
                    max = dataArray[i];
            return max;
        }
    }

    /**
     * localMaxNeighborhoodSpanParam represents the creation of a neighborhood of width 2 * localMaxNeighborhoodSpanParam + 1 and height of the same
     * that we search for finding a local maximum in the hough accumulator space.  This effectively results in not saving multiple nearby r, theta values
     * that represent the same line and does a kind of smoothing of our hough accumulator space.
     */
    public static double[][] findLines(ArrayData arrayData, int heightOfOrig, int widthOfOrig, int numTopLines, int minNumVotes, int localMaxNeighborhoodSpanParam)
    {
        double[][] topLineParams = new double[numTopLines][3]; // theta, rho, numvotes

        int halfRaxisSize = arrayData.height >>> 1;
        int maxRadius = (int)Math.ceil(Math.hypot(heightOfOrig, widthOfOrig));

        int max = 0;
        int currentValue = 0;
        for(int width = 0; width < arrayData.width; width++)
        {
            for(int height = 0; height < arrayData.height; height++)
            {
                // LOOPING THROUGH ALL BUCKETS
                currentValue = arrayData.get(width, height); // get the current bucket value

                // If the line doesnt have the min number of votes then don't save it
                if(currentValue < minNumVotes)
                    continue;
                // see if a larger voted bin exists in surrounding neighborhood
                // if so then don't save it
                // size of neighborhood is
                for(int r = -1 * localMaxNeighborhoodSpanParam; r <= localMaxNeighborhoodSpanParam; r++)
                    for(int theta = -1 * localMaxNeighborhoodSpanParam; theta <= localMaxNeighborhoodSpanParam; theta++)
                    {
                        if((height + r >= 0 && height + r < arrayData.height) && ( width + theta >= 0 && width + theta <= arrayData.width))
                            if(arrayData.get(width + theta, r + height) > currentValue)
                            {
                                max = arrayData.get(width+theta, r+height);
                                r = localMaxNeighborhoodSpanParam + 1;
                                theta = localMaxNeighborhoodSpanParam + 1;
                            }
                    }
                if(max > currentValue) // don't save our current found value if there is a bigger accumulator value in our neighborhood
                    continue;

                // loop through all current top values
                for(int i = 0; i < numTopLines; i++)
                {
                    // if it's bigger than any of them
                    if(currentValue > topLineParams[i][2])
                    {
                        // if checking the last spot then no need to copy down
                        if(i == numTopLines - 1)
                        {
                            // copy over the current into the correct spot
                            topLineParams[i][0] = width * Math.PI / arrayData.width;
                            topLineParams[i][1] = (height - halfRaxisSize) * maxRadius / halfRaxisSize;;
                            topLineParams[i][2] = currentValue;
                        }
                        else
                        {
                            // then loop through the rest of the top lines moving them all down
                            for(int j = numTopLines - 1; j > i; j--) // array out of bounds at numTopLines + 1 so add if statement before this
                            {
                                topLineParams[j][0] = topLineParams[j-1][0];
                                topLineParams[j][1] = topLineParams[j-1][1];
                                topLineParams[j][2] = topLineParams[j-1][2];
                            }
                            // copy over the current into the correct spot
                            topLineParams[i][0] = width * Math.PI / arrayData.width;
                            topLineParams[i][1] = (height - halfRaxisSize) * maxRadius / halfRaxisSize;
                            topLineParams[i][2] = currentValue;
                        }
                        currentValue = 0;  // get out of the loop
                    }
                }
            }
        }
        // END OF LOOPING THROUGH BUCKETS

        return topLineParams;
    }

}

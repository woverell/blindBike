package edu.csueb.ilab.blindbike.roadfollowing;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import edu.csueb.ilab.blindbike.blindbike.BB_Parameters;

/**
 * Class containing all methods pertaining to the hough transform and finding lines in the hough space.
 * Created by williamoverell on 2/27/16.
 */
public class Hough_Lines {
    /**
     * computes HoughTransform where the polar space is partitioned for r into rAxisSize bins and for theta into thetaAxisSize bins
     *   for the binary inputData where 255 are the pixels of interest.
     * @param inputData - The contour edge input image
     *  @param outputData - The grid in rho, theta space representing the hough space as a 1d array
     *  @param thetaAxisSize - number of theta buckets
     *  @param rAxisSize - number of rho buckets
     * @param ignoreEdges - set to true to not process the boundary pixels in the image
     */
    public static void houghTransform(Mat inputData, ArrayData outputData,  int thetaAxisSize, int rAxisSize, boolean ignoreEdges)
    {
        houghTransformVerticalLines(inputData, outputData, thetaAxisSize,rAxisSize, 90, 90, ignoreEdges);
    }

    /**
     * computes HoughTransform where the polar space is partitioned for r into rAxisSize bins and for theta into thetaAxisSize bins
     *   for the binary inputData where 255 are the pixels of interest.  Note, does this in limited theta ranges to in essence find only
     *    "close" to vertical ranges of the Hough Space --- this in particular will calculate for the 0 to theta1 (which translates from Vertical to theta1
     *      angled lines in cartesian coordinates)  and AGAIN for the range of theta2 to 180degrees (which translates from -theta1 angled lines to Vertical in
     *      cartesian coordinates)
     *  @param inputData - The contour edge input image
     *  @param outputData - The grid in rho, theta space representing the hough space as a 1d array
     *  @param thetaAxisSize - number of theta buckets
     *  @param rAxisSize - number of rho buckets
     *  @param theta1 - will search from 0 degrees up to theta1 (max range 0 to 90)- Quadrant 2 (SE) NOTE: theta1 = 0   is vertical line and 90 is horizontal line
     *  @param theta2 - ALSO will search from theta2 to 180 (max range 90 to 180)- Quadrant 3 NOTE:  theta2=90 degrees is horizontal line and 180 is vertical line
     *                NOTE: to cut OUT horizontal lines (+/-10 degrees from horizontal) specify theta1 = 80  and theta2 = 110
     *                NOTE: to do +/- from vertical lines theta1 =45 and theta2 = 135
     *
     *  @param ignoreEdges - set to true to not process the boundary pixels in the image
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

        if(theta1 >= 90.0 || theta1 < 0  || theta2<90 || theta2>180 || theta2<theta1)
            {theta1 = 90;  theta2 = 90; } //saftey check for bad parameter specification

        int theta1Bin = Math.round((float)((theta1 / 180) * (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        int theta2Bin = Math.round((float)((theta2 / 180) * (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        if(theta1Bin == theta2Bin)
            theta1Bin = Math.max(0, theta1Bin - 1); // reduce theta1 by one or set to 0
        if(theta1Bin ==0 && theta2Bin==0) // if both are 0 then ...//this makes no sense
            theta2Bin = thetaAxisSize / 2; // WILL: set theta2 bin to half??

        Log.v("Theta Bin Size","theta1Bin: " + theta1Bin + " theta2Bin: " +theta2Bin);

        // not processing the boundary of the image
        int rows, cols, startrow, startcol;
        if(ignoreEdges){
            rows = height - 5;
            cols = width - 5;
            startrow = 5;
            startcol = 5;
        }else{
            rows = height - 1;
            cols = width - 1;
            startrow = 0;
            startcol = 0;
        }

        for (int y = rows; y >= startrow; y--)
        {
            for (int x = cols; x >= startcol; x--)
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

    /**
     * Determine if angle is between two ranges
     * ranges are from 0 to theta1 where the actual line is perpendicular to this angle
     * and theta2 to 180
     * in polar, so an angle of 0 is a vertical line
     * Passed angle is in polar from -pi/2 to pi/2 and is perpendicular to angle of line
     * @param angle
     * @param theta1
     * @param theta2
     * @return
     */
    private static boolean inRange(double angle, float theta1, float theta2){

        // Correct the angle which is in form of -pi/2 to pi/2 into our 0 to 180 system
        angle = Math.toDegrees(angle); // convert radian angle to degrees
        if(angle > 0){ // if the angle is between 0 and pi/2 (90) then it will be in our quadrant III
            angle = 180 - angle;
        }else{ // if the angle is between 0 and -pi/2 (-90) it will be in our quadrant II
            angle = Math.abs(angle);
        }

        if(angle >= 0 && angle <= theta1) // Quadrant II
            return true;
        else if(angle >= theta2 && angle <= 180) // Quadrant III
            return true;
        else
            return false;
    }

    /**
     * This method handles converting the angle from the gradient calculation which
     * is of the range -pi/2 to pi/2 into the coordinate system of the hough space
     * which is of the range 0 to 180 degrees.  The gradient angle is perpendicular
     * to the gradient of the point, so a angleToConvert of pi/2 would be a horizontal
     * gradient, so the line this point would belong to would be vertical.
     * @param angleToConvert
     * @return returns angle in 0-180 hough space
     */
    private static int convertGradiantToHoughAngle(double angleToConvert){
        angleToConvert = Math.toDegrees(angleToConvert); // convert radian angle to degrees
        if(angleToConvert > 0){ // if the angleToConvert is between 0 and pi/2 (90) then it will be in our quadrant II
            angleToConvert = 180 - angleToConvert;
        }else{ // if the angleToConvert is between 0 and -pi/2 (-90) it will be in our quadrant III
            angleToConvert = Math.abs(angleToConvert);
        }

        return (int)angleToConvert;
    }

    /**
     * computes HoughTransform where the polar space is partitioned for r into rAxisSize bins and for theta into thetaAxisSize bins
     *   for the binary inputData where 255 are the pixels of interest.  Note, does this in limited theta ranges to in essence find only
     *    "close" to vertical ranges of the Hough Space --- this in particular will calculate for the 0 to theta1 (which translates from Vertical to theta1
     *      angled lines in cartesian coordinates)  and AGAIN for the range of theta2 to 180degrees (which translates from -theta1 angled lines to Vertical in
     *      cartesian coordinates)
     *      IF a local point (contour) dow NOT have a local gradient measurement (nxn area) in our desired range  we will have a low vote contribution
     *      BUT if local gradientIS IN our desired angle range it has a HIGH weighting vote in hough space
     *      what we are saying in essence is that the local gradient is an "INDICATION" that the line going through
     *      that point is oriented by its gradient value.
     *  @param inputData - The contour edge input image
     *  @param outputData - The grid in rho, theta space representing the hough space as a 1d array
     *  @param thetaAxisSize - number of theta buckets
     *  @param rAxisSize - number of rho buckets
     *  @param theta1 - will search from 0 degrees up to theta1 (max range 0 to 90)- Quadrant 2 (SE) NOTE: theta1 = 0   is vertical line and 90 is horizontal line
     *  @param theta2 - ALSO will search from theta2 to 180 (max range 90 to 180)- Quadrant 3 NOTE:  theta2=90 degrees is horizontal line and 180 is vertical line
     *                NOTE: to cut OUT horizontal lines (+/-10 degrees from horizontal) specify theta1 = 80  and theta2 = 110
     *                NOTE: to do +/- from vertical lines theta1 =45 and theta2 = 135
     *
     *  @param ignoreEdges - set to true to not process the boundary pixels in the image
     *  @param origImage - original color image used to calculate the gradient
     *  @param localGradientNeighborhoodSize - neighborhood size for calculating the gradient
     *
     */
    public static void houghTransformVerticalLinesMagnifyThetaWithColorChecking(Mat inputData, ArrayData outputData, int thetaAxisSize, int rAxisSize, float theta1, float theta2, boolean ignoreEdges, Mat origImage, int localGradientNeighborhoodSize){
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

        if(theta1 >= 90.0 || theta1 < 0  || theta2<90 || theta2>180 || theta2<theta1)
        {theta1 = 90;  theta2 = 90; } //saftey check for bad parameter specification

        int theta1Bin = Math.round((float)((theta1 / 180) * (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        int theta2Bin = Math.round((float)((theta2 / 180) * (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        if(theta1Bin == theta2Bin)
            theta1Bin = Math.max(0, theta1Bin - 1); // reduce theta1 by one or set to 0
        if(theta1Bin ==0 && theta2Bin==0) // if both are 0 then ...//this makes no sense
            theta2Bin = thetaAxisSize / 2; // WILL: set theta2 bin to half??

        Log.v("Theta Bin Size","theta1Bin: " + theta1Bin + " theta2Bin: " +theta2Bin);

        // not processing the boundary of the image
        int rows, cols, startrow, startcol;
        if(ignoreEdges){
            rows = height - 5;
            cols = width - 5;
            startrow = 5;
            startcol = 5;
        }else{
            rows = height - 1;
            cols = width - 1;
            startrow = 0;
            startcol = 0;
        }

        int weight = 1; // set default weight value
        double[] color_gradient_magnitude;
        // visit every pixel in the contour image
        for (int y = rows; y >= startrow; y--)
        {
            for (int x = cols; x >= startcol; x--)
            {
                // If this is a contour pixel
                if (inputData.get(y,x)[0] > 0) //point want to analyize OLD CODE: if (inputData.contrast(x, y, minContrast))
                {
                    // If the gradient for this pixel is within the accepted range then set appropriate weight
                    color_gradient_magnitude = Utils.calculateColorGradientOrientationAndMagnitude(origImage, x, y, localGradientNeighborhoodSize);
                    if(inRange(color_gradient_magnitude[0], theta1, theta2)){
                        if(color_gradient_magnitude[1] > BB_Parameters.colorGradientMagnitudeThreshold)
                            weight = BB_Parameters.magnificationFactorIfGradientInAngleRange;
                        else
                            weight = Math.max(Math.max(1,BB_Parameters.magnificationFactorIfGradientNotInAngleRange), BB_Parameters.magnificationFactorIfGradientMagnitudeWeak);

                        //color processing
                        //gradient_magnitude = |gradient_red| + |gradient_green| + | gradient_blue|
                        // if(inRange(gradient_magnitude=Utils.calculateColorGradient(origImage, x, y, localGradientNeighborhoodSize), theta1, theta2)){
                        //   We are in a sense re-examining the edge at this contour point and seeing if it is a strong edge or not
                        //    if it is weak like a road to gravel  OR road to not_detected_road  this means there was some issues or we
                        //          were on the edge of our "understanding" of what a road is and it is not strongly distinguised from it
                        //           immediate neighbors and we might not want to have it vote so strongly in a road line edge hough space
                        //    if it is strong like a road to  grass OR road to car OR road to yellow/white line then we know that this is
                        //         some kind of truely road edge point.
                        // if the gradient_magnitude > BBParameters.Threshold)
                        //      weight = BB_Parameters.magnificationFactorIfGradientInAngleRange;
                        //  else
                        //     weight = MAX(1, BB_Parameters.magnificationFactorIfGradientNotInAngleRange, BB_Parameters.mangificationFactorIfGradientMagnitudeWeak)

                    }else{
                        weight = BB_Parameters.magnificationFactorIfGradientNotInAngleRange;
                    }

                    for(int theta = 0; theta <= theta1Bin; theta++)
                    {
                        double r = cosTable[theta] * x + sinTable[theta] * y;
                        int rScaled = (int)Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                        outputData.accumulate(theta, rScaled, weight);  // accumulated by weight
                    }
                    for(int theta = theta2Bin; theta <= thetaAxisSize - 1; theta ++){
                        double r = cosTable[theta] * x + sinTable[theta] * y;
                        int rScaled = (int)Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                        outputData.accumulate(theta, rScaled, weight); //acummulate by weight
                    }
                }
            }
        }
    }

    /**
     * computes HoughTransform where the polar space is partintioned for r into rAxisSize bins and for theta into thetaAxisSize bins
     *   for the binary inputData where 255 are the pixels of interest.  Note, does this in limited theta ranges to in essence find only
     *    "close" to vertical ranges of the Hough Space --- this in particular will calculate for the 0 to theta1 (which translates from Vertical to theta1
     *      angled lines in cartesian coordinates)  and AGAIN for the range of theta2 to 180degrees (which translates from -theta1 angled lines to Vertical in
     *      cartesian coordinates)
     *      IF a local point (contour) dow NOT have a local gradient mesasurement (nxn area) in our desired range  we will have a low vote contribution
     *      BUT if local gradientIS IN our desired angle range it has a HIGH weighting vote in hough space
     *      what we are saying in essesence is that the local gradient is an "INDICATION" that the line going through
     *      that point is oriented by its gradient value.
     *  @param inputData - The contour edge input image
     *  @param outputData - The grid in rho, theta space representing the hough space as a 1d array
     *  @param thetaAxisSize - number of theta buckets
     *  @param rAxisSize - number of rho buckets
     *  @param theta1 - will search from 0 degrees up to theta1 (max range 0 to 90)- Quadrant 2 (SE) NOTE: theta1 = 0   is vertical line and 90 is horizontal line
     *  @param theta2 - ALSO will search from theta2 to 180 (max range 90 to 180)- Quadrant 3 NOTE:  theta2=90 degrees is horizontal line and 180 is vertical line
     *                NOTE: to cut OUT horizontal lines (+/-10 degrees from horizontal) specify theta1 = 80  and theta2 = 110
     *                NOTE: to do +/- from vertical lines theta1 =45 and theta2 = 135
     *
     *  @param ignoreEdges - set to true to not process the boundary pixels in the image
     *  @param roadLabelImage - road labeled image for calculating the gradient
     *  @param localGradientNeighborhoodSize - size of neighborhood nxn to use for gradient kernel
     *
     */
    public static void houghTransformVerticalLinesMagnifyThetaWithBinaryRoadImage(Mat inputData, ArrayData outputData, int thetaAxisSize, int rAxisSize, float theta1, float theta2, boolean ignoreEdges, Mat roadLabelImage, int localGradientNeighborhoodSize)
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

        if(theta1 >= 90.0 || theta1 < 0  || theta2<90 || theta2>180 || theta2<theta1)
        {theta1 = 90;  theta2 = 90; } //saftey check for bad parameter specification

        int theta1Bin = Math.round((float)((theta1 / 180) * (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        int theta2Bin = Math.round((float)((theta2 / 180) * (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        if(theta1Bin == theta2Bin)
            theta1Bin = Math.max(0, theta1Bin - 1); // reduce theta1 by one or set to 0
        if(theta1Bin ==0 && theta2Bin==0) // if both are 0 then ...//this makes no sense
            theta2Bin = thetaAxisSize / 2; // WILL: set theta2 bin to half??

        Log.v("Theta Bin Size","theta1Bin: " + theta1Bin + " theta2Bin: " +theta2Bin);

        // not processing the boundary of the image
        int rows, cols, startrow, startcol;
        if(ignoreEdges){
            rows = height - 5;
            cols = width - 5;
            startrow = 5;
            startcol = 5;
        }else{
            rows = height - 1;
            cols = width - 1;
            startrow = 0;
            startcol = 0;
        }

        int weight = 1; // set default weight value
        double[] gradient_orientationAndMagnitude;
         // visit every pixel in the contour image
        for (int y = rows; y >= startrow; y--)
        {
            for (int x = cols; x >= startcol; x--)
            {
                if (inputData.get(y,x)[0] > 0) //point want to analyize OLD CODE: if (inputData.contrast(x, y, minContrast))
                {
                    gradient_orientationAndMagnitude=Utils.calculateGradientOrientationAndMagnitude(roadLabelImage, x, y, localGradientNeighborhoodSize);
                    // If the gradient for this pixel is within the accepted range then set appropriate weight
                    if(inRange(gradient_orientationAndMagnitude[0], theta1, theta2)){
                        if(gradient_orientationAndMagnitude[1] >= BB_Parameters.binaryGradientMagnitudeThreshold) // and the magnitdue is big enough
                            weight = BB_Parameters.magnificationFactorIfGradientInAngleRange;
                        else
                            weight = BB_Parameters.magnificationFactorIfGradientNotInAngleRange;
                    }else{
                        weight = BB_Parameters.magnificationFactorIfGradientNotInAngleRange;
                    }

                    for(int theta = 0; theta <= theta1Bin; theta++)
                    {
                        double r = cosTable[theta] * x + sinTable[theta] * y;
                        int rScaled = (int)Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                        outputData.accumulate(theta, rScaled, weight);  // accumulated by weight
                    }
                    for(int theta = theta2Bin; theta <= thetaAxisSize - 1; theta ++){
                        double r = cosTable[theta] * x + sinTable[theta] * y;
                        int rScaled = (int)Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                        outputData.accumulate(theta, rScaled, weight); //acummulate by weight
                    }
                }
            }
        }
    }

    /**
     * computes HoughTransform where the polar space is partintioned for r into rAxisSize bins and for theta into thetaAxisSize bins
     *   for the binary inputData where 255 are the pixels of interest.  Note, does this in limited theta ranges to in essence find only
     *    "close" to vertical ranges of the Hough Space --- this in particular will calculate for the 0 to theta1 (which translates from Vertical to theta1
     *      angled lines in cartesian coordinates)  and AGAIN for the range of theta2 to 180degrees (which translates from -theta1 angled lines to Vertical in
     *      cartesian coordinates)
     *      IF a local point (contour) dow NOT have a local gradient mesasurement (nxn area) in our desired range  we will have a low vote contribution
     *      BUT if local gradientIS IN our desired angle range it has a HIGH weighting vote in hough space
     *      what we are saying in essesence is that the local gradient is an "INDICATION" that the line going through
     *      that point is oriented by its gradient value.
     *  @param inputData - The contour edge input image
     *  @param outputData - The grid in rho, theta space representing the hough space as a 1d array
     *  @param thetaAxisSize - number of theta buckets
     *  @param rAxisSize - number of rho buckets
     *  @param theta1 - will search from 0 degrees up to theta1 (max range 0 to 90)- Quadrant 2 (SE) NOTE: theta1 = 0   is vertical line and 90 is horizontal line
     *  @param theta2 - ALSO will search from theta2 to 180 (max range 90 to 180)- Quadrant 3 NOTE:  theta2=90 degrees is horizontal line and 180 is vertical line
     *                NOTE: to cut OUT horizontal lines (+/-10 degrees from horizontal) specify theta1 = 80  and theta2 = 110
     *                NOTE: to do +/- from vertical lines theta1 =45 and theta2 = 135
     *
     *  @param ignoreEdges - set to true to not process the boundary pixels in the image
     *  @param roadLabelImage - road labeled image for calculating the gradient
     *  @param localGradientNeighborhoodSize - size of neighborhood nxn to use for gradient kernel
     *
     */
    public static void houghTransformVerticalLinesEliminateThetaWithBinaryRoadImage(Mat inputData, ArrayData outputData, int thetaAxisSize, int rAxisSize, float theta1, float theta2, boolean ignoreEdges, Mat roadLabelImage, int localGradientNeighborhoodSize)
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

        if(theta1 >= 90.0 || theta1 < 0  || theta2<90 || theta2>180 || theta2<theta1)
        {theta1 = 90;  theta2 = 90; } //saftey check for bad parameter specification

        int theta1Bin = Math.round((float)((theta1 / 180) * (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        int theta2Bin = Math.round((float)((theta2 / 180) * (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        if(theta1Bin == theta2Bin)
            theta1Bin = Math.max(0, theta1Bin - 1); // reduce theta1 by one or set to 0
        if(theta1Bin ==0 && theta2Bin==0) // if both are 0 then ...//this makes no sense
            theta2Bin = thetaAxisSize / 2; // WILL: set theta2 bin to half??

        Log.v("Theta Bin Size","theta1Bin: " + theta1Bin + " theta2Bin: " +theta2Bin);

        // not processing the boundary of the image
        int rows, cols, startrow, startcol;
        if(ignoreEdges){
            rows = height - 5;
            cols = width - 5;
            startrow = 5;
            startcol = 5;
        }else{
            rows = height - 1;
            cols = width - 1;
            startrow = 0;
            startcol = 0;
        }

        double[] gradient_orientationAndMagnitude;
        // visit every pixel in the contour image
        for (int y = rows; y >= startrow; y--)
        {
            for (int x = cols; x >= startcol; x--)
            {
                if (inputData.get(y,x)[0] > 0) //point want to analyize OLD CODE: if (inputData.contrast(x, y, minContrast))
                {
                    gradient_orientationAndMagnitude = Utils.calculateGradientOrientationAndMagnitude(roadLabelImage, x, y, localGradientNeighborhoodSize);
                    // If the gradient for this pixel is within the accepted range then set appropriate weight
                    if(inRange(gradient_orientationAndMagnitude[0], theta1, theta2)){
                        if(gradient_orientationAndMagnitude[1] >= BB_Parameters.binaryGradientMagnitudeThreshold) {
                            for (int theta = 0; theta <= theta1Bin; theta++) {
                                double r = cosTable[theta] * x + sinTable[theta] * y;
                                int rScaled = (int) Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                                outputData.accumulate(theta, rScaled, 1);  // accumulated by weight
                            }
                            for (int theta = theta2Bin; theta <= thetaAxisSize - 1; theta++) {
                                double r = cosTable[theta] * x + sinTable[theta] * y;
                                int rScaled = (int) Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                                outputData.accumulate(theta, rScaled, 1); //acummulate by weight
                            }
                        }
                    }


                }
            }
        }
    }

    /**
     * computes HoughTransform where the polar space is partintioned for r into rAxisSize bins and for theta into thetaAxisSize bins
     *   for the binary inputData where 255 are the pixels of interest.  Note, does this in limited theta ranges to in essence find only
     *    "close" to vertical ranges of the Hough Space --- this in particular will calculate for the 0 to theta1 (which translates from Vertical to theta1
     *      angled lines in cartesian coordinates)  and AGAIN for the range of theta2 to 180degrees (which translates from -theta1 angled lines to Vertical in
     *      cartesian coordinates)
     *  A point will only vote for a range of theta surrounding the theta perpendicular to its gradient.  In essence a point will only
     *  vote for the lines which are perpendicular to its gradient.
     *  @param inputData - The contour edge input image
     *  @param outputData - The grid in rho, theta space representing the hough space as a 1d array
     *  @param thetaAxisSize - number of theta buckets
     *  @param rAxisSize - number of rho buckets
     *  @param theta1 - will search from 0 degrees up to theta1 (max range 0 to 90)- Quadrant 2 (SE) NOTE: theta1 = 0   is vertical line and 90 is horizontal line
     *  @param theta2 - ALSO will search from theta2 to 180 (max range 90 to 180)- Quadrant 3 NOTE:  theta2=90 degrees is horizontal line and 180 is vertical line
     *                NOTE: to cut OUT horizontal lines (+/-10 degrees from horizontal) specify theta1 = 80  and theta2 = 110
     *                NOTE: to do +/- from vertical lines theta1 =45 and theta2 = 135
     *
     *  @param ignoreEdges - set to true to not process the boundary pixels in the image
     *  @param roadLabelImage - road labeled image for calculating the gradient
     *  @param localGradientNeighborhoodSize - size of neighborhood nxn to use for gradient kernel
     *
     */
    public static void houghTransformVerticalLinesReduceThetaWithBinaryRoadImage(Mat inputData, ArrayData outputData, int thetaAxisSize, int rAxisSize, float theta1, float theta2, boolean ignoreEdges, Mat roadLabelImage, int localGradientNeighborhoodSize)
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

        if(theta1 >= 90.0 || theta1 < 0  || theta2<90 || theta2>180 || theta2<theta1)
        {theta1 = 90;  theta2 = 90; } //saftey check for bad parameter specification

        int theta1Bin = Math.round((float)((theta1 / 180) * (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        int theta2Bin = Math.round((float)((theta2 / 180) * (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        if(theta1Bin == theta2Bin)
            theta1Bin = Math.max(0, theta1Bin - 1); // reduce theta1 by one or set to 0
        if(theta1Bin ==0 && theta2Bin==0) // if both are 0 then ...//this makes no sense
            theta2Bin = thetaAxisSize / 2; // WILL: set theta2 bin to half??

        Log.v("Theta Bin Size","theta1Bin: " + theta1Bin + " theta2Bin: " +theta2Bin);

        // not processing the boundary of the image
        int rows, cols, startrow, startcol;
        if(ignoreEdges){
            rows = height - 5;
            cols = width - 5;
            startrow = 5;
            startcol = 5;
        }else{
            rows = height - 1;
            cols = width - 1;
            startrow = 0;
            startcol = 0;
        }

        double[] gradient_orientationAndMagnitude;
        // visit every pixel in the contour image
        for (int y = rows; y >= startrow; y--)
        {
            for (int x = cols; x >= startcol; x--)
            {
                if (inputData.get(y,x)[0] > 0) //point want to analyize OLD CODE: if (inputData.contrast(x, y, minContrast))
                {
                    int houghAngle, upperBoundQ3 = 180, lowerBoundQ3 = 180, upperBoundQ2 = 0, lowerBoundQ2 = 0;
                    gradient_orientationAndMagnitude = Utils.calculateGradientOrientationAndMagnitude(roadLabelImage, x, y, localGradientNeighborhoodSize);
                    // If the gradient for this pixel is within the accepted range then set appropriate weight
                    houghAngle = convertGradiantToHoughAngle(gradient_orientationAndMagnitude[0]); // Convert the -pi/2 to pi/2 to our 0-180 angle range
                    if (gradient_orientationAndMagnitude[1] > BB_Parameters.binaryGradientMagnitudeThreshold) {
                        // Set the lower and upper bounds
                        if(houghAngle > 90) {
                            upperBoundQ3 = houghAngle + BB_Parameters.houghReduceDegreeRange;
                            lowerBoundQ3 = houghAngle - BB_Parameters.houghReduceDegreeRange;
                        }else{
                            upperBoundQ2 = houghAngle + BB_Parameters.houghReduceDegreeRange;
                            lowerBoundQ2 = houghAngle - BB_Parameters.houghReduceDegreeRange;
                        }

                        // Check ranges are in bounds, fix if necessary
                        if(upperBoundQ3 > 180){
                            upperBoundQ2 = upperBoundQ3 - 180;
                            lowerBoundQ2 = 0;
                            upperBoundQ3 = 180;
                        }else if(lowerBoundQ3 < 90){
                            upperBoundQ2 = 90;
                            lowerBoundQ2 = lowerBoundQ3;
                            lowerBoundQ3 = 90;
                        }else if(upperBoundQ2 > 90){
                            lowerBoundQ3 = 90;
                            upperBoundQ3 = upperBoundQ2;
                            upperBoundQ2 = 90;
                        }else if(lowerBoundQ2 < 0){
                            upperBoundQ3 = 180;
                            lowerBoundQ3 = 180 + lowerBoundQ2;
                            lowerBoundQ2 = 0;
                        }

                        if(upperBoundQ2 > theta1)
                            upperBoundQ2 = (int)theta1;

                        if(lowerBoundQ3 < theta2)
                            lowerBoundQ3 = (int)theta2;

                        // convert to bins
                        upperBoundQ2 = Math.round(((upperBoundQ2 * 180) / (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
                        lowerBoundQ2 = Math.round(((lowerBoundQ2 * 180) / (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
                        upperBoundQ3 = Math.round(((upperBoundQ3 * 180) / (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
                        lowerBoundQ3 = Math.round(((lowerBoundQ3 * 180) / (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still


                        for (int theta = lowerBoundQ2; theta <= upperBoundQ2 - 1; theta++) {
                            double r = cosTable[theta] * x + sinTable[theta] * y;
                            int rScaled = (int) Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                            outputData.accumulate(theta, rScaled, 1);  // accumulated by weight
                        }
                        for (int theta = lowerBoundQ3; theta <= upperBoundQ3 - 1; theta++) {
                            double r = cosTable[theta] * x + sinTable[theta] * y;
                            int rScaled = (int) Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                            outputData.accumulate(theta, rScaled, 1); //acummulate by weight
                        }
                    }
                }
            }
        }
    }

    /**
     * computes HoughTransform where the polar space is partintioned for r into rAxisSize bins and for theta into thetaAxisSize bins
     *   for the binary inputData where 255 are the pixels of interest.  Note, does this in limited theta ranges to in essence find only
     *    "close" to vertical ranges of the Hough Space --- this in particular will calculate for the 0 to theta1 (which translates from Vertical to theta1
     *      angled lines in cartesian coordinates)  and AGAIN for the range of theta2 to 180degrees (which translates from -theta1 angled lines to Vertical in
     *      cartesian coordinates)
     *      IF a local point (contour) dow NOT have a local gradient mesasurement (nxn area) in our desired range  we will have a low vote contribution
     *      BUT if local gradientIS IN our desired angle range it has a HIGH weighting vote in hough space
     *      what we are saying in essesence is that the local gradient is an "INDICATION" that the line going through
     *      that point is oriented by its gradient value.
     *  @param inputData - The contour edge input image
     *  @param outputData - The grid in rho, theta space representing the hough space as a 1d array
     *  @param thetaAxisSize - number of theta buckets
     *  @param rAxisSize - number of rho buckets
     *  @param theta1 - will search from 0 degrees up to theta1 (max range 0 to 90)- Quadrant 2 (SE) NOTE: theta1 = 0   is vertical line and 90 is horizontal line
     *  @param theta2 - ALSO will search from theta2 to 180 (max range 90 to 180)- Quadrant 3 NOTE:  theta2=90 degrees is horizontal line and 180 is vertical line
     *                NOTE: to cut OUT horizontal lines (+/-10 degrees from horizontal) specify theta1 = 80  and theta2 = 110
     *                NOTE: to do +/- from vertical lines theta1 =45 and theta2 = 135
     *
     *  @param ignoreEdges - set to true to not process the boundary pixels in the image
     *  @param origImage - original color image to be used for calculating the gradient
     *  @param localGradientNeighborhoodSize - neighborhood size for calculating the gradient
     *
     */
    public static void houghTransformVerticalLinesEliminateThetaWithColorChecking(Mat inputData, ArrayData outputData, int thetaAxisSize, int rAxisSize, float theta1, float theta2, boolean ignoreEdges, Mat origImage, int localGradientNeighborhoodSize) {
        int width = inputData.width();
        int height = inputData.height();
        int maxRadius = (int) Math.ceil(Math.hypot(width, height));
        int halfRAxisSize = rAxisSize >>> 1;
        // x output ranges from 0 to pi
        // y output ranges from -maxRadius to maxRadius
        double[] sinTable = new double[thetaAxisSize];
        double[] cosTable = new double[thetaAxisSize];

        // populate the sin and cosine tables for faster lookup
        for (int theta = thetaAxisSize - 1; theta >= 0; theta--) {
            double thetaRadians = theta * Math.PI / thetaAxisSize;
            sinTable[theta] = Math.sin(thetaRadians);
            cosTable[theta] = Math.cos(thetaRadians);
        }

        if (theta1 >= 90.0 || theta1 < 0 || theta2 < 90 || theta2 > 180 || theta2 < theta1) {
            theta1 = 90;
            theta2 = 90;
        } //saftey check for bad parameter specification

        int theta1Bin = Math.round((float) ((theta1 / 180) * (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        int theta2Bin = Math.round((float) ((theta2 / 180) * (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        if (theta1Bin == theta2Bin)
            theta1Bin = Math.max(0, theta1Bin - 1); // reduce theta1 by one or set to 0
        if (theta1Bin == 0 && theta2Bin == 0) // if both are 0 then ...//this makes no sense
            theta2Bin = thetaAxisSize / 2; // WILL: set theta2 bin to half??

        Log.v("Theta Bin Size", "theta1Bin: " + theta1Bin + " theta2Bin: " + theta2Bin);

        // not processing the boundary of the image
        int rows, cols, startrow, startcol;
        if (ignoreEdges) {
            rows = height - 5;
            cols = width - 5;
            startrow = 5;
            startcol = 5;
        } else {
            rows = height - 1;
            cols = width - 1;
            startrow = 0;
            startcol = 0;
        }

        double[] color_gradient_magnitude;
        // visit every pixel in the contour image
        for (int y = rows; y >= startrow; y--) {
            for (int x = cols; x >= startcol; x--) {
                // If this is a contour pixel
                if (inputData.get(y, x)[0] > 0) //point want to analyize OLD CODE: if (inputData.contrast(x, y, minContrast))
                {
                    // If the gradient for this pixel is within the accepted range then set appropriate weight
                    color_gradient_magnitude = Utils.calculateColorGradientOrientationAndMagnitude(origImage, x, y, localGradientNeighborhoodSize);
                    if (inRange(color_gradient_magnitude[0], theta1, theta2)) {
                        if (color_gradient_magnitude[1] > BB_Parameters.colorGradientMagnitudeThreshold) {
                            for (int theta = 0; theta <= theta1Bin; theta++) {
                                double r = cosTable[theta] * x + sinTable[theta] * y;
                                int rScaled = (int) Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                                outputData.accumulate(theta, rScaled, 1);  // accumulated by weight
                            }
                            for (int theta = theta2Bin; theta <= thetaAxisSize - 1; theta++) {
                                double r = cosTable[theta] * x + sinTable[theta] * y;
                                int rScaled = (int) Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                                outputData.accumulate(theta, rScaled, 1); //acummulate by weight
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * computes HoughTransform where the polar space is partintioned for r into rAxisSize bins and for theta into thetaAxisSize bins
     *   for the binary inputData where 255 are the pixels of interest.  Note, does this in limited theta ranges to in essence find only
     *    "close" to vertical ranges of the Hough Space --- this in particular will calculate for the 0 to theta1 (which translates from Vertical to theta1
     *      angled lines in cartesian coordinates)  and AGAIN for the range of theta2 to 180degrees (which translates from -theta1 angled lines to Vertical in
     *      cartesian coordinates)
     *  A point will only vote for a range of theta surrounding the theta perpendicular to its gradient.  In essence a point will only
     *  vote for the lines which are perpendicular to its gradient.
     *  @param inputData - The contour edge input image
     *  @param outputData - The grid in rho, theta space representing the hough space as a 1d array
     *  @param thetaAxisSize - number of theta buckets
     *  @param rAxisSize - number of rho buckets
     *  @param theta1 - will search from 0 degrees up to theta1 (max range 0 to 90)- Quadrant 2 (SE) NOTE: theta1 = 0   is vertical line and 90 is horizontal line
     *  @param theta2 - ALSO will search from theta2 to 180 (max range 90 to 180)- Quadrant 3 NOTE:  theta2=90 degrees is horizontal line and 180 is vertical line
     *                NOTE: to cut OUT horizontal lines (+/-10 degrees from horizontal) specify theta1 = 80  and theta2 = 110
     *                NOTE: to do +/- from vertical lines theta1 =45 and theta2 = 135
     *
     *  @param ignoreEdges - set to true to not process the boundary pixels in the image
     *  @param origImage - original color image to be used to calculate the gradient of contour points
     *  @param localGradientNeighborhoodSize - neighborhood size used in calulating the gradient
     *
     */
    public static void houghTransformVerticalLinesReduceThetaWithColorChecking(Mat inputData, ArrayData outputData, int thetaAxisSize, int rAxisSize, float theta1, float theta2, boolean ignoreEdges, Mat origImage, int localGradientNeighborhoodSize) {
        int width = inputData.width();
        int height = inputData.height();
        int maxRadius = (int) Math.ceil(Math.hypot(width, height));
        int halfRAxisSize = rAxisSize >>> 1;
        // x output ranges from 0 to pi
        // y output ranges from -maxRadius to maxRadius
        double[] sinTable = new double[thetaAxisSize];
        double[] cosTable = new double[thetaAxisSize];

        // populate the sin and cosine tables for faster lookup
        for (int theta = thetaAxisSize - 1; theta >= 0; theta--) {
            double thetaRadians = theta * Math.PI / thetaAxisSize;
            sinTable[theta] = Math.sin(thetaRadians);
            cosTable[theta] = Math.cos(thetaRadians);
        }

        if (theta1 >= 90.0 || theta1 < 0 || theta2 < 90 || theta2 > 180 || theta2 < theta1) {
            theta1 = 90;
            theta2 = 90;
        } //safety check for bad parameter specification

        int theta1Bin = Math.round(((theta1 / 180) * (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        int theta2Bin = Math.round(((theta2 / 180) * (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
        if (theta1Bin == theta2Bin)
            theta1Bin = Math.max(0, theta1Bin - 1); // reduce theta1 by one or set to 0
        if (theta1Bin == 0 && theta2Bin == 0) // if both are 0 then ...//this makes no sense
            theta2Bin = thetaAxisSize / 2; // WILL: set theta2 bin to half??

        Log.v("Theta Bin Size", "theta1Bin: " + theta1Bin + " theta2Bin: " + theta2Bin);

        // not processing the boundary of the image
        int rows, cols, startrow, startcol;
        if (ignoreEdges) {
            rows = height - 5;
            cols = width - 5;
            startrow = 5;
            startcol = 5;
        } else {
            rows = height - 1;
            cols = width - 1;
            startrow = 0;
            startcol = 0;
        }

        double[] color_gradient_magnitude;

        // visit every pixel in the contour image
        for (int y = rows; y >= startrow; y--) {
            for (int x = cols; x >= startcol; x--) {
                // If this is a contour pixel
                if (inputData.get(y, x)[0] > 0) //point want to analyize OLD CODE: if (inputData.contrast(x, y, minContrast))
                {
                    int houghAngle, upperBoundQ3 = 180, lowerBoundQ3 = 180, upperBoundQ2 = 0, lowerBoundQ2 = 0;
                    // If the gradient for this pixel is within the accepted range then set appropriate weight
                    color_gradient_magnitude = Utils.calculateColorGradientOrientationAndMagnitude(origImage, x, y, localGradientNeighborhoodSize);
                    houghAngle = convertGradiantToHoughAngle(color_gradient_magnitude[0]); // Convert the -pi/2 to pi/2 to our 0-180 angle range
                    if (color_gradient_magnitude[1] > BB_Parameters.colorGradientMagnitudeThreshold) {
                        // Set the lower and upper bounds
                       if(houghAngle > 90) {
                           upperBoundQ3 = houghAngle + BB_Parameters.houghReduceDegreeRange;
                           lowerBoundQ3 = houghAngle - BB_Parameters.houghReduceDegreeRange;
                       }else{
                           upperBoundQ2 = houghAngle + BB_Parameters.houghReduceDegreeRange;
                           lowerBoundQ2 = houghAngle - BB_Parameters.houghReduceDegreeRange;
                       }

                        // Check ranges are in bounds, fix if necessary
                        if(upperBoundQ3 > 180){
                            upperBoundQ2 = upperBoundQ3 - 180;
                            lowerBoundQ2 = 0;
                            upperBoundQ3 = 180;
                        }else if(lowerBoundQ3 < 90){
                            upperBoundQ2 = 90;
                            lowerBoundQ2 = lowerBoundQ3;
                            lowerBoundQ3 = 90;
                        }else if(upperBoundQ2 > 90){
                            lowerBoundQ3 = 90;
                            upperBoundQ3 = upperBoundQ2;
                            upperBoundQ2 = 90;
                        }else if(lowerBoundQ2 < 0){
                            upperBoundQ3 = 180;
                            lowerBoundQ3 = 180 + lowerBoundQ2;
                            lowerBoundQ2 = 0;
                        }

                        if(upperBoundQ2 > theta1)
                            upperBoundQ2 = (int)theta1;

                        if(lowerBoundQ3 < theta2)
                            lowerBoundQ3 = (int)theta2;

                        // convert to bins
                        upperBoundQ2 = Math.round(((upperBoundQ2 * 180) / (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
                        lowerBoundQ2 = Math.round(((lowerBoundQ2 * 180) / (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
                        upperBoundQ3 = Math.round(((upperBoundQ3 * 180) / (thetaAxisSize))); // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still
                        lowerBoundQ3 = Math.round(((lowerBoundQ3 * 180) / (thetaAxisSize)));  // round to nearest value of thetaAxisSize divisible by 180 WILL: Need to match a resolution still


                        for (int theta = lowerBoundQ2; theta <= upperBoundQ2 - 1; theta++) {
                            double r = cosTable[theta] * x + sinTable[theta] * y;
                            int rScaled = (int) Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                            outputData.accumulate(theta, rScaled, 1);  // accumulated by weight
                        }
                        for (int theta = lowerBoundQ3; theta <= upperBoundQ3 - 1; theta++) {
                            double r = cosTable[theta] * x + sinTable[theta] * y;
                            int rScaled = (int) Math.round(r * halfRAxisSize / maxRadius) + halfRAxisSize;
                            outputData.accumulate(theta, rScaled, 1); //acummulate by weight
                        }
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

        /**
         * This clears the dataArray[] which represents the hough space
         */
        public void clear(){
            Arrays.fill(this.dataArray, 0);
        }

        public int get(int x, int y)
        {  return dataArray[y * width + x];  }

        public void set(int x, int y, int value)
        {  dataArray[y * width + x] = value;  }

        public void accumulate(int x, int y, int delta)
        {  set(x, y, get(x, y) + delta);  }

        public Mat toImage(){
            double[] imageArray = new double[this.dataArray.length];
            int[] imageIntArray = new int[this.dataArray.length*4];

            for(int i = 0; i  <= this.dataArray.length - 1; i++){
                int j = i*4;
                imageIntArray[j] = 255;
                imageIntArray[j+1] = Math.min(this.dataArray[i] * 100,255);
                imageIntArray[j+2] = Math.min(this.dataArray[i] * 100,255);
                imageIntArray[j+3] = Math.min(this.dataArray[i] * 100,255);
            }

            Scalar imageScalar = new Scalar(imageArray);

            Mat imageToReturn = new Mat(this.width, this.height, CvType.CV_8UC1, imageScalar);

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String filename = "HoughTransformImage.PNG";
            File f = new File(path, filename);

            Bitmap bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(imageIntArray, 0, this.width, 0, 0, this.width, this.height);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, bos);
            byte[] bar = bos.toByteArray();
            try {
                bos.flush();
                bos.close();
                FileOutputStream fos = new FileOutputStream(f);

                fos.write(bar);
                fos.flush();
                fos.close();
            }catch(IOException e){

            }

            //Highgui.imwrite(f.toString() ,imageToReturn);

            return imageToReturn;
        }

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
     * @param arrayData - hough space represented as 1d array of rho and theta
     * @param heightOfOrig - height of original image
     * @param widthOfOrig - width of original image
     * @param numTopLines - number of hough lines we are looking for
     * @param minNumVotes - min number of votes a candidate line must have
     * @param localMaxNeighborhoodSpanParam -
     */
    public static double[][] findLines(ArrayData arrayData, int heightOfOrig, int widthOfOrig, int numTopLines, int minNumVotes, int localMaxNeighborhoodSpanParam)
    {
        double[][] topLineParams = new double[numTopLines][3]; // theta, rho, numvotes

        int halfRaxisSize = arrayData.height >>> 1;
        int maxRadius = (int)Math.ceil(Math.hypot(heightOfOrig, widthOfOrig));

        int max = 0;
        int currentValue = 0;
        for(int width = 0; width < arrayData.width; width++)//theta dimension
        {
            for(int height = 0; height < arrayData.height; height++) // rho dimension
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
                            topLineParams[i][0] = width * 180 / arrayData.width;
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
                            topLineParams[i][0] = width * 180 / arrayData.width;
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

    /**
     * localMaxNeighborhoodSpanParam represents the creation of a neighborhood of width 2 * localMaxNeighborhoodSpanParam + 1 and height of the same
     * that we search for finding a local maximum in the hough accumulator space.  This effectively results in not saving multiple nearby r, theta values
     * that represent the same line and does a kind of smoothing of our hough accumulator space.
     * @param arrayData - hough space represented as 1d array of rho and theta
     * @param heightOfOrig - height of original image
     * @param widthOfOrig - width of original image
     * @param numTopLines - number of hough lines we are looking for
     * @param minNumVotes - min number of votes a candidate line must have
     * @param localMaxNeighborhoodSpanParam -
     */
    public static double[][] findLinesWithSmoothing(ArrayData arrayData, int heightOfOrig, int widthOfOrig, int numTopLines, int minNumVotes, int localMaxNeighborhoodSpanParam)
    {
        double[][] topLineParams = new double[numTopLines][3]; // theta, rho, numvotes

        int halfRaxisSize = arrayData.height >>> 1;
        int maxRadius = (int)Math.ceil(Math.hypot(heightOfOrig, widthOfOrig));

        int max = 0;
        int sum = 0;
        int currentValue = 0;
        for(int width = 0; width < arrayData.width; width++)
        {
            for(int height = 0; height < arrayData.height; height++)
            {

                currentValue = 0;
                // LOOPING THROUGH ALL BUCKETS in a Smoothing neighborhood surrounding bin (width,height)
                for (int r = -1 * localMaxNeighborhoodSpanParam; r <= localMaxNeighborhoodSpanParam; r++)
                    for (int theta = -1 * localMaxNeighborhoodSpanParam; theta <= localMaxNeighborhoodSpanParam; theta++) {
                        if ((height + r >= 0) && (height + r < arrayData.height) && (width + theta >= 0 )&& (width + theta < arrayData.width)) {
                            currentValue += arrayData.get(width + theta, r+ height);

                        }
                    }

                // If the line doesnt have the min number of votes then don't save it
                if(currentValue < minNumVotes)
                    continue;


                // see if a larger voted (smoothed sum) bin exists in surrounding neighborhood
                // if so then don't save it
                // size of neighborhood is
                for(int r_center = -1 * localMaxNeighborhoodSpanParam; r_center <= localMaxNeighborhoodSpanParam; r_center++)
                    for(int theta_center = -1 * localMaxNeighborhoodSpanParam; theta_center <= localMaxNeighborhoodSpanParam; theta_center++) {
                        sum = 0;
                        //calculate sum in smoothing neighborhood around the (widht+theta_center, height+r_center) bin
                        for (int r = -1 * localMaxNeighborhoodSpanParam; r <= localMaxNeighborhoodSpanParam; r++)
                            for (int theta = -1 * localMaxNeighborhoodSpanParam; theta <= localMaxNeighborhoodSpanParam; theta++) {
                                if ((height + r +r_center >= 0) && (height + r +r_center < arrayData.height) && (width + theta + theta_center >= 0) && (width + theta + theta_center < arrayData.width)) {
                                    sum += arrayData.get(width + theta_center + theta, r + r_center + height);

                                }
                            }

                        if (sum > currentValue)
                        { r_center = localMaxNeighborhoodSpanParam + 1;
                            theta_center = localMaxNeighborhoodSpanParam +1; }
                    }


                //At this point we have a Smoothed Vote that is the highest Smoothed Vote in its neighborhood
                if(sum > currentValue) // don't save our current found value if there is a bigger accumulator value in our neighborhood
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
                            topLineParams[i][0] = width * 180 / arrayData.width;
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
                            topLineParams[i][0] = width * 180 / arrayData.width;
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

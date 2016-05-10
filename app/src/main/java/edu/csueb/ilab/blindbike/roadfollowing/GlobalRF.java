package edu.csueb.ilab.blindbike.roadfollowing;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import org.opencv.core.Core;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;

import com.google.gson.*;

import edu.csueb.ilab.blindbike.blindbike.BB_Parameters;
import edu.csueb.ilab.blindbike.blindbike.R;

/**
 * Created by Lynne on 5/22/2015.   new111
 *
 *
 */
public class GlobalRF {

    /*
        --------------------
        OPERATING CONDITIONS
        --------------------
    */

    /*
        Lighting Condition
        0 = Full Daylight, 1 = Other(fog, rain, cloudy, ...)
     */
    private int light_condition;

    /*
        Road Type
        Number of lanes (2 lane, 4 lane, ...)
     */
    private int road_type;

    /*
        Road Material
        0 = dark asphalt, 1 = ?
     */
    private int road_material;

    /*
        Line Type
        0 = No Lines, 1 = Single Solid White, 2 = Single Dashed White, 3 = Solid Yellow
        4 = Dashed Yellow, 21 = Double Solid White, 22 = Double Dashed White
        23 = Double Solid Yellow, 24 = Double Dashed Yellow
     */
    private int line_type;

    /*
        Width of Road in meters
     */
    private int road_width;

    /*
        Width of Lane in meters
     */
    private int lane_width;

    /**
     * height cutoff for the region of interest, set by constructor
     */
    private int heightCutoff;

    /*
        Bike Tilt Angle
     */
    private int bike_tilt;

    /*
        ---------------------------
        END OF OPERATING CONDITIONS
        ---------------------------
     */

    Hough_Lines.ArrayData outputData;
    double[][] linesData;

    Point topLinePt1; // stores the most recent top line point 1
    Point topLinePt2; // stores the most recent op line point 2

    Mat roadBinaryImage;
    Mat hierarchy;
    Rect boundingRect;

    List<MatOfPoint> contours; // This will store the contours

    java.util.Random pseudoColorRandom;

    Vector<GMM> gmms;

    Vector<ClassedMultipleFixedRangeModel> fixedRangeModels;

    int labeledImage[][];

    /**
     * Constructor
     */
    public GlobalRF(Context context){

        gmms = new Vector();
        fixedRangeModels = new Vector<ClassedMultipleFixedRangeModel>();
        outputData = new Hough_Lines.ArrayData(BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution);
        pseudoColorRandom = new java.util.Random((new java.util.Date()).getTime());

        //Initialize roadBinaryImage
        roadBinaryImage = new Mat(BB_Parameters.runningResolution_height - (int)(BB_Parameters.cutOff_Area_Of_Interest * BB_Parameters.scaleFactor_height), BB_Parameters.runningResolution_width, CvType.CV_8UC1);
        hierarchy = new Mat();
        boundingRect = new Rect(0,0,roadBinaryImage.width(), roadBinaryImage.height());

        heightCutoff = (int)(BB_Parameters.cutOff_Area_Of_Interest * BB_Parameters.scaleFactor_height); // adjusting for scale

        labeledImage = new int[roadBinaryImage.rows()][roadBinaryImage.cols()]; // initialize the labeledImage

        contours = new ArrayList<MatOfPoint>(); // initialize the contours

        if(BB_Parameters.classifyByMultipleClasses == true)
            gmms = Filter_Utility.readParametersForMixtureOfGaussiansForAllDataClasses("gmmFileForAllDataClasses.dat", context, BB_Parameters.std_dev_range_factor);
        else
            gmms = Filter_Utility.readParametersForMixtureOfGaussiansForAllDataClasses("gmmFileForAllDataClasses.dat", context, BB_Parameters.std_dev_range_factor_small);

        if(BB_Parameters.classifyByGMM_NOT_FixedRange == false){
            Gson gson = new Gson();

            // now read in sky model
            String testJsonString = context.getResources().getString(R.string.sky_class);
            fixedRangeModels.add(gson.fromJson(testJsonString, ClassedMultipleFixedRangeModel.class));
            // now read in road model
            testJsonString = context.getResources().getString(R.string.road_class);
            fixedRangeModels.add(gson.fromJson(testJsonString, ClassedMultipleFixedRangeModel.class));
            // now read in white line model
            testJsonString = context.getResources().getString(R.string.whiteline_class);
            fixedRangeModels.add(gson.fromJson(testJsonString, ClassedMultipleFixedRangeModel.class));
        }
    }

    void Erosion( Mat toErode, int erosion_size)
    {
        // The other options were MORPH_CROSS or MORPH_ELLIPSE
        int erosion_type = Imgproc.MORPH_RECT;

        Mat element = Imgproc.getStructuringElement(erosion_type,
                new Size(2 * erosion_size + 1, 2 * erosion_size + 1),
                new Point(erosion_size, erosion_size));

        /// Apply the erosion operation
        Imgproc.erode(toErode, toErode, element);
    }

    /**
     * Method to dialate the input (toDialate) image
     * @param toDilate  input and alters to new dialated image
     * @param dilation_size
     */
    void Dilation( Mat toDilate, int dilation_size)
    {
        // other opetions were MORPH_CROSS or MORPH_ELLIPSE
        int dilation_type = Imgproc.MORPH_RECT;

        Mat element = Imgproc.getStructuringElement( dilation_type,
                new Size( 2*dilation_size + 1, 2*dilation_size+1 ),
                new Point( dilation_size, dilation_size ) );
        /// Apply the dilation operation
        Imgproc.dilate(toDilate, toDilate, element);
    }

    /**
     * This method accepts as input an rgba image frame, desired bearing, and measured bearing
     * and finds a road edge.
     * First each pixel in imgFrame inside the region of interest (set in BB_Parameters)
     * is classified into classes(road, sky, lane line, unknown).  During this process a
     * pseudo color image is created and the calculation/display of this can be toggled
     * on/off in BB_Parameters.java using the pseudo_Calculation parameter.
     *
     * @param imgFrame
     * @param desiredBearing
     * @param measuredBearing
     * @return
     */
    public String processFrame(Mat imgFrame, double desiredBearing, double measuredBearing) {
        boolean road_edge_found = false; // will be true if road edge found in this frame
        int desired_measured_bearing_difference = 0; // set the default bearing difference to 0

        // Initialize hough space to 0's
        outputData.clear();

        // if the bearings are available calculate the difference
        if(desiredBearing >= 0 && measuredBearing >= 0) {
            // The difference between the desired and measured bearing
            // angle measured from measuredBearing to desiredBearing
            // so if desiredBearning is to the left of measuredBearing will be negative
            // otherwise positived
            desired_measured_bearing_difference = angle_difference(measuredBearing, desiredBearing);

            // If the bearing difference is above the threshold then tell user to perform orientation change
            // Do not process frame
            if(Math.abs(desired_measured_bearing_difference) >= BB_Parameters.bearing_offset_orientation_change_cutoff) {
                if(desired_measured_bearing_difference < 0)
                    return "ORIENTATION OFF, STOP AND MOVE HANDLEBARS SLOWLY TO RIGHT";
                else
                    return "ORIENTATION OFF, STOP AND MOVE HANDLEBARS SLOWLY TO LEFT";
            }
        }
        // if the image is empty then don't process anything or if stage to display is original image
        if(imgFrame.empty())
            return "";

        // PHASE 3: Area of Interest
        // regionOfInterest is a submat of imgFrame, any changes to regionOfInterest will change imgFrame
        Mat regionOfInterest = imgFrame.submat(this.heightCutoff, imgFrame.height(), 0, imgFrame.width());
        Mat originalRegionOfInterestClone = regionOfInterest.clone();

        // PHASE 4: Homographic Transform (TBD)


        // PHASE 5: Classification
        // Create and display pseudo colored image of all classes if stage to display is set to 1
        if(BB_Parameters.classifyByMultipleClasses == true) {
            if(BB_Parameters.classifyByGMM_NOT_FixedRange == true)
            {
                Filter_Utility.classifyImageIntoDataClasses(gmms, regionOfInterest, this.roadBinaryImage, labeledImage); // WILL: SLOW
                if(BB_Parameters.adaptive_outlier_elimination)
                   labeledImage = Filter_Utility.adaptiveOutlierElimination(BB_Parameters.road_class_index, labeledImage, gmms,regionOfInterest,this.roadBinaryImage);
            }
            else {
                Filter_Utility.classifyImageIntoDataClasses(fixedRangeModels, regionOfInterest, this.roadBinaryImage, labeledImage, 1);
                if(BB_Parameters.adaptive_outlier_elimination)
                    labeledImage = Filter_Utility.adaptiveOutlierElimination(BB_Parameters.road_class_index, labeledImage, fixedRangeModels,regionOfInterest,this.roadBinaryImage,1);
            }
        }
        // ELSE Create the ONLY by road road binary image(this.roadBinaryImage)
        else {
            if(BB_Parameters.classifyByGMM_NOT_FixedRange == true) {
                Filter_Utility.classifyImageIntoDataClass(gmms.elementAt(BB_Parameters.road_class_index), regionOfInterest, this.roadBinaryImage, BB_Parameters.road_class_index, labeledImage);
                if(BB_Parameters.adaptive_outlier_elimination)
                    labeledImage = Filter_Utility.adaptiveOutlierElimination(BB_Parameters.road_class_index, labeledImage, gmms,regionOfInterest,this.roadBinaryImage);
            }else {
                Filter_Utility.classifyImageIntoDataClass(fixedRangeModels.elementAt(BB_Parameters.road_class_index), regionOfInterest, this.roadBinaryImage, labeledImage); // WILL: NOT IMPLEMENTED
                if(BB_Parameters.adaptive_outlier_elimination)
                    labeledImage = Filter_Utility.adaptiveOutlierElimination(BB_Parameters.road_class_index, labeledImage, fixedRangeModels,regionOfInterest,this.roadBinaryImage, 1);
            }
        }

        // If pseudo creation parameter is true then create pseudo image
        if(BB_Parameters.displaypseudoLabelImage)
            if(BB_Parameters.classifyByGMM_NOT_FixedRange){
                Filter_Utility.createPseudoImage(labeledImage, gmms, regionOfInterest);
            }else {
                Filter_Utility.createPseudoImage(labeledImage, fixedRangeModels, regionOfInterest, 1);
            }

        // If display road binary image set then display the roadBinaryImage
        if (BB_Parameters.displayRoadBinaryImage == true) {
            if(BB_Parameters.classifyByGMM_NOT_FixedRange)
                Filter_Utility.displayBooleanImage(this.roadBinaryImage, regionOfInterest, gmms.elementAt(BB_Parameters.road_class_index).pseudocolor, BB_Parameters.otherClassPseudocolor);
            else
                Filter_Utility.displayBooleanImage(this.roadBinaryImage, regionOfInterest, fixedRangeModels.elementAt(BB_Parameters.road_class_index).pseudocolor, BB_Parameters.otherClassPseudocolor);
        }

        // PHASE 6: Blob Detection
        this.contours.clear(); // clear all old contours

        // Find Contours Notes: takes binary image as input, stores each contour as a MatOfPoint (all contours in contours list),
        // hierarchy contains information about the topology of the image(probably we don't need), mode is the contour retrieval
        // mode, and method is the contour approximation method.  These last two values are set using openCV values.
        if(BB_Parameters.perform_Erosion_Dilation){
            // Going to dilate and erode to try to close gaps CHECK PERFORMANCE
            this.Dilation(this.roadBinaryImage, BB_Parameters.dilation_value);
            this.Erosion(this.roadBinaryImage, BB_Parameters.erosion_Value);
            if(BB_Parameters.displayRoadDialateErode == true) {
                Filter_Utility.displayBooleanImage(this.roadBinaryImage, regionOfInterest, gmms.elementAt(BB_Parameters.road_class_index).pseudocolor, BB_Parameters.otherClassPseudocolor);
                return "";
            }
        }

        // Find contours in the roadBinaryImage
        Imgproc.findContours(this.roadBinaryImage, this.contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);



        // Phase 7: Identification of Road Blob
        Iterator<MatOfPoint> each = this.contours.iterator(); // this will iterate through each contour
        MatOfPoint topContours[] = new MatOfPoint[2];
        double areaOfTopContours[] = new double[2];
        topContours[0] = null;
        topContours[1] = null;

        while(each.hasNext()){
            MatOfPoint nextContour = each.next(); // get the next blob

            if(BB_Parameters.displayAllContours == true) {
                List<MatOfPoint> nextContourList = new ArrayList<MatOfPoint>();
                nextContourList.add(nextContour);
                Imgproc.drawContours(regionOfInterest, nextContourList, -1, new Scalar(pseudoColorRandom.nextInt(255), pseudoColorRandom.nextInt(255), pseudoColorRandom.nextInt(255)));
            }

            double contourArea = Imgproc.contourArea(nextContour); // Can possibly ignore and look for small "blob" by looking at contour length instead

            // 7.1: Small blob elimination
            if(contourArea > BB_Parameters.minNumberBlobPixels) {
                //Log.i("ContourArea:", Double.toString(contourArea));
                //Log.i("ContourHeight", Double.toString(nextContour.size().height));
                //Log.i("ContourWidth", Double.toString(nextContour.size().width));

                // Determine if connected to the front wheel of the bike
                // Will be connected to the front wheel of the bike if
                // contains pixels in bottom center of the frame
                // IDEA: Have polygon corresponding to the tire of the bike/
                // bottom center of image
                // and intersect this polygon with the contour, if the intersection
                // is not empty then this blob touches the tire (not sure how computationally
                // intensive this intersection would be

                // Step 7.3: take top 2 blobs based on size and being lower in the image and
                // to the right and pass them to phase 8 may want to use dilation to merge blobs
                // before selection the question is performance
                // Solution = largest 2 blobs in the lower right blob search area specified by BB_Parameters.startingRow,startingColumn
                // and the lower right pixel location of the entire image
                if(isInBlobSearchArea(nextContour)){
                    if(topContours[0] == null){
                        topContours[0] = nextContour;
                        areaOfTopContours[0] = contourArea;
                    }else if(topContours[1] == null && contourArea < areaOfTopContours[0]){
                        topContours[1] = nextContour;
                        areaOfTopContours[1] = contourArea;
                    }else if(topContours[1] == null)
                    {
                        topContours[1] = topContours[0];
                        areaOfTopContours[1] = areaOfTopContours[0];
                        topContours[0] = nextContour;
                        areaOfTopContours[0] = contourArea;
                    }
                    else if(contourArea > areaOfTopContours[0]){
                        topContours[1] = topContours[0];
                        areaOfTopContours[1] = areaOfTopContours[0];
                        topContours[0] = nextContour;
                        areaOfTopContours[0] = contourArea;
                    }else if(contourArea > areaOfTopContours[1]){
                        topContours[1] = nextContour;
                        areaOfTopContours[1] = contourArea;
                    }
                }

                // What if we aren't on the road?  Use largest road blob??
            }else{
                //each.remove(); // If not min number of pixels then remove
            }
        }


        // Draw the top 2 contours
        if(topContours[0] != null && topContours[1] == null && BB_Parameters.displayTopTwoContours ==true)
            Imgproc.drawContours(regionOfInterest, Arrays.asList(topContours[0]), -1, new Scalar(255,0,0));
        else if (topContours[0] != null && topContours[1] != null && BB_Parameters.displayTopTwoContours == true)
            Imgproc.drawContours(regionOfInterest, Arrays.asList(topContours), -1, new Scalar(255,0,0));
        if (topContours[0] != null && BB_Parameters.displayTopTwoContours == true)
            Imgproc.drawContours(regionOfInterest, Arrays.asList(topContours[0]), -1, new Scalar(255,0,255));

        // Step 8: processing the topContours[] which contains zero, one or two contour elements
        // Perform Hough Transform on contours to find vertical lines
        // Step 8.1: Make binary image from contours
        // Make new mat same size as original image and
        // Initalize all pixels in binaryContourImage to 0,0,0
        Mat binaryContourImage = new Mat(regionOfInterest.size(), CvType.CV_8UC1, new Scalar(0));
        // Draw contour onto binaryContourImage fill with 255
        if (topContours[0] != null && topContours[1] == null)
            Imgproc.drawContours(binaryContourImage, Arrays.asList(topContours[0]), -1, new Scalar(255), 1);
        else if (topContours[0] != null && topContours[1] != null)
            Imgproc.drawContours(binaryContourImage, Arrays.asList(topContours), -1, new Scalar(255), 1);

        if (BB_Parameters.displayBinaryContourImage)
            Filter_Utility.displayBooleanImage(binaryContourImage, regionOfInterest, new double[]{255,255,255,255}, new double[]{0,0,0,255});


        // Run Hough Transform on binaryContourImage
        //Imgproc.HoughLines(binaryContourImage, lines, BB_Parameters.houghRhoResolution_PercentRunningResolution_AccumulatorSpace, BB_Parameters.houghThetaResolution_AccumulatorSpace, BB_Parameters.houghMinNumVotes);
        //Imgproc.HoughLinesP(binaryContourImage, lines, BB_Parameters.houghRhoResolution_PercentRunningResolution_AccumulatorSpace, BB_Parameters.houghThetaResolution_AccumulatorSpace, BB_Parameters.houghMinNumVotes);

        // If a desired bearing is not set then use the default parameters for hough lines for angle selection
        if(desiredBearing < 0 || BB_Parameters.houghAdjustAnglesBasedOnBearing == false) {
            // Choose the correct hough space calculation method
            if(BB_Parameters.houghMethodNumber == 0)
                Hough_Lines.houghTransformVerticalLines(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow, BB_Parameters.lineSelectionAngleRangeHigh, BB_Parameters.ignoreEdgesInHoughTransform);
            else if(BB_Parameters.houghMethodNumber == 1)
                Hough_Lines.houghTransformVerticalLinesMagnifyThetaWithColorChecking(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow, BB_Parameters.lineSelectionAngleRangeHigh, BB_Parameters.ignoreEdgesInHoughTransform, originalRegionOfInterestClone, BB_Parameters.houghAngleRangeNeighborhoodSize);
            else if(BB_Parameters.houghMethodNumber == 2)
                Hough_Lines.houghTransformVerticalLinesEliminateThetaWithColorChecking(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow, BB_Parameters.lineSelectionAngleRangeHigh, BB_Parameters.ignoreEdgesInHoughTransform, originalRegionOfInterestClone, BB_Parameters.houghAngleRangeNeighborhoodSize);
            else if(BB_Parameters.houghMethodNumber == 3)
                Hough_Lines.houghTransformVerticalLinesMagnifyThetaWithBinaryRoadImage(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow, BB_Parameters.lineSelectionAngleRangeHigh, BB_Parameters.ignoreEdgesInHoughTransform, this.roadBinaryImage, BB_Parameters.houghAngleRangeNeighborhoodSize);
            else if(BB_Parameters.houghMethodNumber == 4)
                Hough_Lines.houghTransformVerticalLinesEliminateThetaWithBinaryRoadImage(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow, BB_Parameters.lineSelectionAngleRangeHigh, BB_Parameters.ignoreEdgesInHoughTransform, this.roadBinaryImage, BB_Parameters.houghAngleRangeNeighborhoodSize);
            else if(BB_Parameters.houghMethodNumber == 5)
                Hough_Lines.houghTransformVerticalLinesReduceThetaWithColorChecking(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow, BB_Parameters.lineSelectionAngleRangeHigh, BB_Parameters.ignoreEdgesInHoughTransform, originalRegionOfInterestClone, BB_Parameters.houghAngleRangeNeighborhoodSize);
            else if(BB_Parameters.houghMethodNumber == 6)
                Hough_Lines.houghTransformVerticalLinesReduceThetaWithBinaryRoadImage(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow, BB_Parameters.lineSelectionAngleRangeHigh, BB_Parameters.ignoreEdgesInHoughTransform, this.roadBinaryImage, BB_Parameters.houghAngleRangeNeighborhoodSize);

            if(BB_Parameters.writeHoughToFile)
                outputData.toImage();
        // otherwise use the desired bearing and the current bearing to find lines in the expected range
        }else{
            // if the angle difference is less than 45 degrees then adjust the hough detector angle window to accomodate
            // if the angle difference is greater than 45 degrees it should have already been caught by orientation adjustment
            if(Math.abs(desired_measured_bearing_difference) <= 45) {
                // Choose the correct hough space calculation method
                if(BB_Parameters.houghMethodNumber == 0)
                    Hough_Lines.houghTransformVerticalLines(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow + desired_measured_bearing_difference, BB_Parameters.lineSelectionAngleRangeHigh + desired_measured_bearing_difference, BB_Parameters.ignoreEdgesInHoughTransform);
                if(BB_Parameters.houghMethodNumber == 1)
                    Hough_Lines.houghTransformVerticalLinesMagnifyThetaWithColorChecking(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow + BB_Parameters.lineSelectionAngleRangeLow + BB_Parameters.lineSelectionAngleRangeLow + desired_measured_bearing_difference, BB_Parameters.lineSelectionAngleRangeHigh + desired_measured_bearing_difference, BB_Parameters.ignoreEdgesInHoughTransform, originalRegionOfInterestClone, BB_Parameters.houghAngleRangeNeighborhoodSize);
                else if(BB_Parameters.houghMethodNumber == 2)
                    Hough_Lines.houghTransformVerticalLinesEliminateThetaWithColorChecking(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow+ BB_Parameters.lineSelectionAngleRangeLow + BB_Parameters.lineSelectionAngleRangeLow + desired_measured_bearing_difference, BB_Parameters.lineSelectionAngleRangeHigh + desired_measured_bearing_difference, BB_Parameters.ignoreEdgesInHoughTransform, originalRegionOfInterestClone, BB_Parameters.houghAngleRangeNeighborhoodSize);
                else if(BB_Parameters.houghMethodNumber == 3)
                    Hough_Lines.houghTransformVerticalLinesMagnifyThetaWithBinaryRoadImage(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow+ BB_Parameters.lineSelectionAngleRangeLow + BB_Parameters.lineSelectionAngleRangeLow + desired_measured_bearing_difference, BB_Parameters.lineSelectionAngleRangeHigh + desired_measured_bearing_difference, BB_Parameters.ignoreEdgesInHoughTransform, this.roadBinaryImage, BB_Parameters.houghAngleRangeNeighborhoodSize);
                else if(BB_Parameters.houghMethodNumber == 4)
                    Hough_Lines.houghTransformVerticalLinesEliminateThetaWithBinaryRoadImage(binaryContourImage, outputData, BB_Parameters.houghThetaResolution, BB_Parameters.houghRhoResolution, BB_Parameters.lineSelectionAngleRangeLow+ BB_Parameters.lineSelectionAngleRangeLow + BB_Parameters.lineSelectionAngleRangeLow + desired_measured_bearing_difference, BB_Parameters.lineSelectionAngleRangeHigh + desired_measured_bearing_difference, BB_Parameters.ignoreEdgesInHoughTransform, this.roadBinaryImage, BB_Parameters.houghAngleRangeNeighborhoodSize);

                if (BB_Parameters.writeHoughToFile)
                    outputData.toImage();
            }
        }
        if(BB_Parameters.houghSmoothing)
            linesData = Hough_Lines.findLinesWithSmoothing(outputData, binaryContourImage.height(), binaryContourImage.width(), BB_Parameters.houghNumTopLines, BB_Parameters.houghMinNumVotes, BB_Parameters.houghNeighborhoodSize);
        else
            linesData = Hough_Lines.findLines(outputData, binaryContourImage.height(), binaryContourImage.width(), BB_Parameters.houghNumTopLines, BB_Parameters.houghMinNumVotes, BB_Parameters.houghNeighborhoodSize);


        // temporary points to use in loop
        Point pt1 = new Point();
        Point pt2 = new Point();
        // reset the top line
        this.topLinePt1 = null;
        this.topLinePt2 = null;

        double intersection_col_at_bottom_row = regionOfInterest.width()/2; // intersection at bottom row
        double intersection_col_at_middle_row_difference = regionOfInterest.width() / 2; // intersection at bottom row

        double topLineNumVotes = 0;

        // Calculate the parameters for the top lines and determine which is the road edge
        // Loop through all the top voted lines
        for (int i = BB_Parameters.houghNumTopLines - 1; i >= 0; i--) {

            double[] currentLine = linesData[i];
            double rho = currentLine[1];
            double theta = currentLine[0];
            int numVotes = (int)currentLine[2];
            if(numVotes > 0) {
                Log.i("Line", "Num:" + Integer.toString(i) + " rho:" + Double.toString(rho) + " theta" + Double.toString(theta));

                // calculate the start and end points of the line (convert from polar to cartesian)
                double a = Math.cos((theta/180)*Math.PI), b = Math.sin((theta/180)*Math.PI); // coner
                double x0 = a * rho, y0 = b * rho;
                pt1.x = Math.round(x0 + 1000 * (-b)); // add by 1000 here to extend past edge of image, clip later
                pt1.y = Math.round(y0 + 1000 * (a)); // add by 1000 here to extend past edge of image, clip later
                pt2.x = Math.round(x0 - 1000 * (-b)); // subtract by 1000 here to extend past edge of image, clip later
                pt2.y = Math.round(y0 - 1000 * (a)); // subtract by 1000 here to extend past edge of image, clip later

                Core.clipLine(this.boundingRect, pt1, pt2); // clip the line to the region of interest

                // ----------------Select "rightmost" line------------------
                // ---------------------------------------------------------
                // if this is the first line
                // and the slope is positive (because of inverted coordinate system, would be negative in traditional cartesian coords)
                double slope_of_curr_line = slope(pt1, pt2); // slope of the current line

                double temp_intersection_col_at_bottom_row = pt2.x + slope_of_curr_line*(regionOfInterest.height() - pt2.y);
                double temp_intersection_col_at_middle_row_difference = Math.abs((regionOfInterest.width()/2) - (pt2.x + slope_of_curr_line*(regionOfInterest.height()/2 - pt2.y)));


                if (BB_Parameters.rightMostLineSelectionOption == 1) {
                    // OPTION 1 - average of endpoints furthest right
                    if(this.topLinePt1 == null && slope_of_curr_line > 0){
                        this.topLinePt1 = pt1.clone();
                        this.topLinePt2 = pt2.clone();
                        topLineNumVotes = currentLine[2];

                        road_edge_found = true; // signal a road edge was found

                    // otherwise if this isn't the first line and it is further right and the slope is positive
                    // (because of inverted coordinate system, would be negative in traditional cartesian coords)
                    }else if(this.topLinePt1 != null && pt1.x + pt2.x >= topLinePt1.x + topLinePt2.x && slope_of_curr_line > 0){
                        // if the current point is further right than the current top line then replace
                        this.topLinePt1 = pt1.clone();
                        this.topLinePt2 = pt2.clone();
                        topLineNumVotes = currentLine[2];
                    }
                }else if(BB_Parameters.rightMostLineSelectionOption == 2){
                    // OPTION 2 - intersect bottom row furthest right
                    // must have intersection greater than half the image width
                    if(temp_intersection_col_at_bottom_row > intersection_col_at_bottom_row){
                        intersection_col_at_bottom_row = temp_intersection_col_at_bottom_row;
                        this.topLinePt1 = pt1.clone();
                        this.topLinePt2 = pt2.clone();
                        topLineNumVotes = currentLine[2];
                        road_edge_found = true;
                    }

                }else if(BB_Parameters.rightMostLineSelectionOption == 3){
                    // Option 3 - intersect middle row closest to middle
                    // must have difference less than half the image width
                    if(temp_intersection_col_at_middle_row_difference < intersection_col_at_middle_row_difference){
                        intersection_col_at_middle_row_difference = temp_intersection_col_at_middle_row_difference;
                        this.topLinePt1 = pt1.clone();
                        this.topLinePt2 = pt2.clone();
                        topLineNumVotes = currentLine[2];
                        road_edge_found = true;
                    }
                }

                // ----------------------------------------------------------
                // --------------End Select "rightmost" line------------------

                // draw the top lines
                if (BB_Parameters.displayHoughLines == true) {
                    if (i == 0) {
                        Core.line(regionOfInterest, pt1, pt2, new Scalar(255, 0, 255)); // pink
                        Core.putText(regionOfInterest, Double.toString(currentLine[2]), new Point(20,40), Core.FONT_HERSHEY_COMPLEX, 0.4, new Scalar(255, 0, 255));
                    }
                    else if (i == 1) {
                        Core.line(regionOfInterest, pt1, pt2, new Scalar(255, 0, 0)); // red
                        Core.putText(regionOfInterest, Double.toString(currentLine[2]), new Point(20,60), Core.FONT_HERSHEY_COMPLEX, 0.4, new Scalar(255, 0, 0));
                    }
                    else if (i == 2) {
                        Core.line(regionOfInterest, pt1, pt2, new Scalar(255, 255, 0)); // yellow
                        Core.putText(regionOfInterest, Double.toString(currentLine[2]), new Point(20,80), Core.FONT_HERSHEY_COMPLEX, 0.4, new Scalar(255, 255, 0));
                    }
                    else if (i == 3) {
                        Core.line(regionOfInterest, pt1, pt2, new Scalar(0, 255, 255)); // teal
                        Core.putText(regionOfInterest, Double.toString(currentLine[2]), new Point(20,100), Core.FONT_HERSHEY_COMPLEX, 0.4, new Scalar(0, 255, 255));
                    }
                    else if (i == 4) {
                        Core.line(regionOfInterest, pt1, pt2, new Scalar(255, 100, 0)); // orange
                        Core.putText(regionOfInterest, Double.toString(currentLine[2]), new Point(20,120), Core.FONT_HERSHEY_COMPLEX, 0.4, new Scalar(255, 100, 0));
                    }
                    else {
                        Core.line(regionOfInterest, pt1, pt2, new Scalar(0, 0, 255)); // blue
                        Core.putText(regionOfInterest, Double.toString(currentLine[2]), new Point(20,140), Core.FONT_HERSHEY_COMPLEX, 0.4, new Scalar(0, 0, 255));
                    }
                }
            }
            //}
        }
        if(road_edge_found) {
            // Draw the road edge
            Log.v("ROAD EDGE", "R.E. FOUND!");
            Core.line(regionOfInterest, this.topLinePt1, this.topLinePt2, new Scalar(0, 255, 0)); // green
            Core.putText(regionOfInterest, Double.toString(topLineNumVotes), new Point(20,20), Core.FONT_HERSHEY_COMPLEX, 0.4, new Scalar(0, 255, 0));


            // Decide if need to go to the left or right
            if (this.topLinePt1.y >= this.topLinePt2.y) { // Check which end point of the line is the bottom point
                // Pt1 is the bottom point
                // Calculate bottom row intersection point
                int bottomRowIntersectionPoint;
                if(this.topLinePt1.y != regionOfInterest.height() - 1) {
                    double slope_of_curr_line = slope(this.topLinePt1, this.topLinePt2); // slope of the current line

                    bottomRowIntersectionPoint = (int) (this.topLinePt2.x + slope_of_curr_line * (regionOfInterest.height() - this.topLinePt2.y));
                }else {
                    bottomRowIntersectionPoint = (int) this.topLinePt1.x;
                }
                // Calculate metersFromRoadEdge for console
                double metersFromRoadEdge = ((BB_Parameters.runningResolution_width / 2) - bottomRowIntersectionPoint) / BB_Parameters.pixelsPerMeter;
                if(metersFromRoadEdge < 0)
                    Log.v("R.E. Distance", Double.toString(metersFromRoadEdge) + " meters to left of road edge");
                else
                    Log.v("R.E. Distance", Double.toString(metersFromRoadEdge) + " meters to right of road edge");
                // If the line is outside of the acceptable range then provide instruction to user
                if (bottomRowIntersectionPoint >= BB_Parameters.leftOfCenterThreshold) {
                    // we are too far left go to the right
                    return "Lane Departure Warning: Merge Right";
                } else if (bottomRowIntersectionPoint < BB_Parameters.rightOfCenterThreshold) {
                    // we are too far right go to the left
                    return "Lane Departure Warning: Merge Left";
                } else {
                    // on good course
                    return "On Course";
                }
            } else {
                // Pt2 is the bottom point
                // Calculate bottom row intersection point
                int bottomRowIntersectionPoint;
                if(this.topLinePt2.y != regionOfInterest.height() - 1) {
                    double slope_of_curr_line = slope(this.topLinePt1, this.topLinePt2); // slope of the current line

                    bottomRowIntersectionPoint = (int) (this.topLinePt2.x + slope_of_curr_line * (regionOfInterest.height() - this.topLinePt2.y));
                }else {
                    bottomRowIntersectionPoint = (int) this.topLinePt1.x;
                }
                double metersFromRoadEdge = ((BB_Parameters.runningResolution_width / 2) - bottomRowIntersectionPoint) / BB_Parameters.pixelsPerMeter;
                if(metersFromRoadEdge < 0)
                    Log.v("R.E. Distance", Double.toString(metersFromRoadEdge) + " meters to left of road edge");
                else
                    Log.v("R.E. Distance", Double.toString(metersFromRoadEdge) + " meters to right of road edge");
                // If the line is outside of the acceptable range then provide instruction to user
                if (bottomRowIntersectionPoint >= BB_Parameters.leftOfCenterThreshold) {
                    // we are too far left go to the right
                    return "Lane Departure Warning: Merge Right";
                } else {
                    if (bottomRowIntersectionPoint < BB_Parameters.rightOfCenterThreshold) {
                        // we are too far right go to the left
                        return "Lane Departure Warning: Merge Left";

                    } else {
                        // on good course
                        return "On Course";
                    }
                }
            }

        }else{
            return "No Road Edge Found";
        }
    }

    /**
     * returns the angle distance between two passed angles
     * returns negative if start_angle is to the right of end_angle
     * returns positive if start_angle is to the left of end_angle
     * assuming we are looking at closest distance between the two angles
     * @param start_angle
     * @param end_angle
     * @return distance
     */
    private int angle_difference(double start_angle, double end_angle){
        double phi = Math.abs(start_angle - end_angle) % 360;       // This is either the distance or 360 - distance
        double distance = phi > 180 ? 360 - phi : phi;

        if((end_angle + distance % 360) == start_angle)
            return (int)-distance;
        else
            return (int)distance;
    }

    /**
     * returns the slope of the line created by two points
     * @param pt1
     * @param pt2
     * @return slope of the line
     */
    private double slope(Point pt1, Point pt2){
        return (pt2.y - pt1.y) / (pt2.x - pt1.x);
    }

    /**
     * This function will take in a MatOfPoint object and return a Mat containing the line parameters
     * Creates line based on points that are within 'distance' in x axis of the furthest right point
     * in 'points'
     *
     * @param points
     * @param distance
     * @return LineParams
     */
    private Mat findLineFromPointsWithinRangeFromFurthestRightPoint(MatOfPoint points, int distance){
        Mat lineParams = new Mat();
        Vector<Point> pointsToFit = new Vector<Point>();

        // Select points within distance of furthest right point
        // Step 1: Find furthest right point and put at 0 location in pointsToFit
        Point[] contourPoints = points.toArray();
        for(int i = 0; i < contourPoints.length; i++) {
            if (pointsToFit.isEmpty())
                pointsToFit.add(contourPoints[i]);
            else if (pointsToFit.get(0).x < contourPoints[i].x)
                pointsToFit.set(0, contourPoints[i]);
        }

        // Step 2: Get all points within distance of x value of furthest right point
        double xVal = pointsToFit.get(0).x;
        for(int i = 0; i < contourPoints.length; i++) {
            if(contourPoints[i].x >= xVal - distance)
                pointsToFit.add(contourPoints[i]);
        }

        // Step 3: Fit line to pointsToFit
        Mat pointsToFitMat = new Mat(pointsToFit.size(), 2, CvType.CV_32F);
        for(int i = 0; i < pointsToFit.size(); i++){
            pointsToFitMat.put(i,0, pointsToFit.get(i).x);
            pointsToFitMat.put(i,1, pointsToFit.get(i).y);
        }
        Log.i("PointsToFit", pointsToFitMat.dump());
        if(!pointsToFitMat.empty()) {
            Imgproc.fitLine(pointsToFitMat, lineParams, Imgproc.CV_DIST_L2, 0, 0.01, 0.01);
            Log.i("FitLine", lineParams.dump());
        }
        return lineParams;
    }

    /**
     * Test if contour is in the blob search area specified by lower right blob search area specified by BB_Parameters.startingRow,startingColumn
     // and the lower right pixel location of the entire image
     * @param contour
     * @return
     */
    public boolean isInBlobSearchArea(MatOfPoint contour){
        // Check to see if any point in the contour is within the blob search area
        Point[] contourPoints = contour.toArray();
        for(int i = 0; i < contourPoints.length; i++){
            if(contourPoints[i].x > BB_Parameters.startingColumn && contourPoints[i].y > BB_Parameters.startingRow)
                return true;
        }
        return false;
    }
}

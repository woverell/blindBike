package edu.csueb.ilab.blindbike.blindbike;
// import android.util.Size;
/**
 * Class to hold any parameters used by this program as static variables
 * Created by Lynne on 9/20/2015.
 */
public class BB_Parameters {

    // This is the file path to the source code where the .dat files are located
    public static String filepath_to_code_source = "file:///android_asset/";

    // This is the number of components of the feature vector (RGB currently)
    public final static int featureVectorDimension = 3;

    // These are the road class id's in the gmmFileForAllDataClasses.dat
    // The road class used for single class GMM and for the road pseudocolor set by
    // the road_class_index parameter below
    public final static int[] road_classes = {1,4};

    // Parameter to signify which is the index of the road class in our set of gmms
    public static final int road_class_index = 1; // please look at file gmmFileForAllDataClasses.dat in assets for appropriate number
    // also see class_models_with_fixed_ranges.xml for appropriate number AND THEY SHOULD BE THE SAME AND BE IN THE SECOND POSITION OF THE
    // CLASS MODELS IN THE DATA FILES SO IT MAPS TO INDEX OF 1 IN THE VECTOR CONTAINING ALL THE CLASS MODELS

    // Distance in meters from shape point to consider having reached it
    public final static int SHAPE_POINT_DISTANCE_THRESHOLD = 10;

    // This sets the psuedo-color for the 'other' class
    public final static double[] otherClassPseudocolor = {0,255,0,255};

    // Verbose mode for Filter_Utility.java
    public static boolean VERBOSE = false;

    // Standard Deviation range for GMM Classification
    public static double std_dev_range_factor = 1.8; // for multiple classes (road,sky,white-line)
    public static double std_dev_range_factor_small = 1.0; // for single class (road/non-road)

    // Training data resolution
    public final static int trainingResolution_width = 720;
    public final static int trainingResolution_height = 588;

    // For performance speed up this is range of resolution desired
    public   final static int idealResolutionLow_width = 300;   //this is the low end of ideal Size.width, Size.height we ant to achieve of pictures
    public  final static int idealResolutionLow_height = 200;
    public final static int idealResolutionHigh_width = 400;   //this is the high end of ideal Size.width, Size.height we want to achieve of pictures
    public final static int idealResolutionHigh_height = 300;
    public static int runningResolution_width;
    public static int runningResolution_height;

    // Parameters to determine width and height scale from original to lower resolution
    // (lower divided by original) therefore multiply by the scale
    public static double scaleFactor_width = 1;
    public static double scaleFactor_height = 1;

    // This is the top row out of a height of this.trainingResolution_height rows you want to start examining in your RoI
    // This value should be between 0 and this.trainingResolution_height
    public static final int cutOff_Area_Of_Interest = 200;

    // The angle difference at which the user must perform an orientation change, no point in processing frame
    public static final double bearing_offset_orientation_change_cutoff = 65;

    // If test_mode true then we process on testImage rather than camera images
    // MUST BE SET TO TRUE FOR BATCH TESTING
    public static final boolean test_mode = false;
    public static final boolean save_to_gallery = true; // set to true to save the output of the test_mode to the phone gallery

    // set to true if want to process on images in assets/imgs_to_process folder
    // processed output will be saved in the photo gallery on device
    // test_mode MUST ALSO BE SET TO TRUE FOR BATCH TESTING
    public static final boolean batch_testing = false;

    // **********************************************************************************************
    // ********************************Image to use in test_mode*************************************
    // Images are put in res/drawable folder in .png format
    public static int testImage = R.drawable.roadtest17;
    //***********************************************************************************************

    // Parameter for Small Blob Elimination Step (Step 7.1)
    // Based on birds eye view geometry which we dont have 10 columns filling aprox 280 pixel high image would be 2800
    // and correcting for perspective we are guessing that might be 2000Math.min(this.dataArray[i] * 100,255)
    public static final int minNumberBlobPixels = 2000;


    // If true then classifies with multiple class GMM (road, sky, white line)
    // If false then classifies with one road class GMM
    public static final boolean classifyByMultipleClasses = true;

    // Set true to classify using RGB
    // Set false to classify using HSV
    public static final boolean classifyByRGBNotHSV = false;

    /**
     * true if classifying with GMM, false if classifying with fixed ranges
      */
    public static final boolean classifyByGMM_NOT_FixedRange = true;

    /**
     * if true we are invocing the adaptive outlier elimination module that
     * re-classifies outlier road pixels based on the threshold of
     * adaptive_std_dev_range_factor*stddev of road pixels in THAT image
     * from the mean of the road pixels in THAT image
     */
    public static final boolean adaptive_outlier_elimination = true;

    /**
     * standard deviation
     */
    public static final float adaptive_std_dev_range_factor = 2.0f;

    // This is to create for display pseudo color labeled image
    // Either multiple classes or just road or non-road
    public static final boolean displaypseudoLabelImage = true;

    public static final boolean displayRoadBinaryImage = false;

    public static final boolean displayRoadDialateErode = false;

    public static final boolean displayAllContours = false;

    public static final boolean displayEligibleContours = false;

    public static final boolean displayLocationReducedContours = false;

    public static final boolean displayTopTwoContours = false;

    public static final boolean displayHoughLines = false;

    public static final boolean displayRoadEdge = true;

    public static final boolean displayBinaryContourImage = false;

    public static final boolean writeHoughToFile = false;

    // Parameter for erosion/dilation value of roadBinaryImage
    public static final boolean perform_Erosion_Dilation = false;
    public static final int erosion_Value = 1; // 5x5 window
    public static final int dilation_value = 1; // 5x5 window

    // Parameter for Hough Lines Detection in GlobalRF
    public static final double houghRhoResolution_PercentRunningResolution_AccumulatorSpace = 5.0; // parameter when using openCV's hough transform call
    public static double houghRhoResolution_RunningResolution_AccumulatorSpace;
    //public static final double houghThetaResolution_AccumulatorSpace = Math.PI / 180;
    public static final int houghThetaResolution = 180; // How many buckets for theta in hough space (original value: 180)
    public static final int houghRhoResolution = 320; // How many buckets for Rho in hough space (original value: 320)
    public static final int houghMinNumVotes = 10; // minimum number of votes to consider a line
    public static final int houghMaxLineGap = 1; // Used for OpenCV hough implementation for gap between connected lines
    public static final int houghNumTopLines = 10; // This is for display to see the top x lines
    public static final int houghSelectTopLines = 4; // This is for selecting the right most of the top lines
    public static final int houghNeighborhoodSize = 4; // Neighborhood Size for calculating hough space
    public static final boolean houghSmoothing = false; // Use hough smoothing, see findLinesWithSmoothing() in Hough_Lines.java
    public static final int magnificationFactorIfGradientInAngleRange = 10; // weight given to contour pixels with gradient in our range
    public static final int magnificationFactorIfGradientNotInAngleRange = 1; // weight given to contour pixels with gradient not in our range
    public static final int magnificationFactorIfGradientMagnitudeWeak = 3; // weight given to contour pixels with weak gradient
    public static final int colorGradientMagnitudeThreshold = 5; // minimum magnitude for gradient using color image
    public static final int binaryGradientMagnitudeThreshold = 1; // minimum magnitude for gradient using binary road image
    public static final boolean ignoreEdgesInHoughTransform = true; // ignore the edge pixels when calculating the hough space
    public static final int houghGradientNeighborhoodSize = 3; // Neighborhood for calculating the gradient
    public static final boolean houghAdjustAnglesBasedOnBearing = false; // Based on bearing difference adjust which angles we look for in hough space
    public static final int houghReduceDegreeRange = 7; // Angle in degrees for range around hough space reduction

    // Selection For Hough Space Calculation Method
    // 0 = Original method with no theta or magnitude weighting
    // 1 = COLOR magnification by theta with minimum magnitude
    // 2 = COLOR elimination by theta with minimum magnitude
    // 3 = BINARY magnification by theta with minimum magnitude
    // 4 = BINARY elimination by theta with minimum magnitude
    // 5 = COLOR reduction by theta with minimum magnitude
    // 6 = BINARY reduction by theta with minimum magnitude
    // 7 = NO Hough --Doing Right Lower Contour Perfect Vertical Line (ave column) fitting
    // 8 = NO Hough -- Doing Right Lower Contour Line estimation
    public static final int houghMethodNumber = 5;

    //Parameters for "houghMethod" 7 Right Lower Contour Perfect Vertical Line (ave column) fitting
    public static  int  verticalLineFit_Examine_HorizontalSlice_row_top = 120; //cant be more than bottom
    public static  int verticalLineFit_Examine_HorizontalSlice_row_bottom = 160;  //can't be more then image height


    // Line Selection Algorithm Parameters
    // Angle Range
    // Angle of 0 or 180 is vertical line
    public static final int lineSelectionAngleRangeLow = 50; // decrease this to narrow range (min 1)
    public static final int lineSelectionAngleRangeHigh = 130; // increase this to narrow range (max 179)

    // Algorithm selection for rightmost line selection
    // 1 - pick line with positive slope(pointing to top left) that has endpoints on avg furthest right
    // 2 - pick line that intersects the bottom row of the image furthest to the right
    // 3 - pick line that intersects the middle row of the image closest to the center of the image
    // 4 - pick the highest voted line
    public static final int rightMostLineSelectionOption = 1;


    // Camera downward tilt angle - how far the camera is angled downward on the handlebars
    // This affects which metersOfBottomRowXDegreeTilt to use below
    public final static int cameraTiltAngle = 5;
    // Pixels to Meters Measurements
    public static double pixelsPerMeter;
    // Bottom row corresponds to following number of meters at different camera tilt angles
    // These were calculated measuring the length of ground corresponding to the bottom row
    // of the image on flat ground
    public static final double metersOfBottomRow5DegreeTilt = 3.48;
    public static final double metersOfBottomRow10DegreeTilt = 2.72;
    public static final double metersOfBottomRow20DegreeTilt = 2.13;

    // Merge direction thresholds
    public static int leftOfCenterThreshold; // Set in CustomizeView.java because of different resolutions
    public static final double leftOfCenterOffsetMeters = 0.0; // How many meters off the user can be on the left
    public static int rightOfCenterThreshold; // Set in CustomizeView.java because of different resolutions
    public static final double rightOfCenterOffsetMeters = 1.2; // How many meters off the user can be on the right


    // Parameter for blob search area
    // startingRow = top row of search area
    // startingColumn = leftmost column of search area
    // note: bottom of search rectangle area is bottom right of image
    public static int startingRow;
    public static int startingColumn;
    // percentage of the width of the image(starting from the right) to search for road blob
    // Ex: value of 80 means the right 80% of the image gets searched
    public static int widthPercentToCalculateForRoadArea = 80; }
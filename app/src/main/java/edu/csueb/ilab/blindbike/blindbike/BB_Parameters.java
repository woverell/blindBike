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

    // Distance in meters from shape point to consider having reached it
    public final static int SHAPE_POINT_DISTANCE_THRESHOLD = 10;

    // This sets the psuedo-color for the 'other' class
    public final static double[] otherClassPseudocolor = {0,255,0,255};

    // Verbose mode for Filter_Utility.java
    public static boolean VERBOSE = false;

    // Standard Deviation range for GMM Classification
    public static double std_dev_range_factor = 2.0; // for multiple classes (road,sky,white-line)
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

    // Parameter to signify which is the index of the road class in our set of gmms
    public static final int road_class_index = 1; // please look at file gmmFileForAllDataClasses.dat in assets for appropriate number
    // also see class_models_with_fixed_ranges.xml for appropriate number AND THEY SHOULD BE THE SAME AND BE IN THE SECOND POSITION OF THE
    // CLASS MODELS IN THE DATA FILES SO IT MAPS TO INDEX OF 1 IN THE VECTOR CONTAINING ALL THE CLASS MODELS


    // If test_mode true then we process on testImage rather than camera images
    public static final boolean test_mode = false;

    // Image to use in test_mode
    public static int testImage = R.drawable.si_4;

    // Parameter for Small Blob Elimination Step (Step 7.1)
    // Based on birds eye view geometry which we dont have 10 columns filling aprox 280 pixel high image would be 2800
    // and correcting for perspective we are guessing that might be 2000
    public static final int minNumberBlobPixels = 2000;


    // If true then classifies with multiple class GMM (road, sky, white line)
    // If false then classifies with one road class GMM
    public static final boolean classifyByMultipleClasses = true;

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
    public static final boolean displaypseudoLabelImage = false;

    public static final boolean displayRoadBinaryImage = false;

    public static final boolean displayRoadDialateErode = false;

    public static final boolean displayAllContours = false;

    public static final boolean displayTopTwoContours = false;

    public static final boolean displayHoughLines = false;

    public static final boolean displayBinaryContourImage = false;

    // Parameter for erosion/dilation value of roadBinaryImage
    public static final boolean perform_Erosion_Dilation = false;
    public static final int erosion_Value = 1; // 5x5 window
    public static final int dilation_value = 1; // 5x5 window

    // Parameter for Hough Lines Detection in GlobalRF
    public static final double houghRhoResolution_PercentRunningResolution_AccumulatorSpace = 5.0;
    public static double houghRhoResolution_RunningResolution_AccumulatorSpace;
    //public static final double houghThetaResolution_AccumulatorSpace = Math.PI / 180;
    public static final int houghThetaResolution = 180;
    public static final int houghRhoResolution = 320;
    // Minimum Number of votes to consider line
    public static final int houghMinNumVotes = 20;
    public static final int houghMaxLineGap = 1;
    public static final int houghNumTopLines = 10; // This is for display to see the top x lines
    public static final int houghSelectTopLines = 4; // This is for selecting the right most of the top lines
    public static final int houghNeighborhoodSize = 4;

    public static final boolean ignoreEdgesInHoughTransform = true;

    // Line Selection Algorithm Parameters
    // Angle Range
    // Angle of 0 or 180 is vertical line
    public static final int lineSelectionAngleRangeLow = 45; // decrease this to narrow range (min 1)
    public static final int lineSelectionAngleRangeHigh = 135; // increase this to narrow range (max 179)

    // Merge direction thresholds
    public static int leftOfCenterThreshold;
    public static final int leftOfCenterOffsetValue = 70;
    public static int rightOfCenterThreshold;
    public static final int rightOfCenterOffsetValue = -70;


    // Parameter for blob search area
    // startingRow = top row of search area
    // startingColumn = leftmost column of search area
    // note: bottom of search rectangle area is bottom right of image
    public static int startingRow;
    public static int startingColumn;
}
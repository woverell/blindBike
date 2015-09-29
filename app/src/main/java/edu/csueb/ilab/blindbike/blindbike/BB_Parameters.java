package edu.csueb.ilab.blindbike.blindbike;
// import android.util.Size;
/**
 * Class to hold any parameters used by this program as static variables
 * Created by Lynne on 9/20/2015.
 */
public class BB_Parameters {

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

    public static final int cutOff_Area_Of_Interest = 200;

    public static final boolean pseudo_Creation = false;

}
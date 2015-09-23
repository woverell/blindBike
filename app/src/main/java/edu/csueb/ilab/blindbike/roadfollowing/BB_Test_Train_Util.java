package edu.csueb.ilab.blindbike.roadfollowing;

/**
 * Created by Will on 9/21/2015.
 */
public class BB_Test_Train_Util {
    // This is the file path to the source code where the .dat files are located
    public static String filepath_to_code_source = "C:\\Users\\Will\\Desktop\\Eclipse Android Workspace\\BlindBike Training Filters\\src\\";

    // This is the number of components of the feature vector (RGB currently)
    public final static int featureVectorDimension = 3;

    public final static double[] otherClassPseudocolor = {0,255,0};

    public static boolean VERBOSE = false;

    public static double std_dev_range_factor = 2.0;
}


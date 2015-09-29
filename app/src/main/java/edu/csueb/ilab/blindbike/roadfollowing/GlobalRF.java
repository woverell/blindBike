package edu.csueb.ilab.blindbike.roadfollowing;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.TermCriteria;
import org.opencv.ml.CvKNearest;
import org.opencv.ml.EM;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

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

    /*
        Bike Tilt Angle
     */
    private int bike_tilt;

    /*
        Region of Interest
        Set columns in params_operating.xml
        REGION_OF_INTEREST_COLUMNS value
     */
    private Rect regionOfInterest;

    /*
        ---------------------------
        END OF OPERATING CONDITIONS
        ---------------------------
     */

    /*
        -------------------
        MATRIX DECLARATIONS
        -------------------
     */
        private Mat localFrame;
        private Mat subFrame;
        private Mat sample;
    /*
        --------------------------
        END OF MATRIX DECLARATIONS
        --------------------------
     */

    Vector<GMM> gmms;

    /**
     * Constructor
     */
    public GlobalRF(Context context){
        // Initialize Matrices
        localFrame = new Mat();
        sample = new Mat();

        gmms = new Vector();


        gmms = Filter_Utility.readParametersForMixtureOfGaussiansForAllDataClasses("gmmFileForAllDataClasses.dat", context);
        Log.i("WILLGMM", gmms.elementAt(0).className);

    }

    /**
     * This method accepts as input an rgba image frame and finds a road edge.
     * First each pixel in imgFrame inside the region of interest (set in BB_Parameters)
     * is classified into classes(road, sky, lane line, unknown).  During this process a
     * pseudo color image is created and the calculation/display of this can be toggled
     * on/off in BB_Parameters.java using the pseudo_Calculation parameter.
     *
     * @param imgFrame
     */
    public void processFrame(Mat imgFrame) {

        // PHASE 3: Area of Interest
        int heightCutoff = (int)(BB_Parameters.cutOff_Area_Of_Interest * BB_Parameters.scaleFactor_height); // adjusting for scale
        Mat regionOfInterest = imgFrame.submat(heightCutoff, imgFrame.height(), 0, imgFrame.width());

        // PHASE 4: Homographic Transform (TBD)

        // PHASE 5: Classification
        Filter_Utility.classifyImageIntoDataClasses(gmms, regionOfInterest);

        // PHASE 6: Blob Detection

    }
}

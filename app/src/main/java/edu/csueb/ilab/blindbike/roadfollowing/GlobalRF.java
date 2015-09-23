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
     *  This function takes the original frame size of the image
     *  and sets the proper region of interest for the downsampled
     *  frame based on number of times downsampled and the
     *  region of interest percent.
     * @param height of original frame
     * @param width of original frame
     */
    public void setROI(int height, int width){
        // Adjust height and width based on downsampling
        // TODO: CHECK IF MATCHES UP WITH DOWNSAMPLED IMAGE SIZE
        height = height / (R.integer.DOWNSAMPLE_TIMES * 2);
        width = width / (R.integer.DOWNSAMPLE_TIMES * 2);
        // Set region of interest rectangle, eliminate top REGION_OF_INTEREST_PERCENT percent
        this.regionOfInterest = new Rect(0, (int) ((R.integer.REGION_OF_INTEREST_PERCENT / 100.0) * height), width, height);
    }

    /**
     * This is the primary method of the GlobalRF class.
     * Processes imgFrame to find edge of road.
     * @param imgFrame
     */
    public Mat processFrame(Mat imgFrame) {
        //imgFrame.copyTo(localFrame); // Copy to local so can modify

        // PHASE 3: Set Region of Interest
        //subFrame = localFrame.submat(regionOfInterest);

        // PHASE 4: Homographic Transform (TBD)

        // PHASE 5: Classification (pixels -> classes)

        imgFrame = Filter_Utility.classifyImageIntoDataClasses(gmms, imgFrame);
        return imgFrame;
    }

    /**
     * This function will return an integer corresponding to the class
     * that the pixel belongs to.
     * @return c
     */
    private int classify(){
        int c = 0;


        return c;
    }
}

package edu.csueb.ilab.blindbike.roadfollowing;

import android.util.Log;
import android.widget.Toast;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.ml.CvKNearest;

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

    /*
        K-Nearest Neighbors class
     */
    CvKNearest knn;

    /**
     * Constructor
     */
    public GlobalRF(){
        // Initialize Matrices
        localFrame = new Mat();
        sample = new Mat();

        // Initialize K-NN class with training data
        // TODO: ADD TRAINING DATA AS PARAMETER
        int[][] intArray = new int[][]{{2,3,4},{5,6,7},{8,9,10},{9, 9, 7},{8,7,7}}; // points
        Mat sampleData = new Mat(5,3, CvType.CV_32FC1); // 5 rows; 3 cols
        for(int row=0;row<5;row++){
            for(int col=0;col<3;col++)
                sampleData.put(row, col, intArray[row][col]);
        }

        int[][]labelArray = new int[][]{{1}, {1}, {0}, {1}, {0}}; // labels
        Mat labels = new Mat(5,1, CvType.CV_32FC1);  // 5 rows; 1 col
        for(int row=0;row<5;row++){
            for(int col=0;col<1;col++)
                labels.put(row, col, labelArray[row][col]);
        }
        knn = new CvKNearest(sampleData, labels); // initialize and train knn

        // Test knn
        Mat results = new Mat();
        Mat neighborResponses = new Mat();
        Mat dists = new Mat();
        Mat testSample = new Mat(1,3, CvType.CV_32FC1); // will have one entry: (2,3,4)
        testSample.put(0, 0, 9);
        testSample.put(0, 1, 9);
        testSample.put(0, 2, 6);
        knn.find_nearest(testSample, 3, results, neighborResponses, dists);
        double resultClass = results.get(0,0)[0];
        Log.i("NaviActivity", "class: " + resultClass + "\n");
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
        regionOfInterest = new Rect(0, (int) ((R.integer.REGION_OF_INTEREST_PERCENT / 100.0) * height), width, height);
    }

    /**
     * This is the primary method of the GlobalRF class.
     * Processes imgFrame to find edge of road.
     * @param imgFrame
     */
    public void processFrame(final Mat imgFrame) {
        imgFrame.copyTo(localFrame); // Copy to local so can modify

        // PHASE 3: Set Region of Interest
        //subFrame = localFrame.submat(regionOfInterest);

        // PHASE 4: Homographic Transform (TBD)

        // PHASE 5: Classification (pixels -> classes)
        /*
        for(int i = 0; i < subFrame.height(); i++){
            for(int j = 0; j < subFrame.width(); j++){
                // Set up features for this pixel
                // Get response
                //knn.find_nearest();
            }
        }
        */
    }
}

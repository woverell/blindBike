package edu.csueb.ilab.blindbike.blindbike;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.io.IOException;
import java.util.List;

/**
 * Created by Admin on 5/14/2015.
 */
public class CustomizeView extends JavaCameraView{

    private static final String TAG = "Sample::Tutorial3View";
    private String mPictureFileName;

    public CustomizeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //     connectCamera(getWidth(), getHeight());  //LYNNE: CODE FAILING HERE --- getWidth on a null object will always be 0 so it fails inside this function
    }

    public List<Camera.Size> getResolutionList() {
        if (mCamera == null)
        { Log.i("CustomizeView", "  in getResoltuionList() mCamera is null"); //return empty list
            return null; }

        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public void setClosestResolution( int low_width, int low_height, int high_width, int high_height  )
    {
        this.setClosestResolution(this.getResolutionList(),low_width,low_height,high_width,high_height);
    }

    //method to setup closest Resoltuion to range given otherwise if is not supported do not set resolution
    public void setClosestResolution(List<Camera.Size> list, int low_width, int low_height, int high_width, int high_height  )
    {
        int bestWidth=0, bestHeight=0;

        Camera.Size current_size;
        int current_width, current_height;

        if (list.isEmpty())
        { Log.i("CustomizeView: ", "no supported camera resolutions found");
            return; } //do nothing but log, do not change resolution

        //cylce thorough list of Sizes supported by Camera and find the largest value that is in the range of low and high values

        for(int i=0; i<list.size(); i++)
        {
            //get next item in list
            current_width = list.get(i).width;
            current_height = list.get(i).height;
            if (current_width >= low_width && current_width <=high_width && current_height >= low_height && current_height <= high_height) {
                //replace best with these values
                bestWidth = current_width;
                bestHeight = current_height;
            }

        }

        //make sure not 0 --we found something in our range
        if(bestWidth ==0  || bestHeight == 0)
        { Log.i("CustomizeView: ", "no supported camera resolutions range in BB_Parameters" + low_width +"," + low_height +") to (" + high_width + ","+ high_height +")");
            return; }

        // set resolution and scale parameters
        BB_Parameters.runningResolution_height = bestHeight;
        BB_Parameters.runningResolution_width = bestWidth;
        BB_Parameters.scaleFactor_height = (double)bestHeight / (double)BB_Parameters.trainingResolution_height;
        BB_Parameters.scaleFactor_width = (double)bestWidth / (double)BB_Parameters.trainingResolution_width;
        BB_Parameters.startingRow  = (BB_Parameters.runningResolution_height / 2) - (int)(BB_Parameters.cutOff_Area_Of_Interest * BB_Parameters.scaleFactor_height);
        // Calculate Hough Parameter related to running resolution
        BB_Parameters.houghRhoResolution_RunningResolution_AccumulatorSpace = Math.max(Math.min(BB_Parameters.runningResolution_height, BB_Parameters.runningResolution_width) * (BB_Parameters.houghRhoResolution_PercentRunningResolution_AccumulatorSpace / 100), 1);
        if(BB_Parameters.startingRow < 0){
            Log.i("ERROR", "startingRow miscalculated");
            BB_Parameters.startingRow = 0; // Not really the right thing but what can we do? because the cutoff parameter was wrong to begin with
        }else if(BB_Parameters.startingRow > BB_Parameters.runningResolution_height){
            Log.i("ERROR", "startingRow miscalculated");
            BB_Parameters.startingRow = 0; // Not really the right thing but what can we do? because the cutoff parameter was wrong to begin with
        }
        BB_Parameters.startingColumn  = (int)(BB_Parameters.runningResolution_width - (BB_Parameters.runningResolution_width * BB_Parameters.widthPercentToCalculateForRoadArea / 100.0));
        // check for validity
        if(BB_Parameters.startingColumn > BB_Parameters.runningResolution_width)
            BB_Parameters.startingColumn = BB_Parameters.runningResolution_width - 1;
        else if(BB_Parameters.startingColumn < 0)
            BB_Parameters.startingColumn = 0;

        // Set the off road edge thresholds
        // Use camera tilt angle to use proper bottom row meters parameter to determine how many meters correspond to each pixel
        BB_Parameters.pixelsPerMeter = 0;
        if(BB_Parameters.cameraTiltAngle == 5)
            BB_Parameters.pixelsPerMeter = BB_Parameters.runningResolution_width / BB_Parameters.metersOfBottomRow5DegreeTilt;
        else if(BB_Parameters.cameraTiltAngle == 10)
            BB_Parameters.pixelsPerMeter = BB_Parameters.runningResolution_width / BB_Parameters.metersOfBottomRow10DegreeTilt;
        else if(BB_Parameters.cameraTiltAngle == 20)
            BB_Parameters.pixelsPerMeter = BB_Parameters.runningResolution_width / BB_Parameters.metersOfBottomRow20DegreeTilt;

        // calculate how many pixels off the center the line can be
        BB_Parameters.leftOfCenterThreshold = BB_Parameters.runningResolution_width / 2 - Math.round(Math.round(BB_Parameters.leftOfCenterOffsetMeters * BB_Parameters.pixelsPerMeter));
        BB_Parameters.rightOfCenterThreshold = BB_Parameters.runningResolution_width / 2 + Math.round(Math.round(BB_Parameters.rightOfCenterOffsetMeters * BB_Parameters.pixelsPerMeter));

        if(BB_Parameters.scaleFactor_height > 1.0 || BB_Parameters.scaleFactor_width > 1.0){
            Log.i("PARAMETERS", "Parameter scale factor greater than 1.0, meaning we are trying to increase resolution rather than decrease");
        }

        //this means we found a resolution in the range we found
        disconnectCamera();
        connectCamera(bestWidth, bestHeight);
    }



    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }


    public void setPreviewFPS(double min, double max) throws IOException {


        Camera.Parameters params = mCamera.getParameters();
   /*     List<int[]> frameRates = params.getSupportedPreviewFpsRange();
        Log.e("frameRates","JUST SEE THE LIST");
        for (int[] rates : frameRates) {
            Log.e("min : ", "max :"+ rates[0] + rates[1]);
        }*/
        params.setPreviewFpsRange((int)(10000),(int)(12000));
        mCamera.setParameters(params);
    }
}

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

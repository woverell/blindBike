package edu.csueb.ilab.blindbike.blindbike;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

import java.io.IOException;
import java.util.List;

/**
 * Created by Admin on 5/14/2015.
 */
public class CustomizeView extends JavaCameraView{

    private static final String TAG = "Sample::Tutorial3View";
    private String mPictureFileName;
    Camera.Parameters cameraParameters;

    public CustomizeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
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

    public float getFocalLength()
    {
        cameraParameters = mCamera.getParameters();
        Camera.CameraInfo myinfo = new Camera.CameraInfo();
        float l=cameraParameters.getFocalLength();
        return l;
    }
}

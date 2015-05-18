package edu.csueb.ilab.blindbike.blindbike;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;

/**
 * Created by Admin on 5/1/2015.
 */
public class Navi_activity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2,View.OnTouchListener{

    private static final String TAG = "OCVSample::Activity";
    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private CustomizeView mMyCamera;
    private MyView myv;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(Navi_activity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.navi_activity);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mMyCamera= CustomizeView.class.cast(findViewById(R.id.tutorial1_activity_java_surface_view));
        // the only camera needed is the back facing!
        // Some code to check for the camera if its available.
        mOpenCvCameraView.setMaxFrameSize(640,480);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
                mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_BGRA2GRAY);
        //step 1 downsize an image
        // Usual resolution is way too high
        //downsampling 1080x1920 by 2
        //480x270 should be the ideal
        // step2any preprocessing or noise removal maybe no!!
        //step 3 do we only process every x frames?
            // we would figure out x is a fn of X=f(max bike speed,max distance between frames)
            //if(c==x) (process frames)
            //c++
            //if(c>x) (set c=1)

        Log.i("Resolution","width = "+mRgba.width()+"Height = "+mRgba.height());


        //step 4 Performance Monitoring
        // maybe we shd have a timer fn aso we knw which frames are processing
        //write out to a log file to monitor performance

        //Note looking at FPS Meter




        // Call road Following(William)


        //Call intersection detection (Chris)
        return mGray;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        try {

            mMyCamera.setPreviewFPS(Double.valueOf(1000),Double.valueOf(6000));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class MyView extends JavaCameraView{

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyView(Context context,int cameraId)
    {
        super(context,cameraId);
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
}
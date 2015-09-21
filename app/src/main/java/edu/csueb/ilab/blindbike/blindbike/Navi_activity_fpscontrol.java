package edu.csueb.ilab.blindbike.blindbike;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/**
 * Created by Admin on 5/1/2015.
 */
public class Navi_activity_fpscontrol extends Activity implements CameraBridgeViewBase.CvCameraViewListener2,View.OnTouchListener{

    private static final String TAG = "OCVSample::Activity";
    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private CustomizeView mMyCamera;
    // The index of the active camera.
    private int mCameraIndex;
    // A key for storing the index of the active camera.
    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    // Whether the active camera is front-facing.
    // If so, the camera view should be mirrored.
    private boolean mIsCameraFrontFacing;
    // The number of cameras on the device.
    private int mNumCameras;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(Navi_activity_fpscontrol.this);
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
        if(getResources().getBoolean(R.bool.DEVELOPMENT_MODE)) {
            setContentView(R.layout.navi);
        }else
        {
            setContentView(R.layout.navi);
        }
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mMyCamera= CustomizeView.class.cast(findViewById(R.id.tutorial1_activity_java_surface_view));
        // the only camera needed is the back facing!
        // Some code to check for the camera if its available.

        if(savedInstanceState!=null)
        {
            mCameraIndex=savedInstanceState.getInt(STATE_CAMERA_INDEX,0);

        }
        else
        {
            mCameraIndex=0;
        }

        Camera.CameraInfo cameraInfo=new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraIndex, cameraInfo);
        mIsCameraFrontFacing=(cameraInfo.facing== Camera.CameraInfo.CAMERA_FACING_FRONT);
        mNumCameras=Camera.getNumberOfCameras();


        mOpenCvCameraView.setMaxFrameSize(640,480);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current camera index.
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);
        super.onSaveInstanceState(savedInstanceState);
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
        //if front facing camera is selected
        if(mIsCameraFrontFacing)
        {
            Core.flip(mRgba, mRgba, 1);
        }

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}


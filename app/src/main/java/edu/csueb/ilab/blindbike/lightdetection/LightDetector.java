package edu.csueb.ilab.blindbike.lightdetection;

import android.os.Environment;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Lynne on 5/22/2015. new
 */
public class LightDetector {
    public void processFrame(Mat imgFrame) {
       // change
    }

    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mLowerBound = new Scalar(176,255,244); 	//for blue 120,100,100
    // for flouracent green light 57,255,20
    private Scalar mUpperBound = new Scalar(177,255,252); 	// for blue 179,255,255 , blue cap 28,28,37
    // for flouracent green light 57,255,200
    // for gray signs 76,55,28
    // for gray signs 89,62,33 ,blue cap 80,109,149
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.01; //tried 0.4
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(25,50,50,0);	//initial val 25,50,50,0 //214,55,52,0 for the blue cap
    private Mat mSpectrum = new Mat();						//
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();

    // Cache
    Mat mPyrDownMat = new Mat();
    Mat mHsvMat = new Mat();
    Mat mMask = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();

    SimpleDateFormat df= new SimpleDateFormat("yyyy_MM_dd_HH_mm_yyyy");

    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    }

    public void setHsvColor(Scalar hsvColor) {
        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0;
        double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);

        for (int j = 0; j < maxH-minH; j++) {
            byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
            spectrumHsv.put(0, j, tmp);
        }

        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2BGR_FULL, 4); //COLOR_HSV2RGB_FULL
    }

    public Mat getSpectrum() {
        return mSpectrum;
    }

    public void setMinContourArea(double area) {
        mMinContourArea = area;
    }

    public void process(Mat rgbaImage) {
        Scalar colorGreen=new Scalar(0, 128, 0);


        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_BGR2HSV_FULL);

        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
        Imgproc.dilate(mMask, mDilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Core.multiply(contour, new Scalar(4,4), contour);

                mContours.add(contour);
            }
        }

        File path =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        String filename = "christ"+df.format(new Date()).toString()+".png";
        File file = new File(path, filename);
        filename = file.toString();
        Boolean save;

        MatOfPoint2f approxCurve=new MatOfPoint2f();
        for(int i=0;i<contours.size();i++)
        {
            MatOfPoint2f countour2f = new MatOfPoint2f(contours.get(i).toArray());
            double approxDistance = Imgproc.arcLength(countour2f, true)*0.02;
            Imgproc.approxPolyDP(countour2f, approxCurve, approxDistance, true);

            // Convert back to Contour
            MatOfPoint points=new MatOfPoint(approxCurve.toArray());

            //Get Bounding rect of contour
            Rect rect=Imgproc.boundingRect(points);

            //draw enclosing rectangle
            Mat ROI = rgbaImage.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);

            save= Highgui.imwrite(filename,ROI);
            if (save == true)
                Log.i("Save Status", "SUCCESS writing image to external storage");
            else
                Log.i("Save Status", "Fail writing image to external storage");

            Core.rectangle(rgbaImage, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(255,0,0,255),3);
        }


    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }
}

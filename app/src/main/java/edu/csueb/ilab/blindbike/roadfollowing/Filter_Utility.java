package edu.csueb.ilab.blindbike.roadfollowing;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import edu.csueb.ilab.blindbike.blindbike.BB_Parameters;

/**
 *
 */
public class Filter_Utility {

    static boolean isRoadClass(int classID){
        for(int i = 0; i < BB_Parameters.road_classes.length; i++){
            if(classID == BB_Parameters.road_classes[i])
                return true;
        }
        return false;
    }

    /**
     * Classify the pixels of the image based on their feature vectors into 0 - no class or
     * 255 - the class.  The class represented by the gmm.
     * @param gmm
     * @param inputImg
     * @param outputImg
     */
    static void classifyImageIntoDataClass(GMM gmm, Mat inputImg, Mat outputImg, int class_index, int[][] classArray){

        // Step 1: Cycle through every feature vector (each feature vector corresponds to a pixel)
        // and classify it using the gmm into either 0(not of that class) or 255(of that class)
        double[] sample = new double[BB_Parameters.featureVectorDimension];

        for(int i = 0; i < inputImg.rows(); i++){
            for(int j = 0; j < inputImg.cols(); j++){
                sample[2] = inputImg.get(i, j)[0]; // R
                sample[1] = inputImg.get(i, j)[1]; // G
                sample[0] = inputImg.get(i, j)[2]; // B

                if((gmm.predict(sample)) >= 0){
                    // The pixel at this location belongs to this class
                    outputImg.put(i, j, 255);
                    classArray[i][j] =class_index;
                }else {
                    // The pixel at this location does not belong to this class

                    outputImg.put(i, j, 0);
                    classArray[i][j] = -1;

                }

            }

        }

        //return classArray;
    }

    /**
     * This method will classify the input images based on examining each pixel's feature vector
     * into one of a set of gmms.  Creates array where each pixel is represented by the label of its
     * classified data class.  This label can be found in the gmm class.
     * If BB_Parameters.pseudo_Creation = ture replaces original pixel values with the pseudo color representing
     * the determined data class for that pixel.
     * @param gmms
     * @param img
     * @return 2d array representing labeled image of classes
     */
    static void classifyImageIntoDataClasses(Vector<GMM> gmms, Mat img, Mat roadBinaryImage, int[][] classArray){
        byte buff1[] = new byte[(int)img.total() * img.channels()]; // to store original img
        byte buff2[] = new byte[(int)roadBinaryImage.total() * roadBinaryImage.channels()]; // to store roadBinaryImage

        // copy img into buff1
        img.get(0,0, buff1);

        //String test = img.dump();

            int count_Classes[] = new int[gmms.size()];

            for(int c =0; c< gmms.size(); c++)
                count_Classes[c] = 0;

            // Step 1: Cycle through every feature vector (each feature vector corresponds to a pixel)
            // and classify it using the GMMs, one GMM per data class (one gmm for sky, one for road, ...)
            double[] sample = new double[BB_Parameters.featureVectorDimension];

            for(int i = 0; i < img.rows(); i++){
                for(int j = 0; j < img.cols(); j++){
                    // WILLIAM: SWITCHING RGB TO BGR -- gmm uses order of b,g,r not the traditional r,g,b
                    sample[2] = buff1[(i*img.cols()*4)+(j*4)+0] & 0xFF;
                    sample[1] = buff1[(i*img.cols()*4)+(j*4)+1] & 0xFF;
                    sample[0] = buff1[(i*img.cols()*4)+(j*4)+2] & 0xFF;
                    /*sample[2] = nextFeatureVector.get(i, j)[0]; // R
                    sample[1] = nextFeatureVector.get(i, j)[1]; // G
                    sample[0] = nextFeatureVector.get(i, j)[2]; // B
                    test1 = buff1[(i*img.cols()*4)+(j*4)+0] & 0xFF;
                    test2 = buff1[(i*img.cols()*4)+(j*4)+1] & 0xFF;
                    test3 = buff1[(i*img.cols()*4)+(j*4)+2] & 0xFF;
                    if(test1 != (int)sample[2] || test2 != (int)sample[1] || test3 != (int)sample[0])
                        Log.i("buff_test", "FAIL");
                    else
                        Log.i("buff_test","SUCCEED");*/

                    // Cycle through GMM list to figure out which class has the highest probability
                    double max = -10;
                    int class_index = -1;
                    double tmp;

                    for(int g = 0; g < gmms.size(); g++){
                        if((tmp = gmms.elementAt(g).predict(sample)) > max && tmp >= 0.0){
                            // This means it is class g at this point
                            class_index = g;
                            max = tmp;
                        }

                    }


                    //SOme counting AND
                    //create binary image of road/not road pixels for future processing
                    if(class_index == -1 ) {
                        //roadBinaryImage.put(i, j, 0);
                        buff2[i*img.cols()+j] = 0;
                    }
                    else {
                        count_Classes[class_index] += 1;
                        if (class_index == 1) { //MEANS WE ALWAYS need to keep this meaning road
                            //roadBinaryImage.put(i, j, 255);
                            buff2[i*img.cols()+j] = -1;
                        }
                        else {
                           //roadBinaryImage.put(i, j, 0);
                            buff2[i*img.cols()+j] = 0;
                        }
                    }
                    classArray[i][j] = class_index;
                }
            }

        roadBinaryImage.put(0,0,buff2); // put the buffer in the roadBinaryImage
        //return classArray;
    }

    /**
     * Classify the pixels of the image based on their feature vectors into 0 - no class or
     * 255 - the class.  The class represented by the gmm.
     * @param gmms
     * @param img_HSV
     * @param roadBinaryImage
     * @param classArray
     */
    static void classifyImageIntoDataClassesHSV(Vector<GMM> gmms, Mat img_HSV, Mat roadBinaryImage, int[][] classArray){
        byte buff1[] = new byte[(int)img_HSV.total() * img_HSV.channels()]; // to store original img
        byte buff2[] = new byte[(int)roadBinaryImage.total() * roadBinaryImage.channels()]; // to store roadBinaryImage

        // copy img_HSV into buff1
        img_HSV.get(0,0, buff1);

        //String test = img.dump();

        int count_Classes[] = new int[gmms.size()];

        for(int c =0; c< gmms.size(); c++)
            count_Classes[c] = 0;

        // Step 1: Cycle through every feature vector (each feature vector corresponds to a pixel)
        // and classify it using the GMMs, one GMM per data class (one gmm for sky, one for road, ...)
        double[] sample = new double[BB_Parameters.featureVectorDimension];

        for(int i = 0; i < img_HSV.rows(); i++){
            for(int j = 0; j < img_HSV.cols(); j++){
                // Pull out H, S, V values
                sample[0] = buff1[(i*img_HSV.cols()*3)+(j*3)+0] & 0xFF; // H
                sample[1] = buff1[(i*img_HSV.cols()*3)+(j*3)+1] & 0xFF; // S
                sample[2] = buff1[(i*img_HSV.cols()*3)+(j*3)+2] & 0xFF; // V

                // Cycle through GMM list to figure out which class has the highest probability
                double max = -10;
                int class_index = -1;
                double tmp;

                for(int g = 0; g < gmms.size(); g++){
                    if((tmp = gmms.elementAt(g).predict(sample)) > max && tmp >= 0.0){
                        // This means it is class g at this point
                        class_index = g;
                        max = tmp;
                    }
                }

                //Some counting AND
                //create binary image of road/not road pixels for future processing
                if(class_index == -1 ) {
                    //roadBinaryImage.put(i, j, 0);
                    buff2[i*img_HSV.cols()+j] = 0;
                }
                else {
                    count_Classes[class_index] += 1;
                    if(isRoadClass(class_index)) { //MEANS WE ALWAYS need to keep this meaning road
                        //roadBinaryImage.put(i, j, 255);
                        buff2[i*img_HSV.cols()+j] = -1; // this is a road pixel, -1 for byte buffer
                    }
                    else {
                        //roadBinaryImage.put(i, j, 0);
                        buff2[i*img_HSV.cols()+j] = 0;
                    }
                }
                classArray[i][j] = class_index;
            }
        }

        roadBinaryImage.put(0,0,buff2); // put the buffer in the roadBinaryImage
        //return classArray;
    }

    /**
     * Classify the pixels of the image based on their feature vectors into 0 - no class or
     * 255 - the class.  The class represented by the fixed range model.  NOT IMPLEMENTED
     * @param fixedRangeModel
     * @param inputImg
     * @param outputImg
     */
    static void classifyImageIntoDataClass(ClassedMultipleFixedRangeModel fixedRangeModel, Mat inputImg, Mat outputImg, int[][] classArray) {

    }

    /**
     * This method will classify the input images based on examining each pixel's feature vector
     * into one of a set of fixed range models.  Creates array where each pixel is represented by the label of its
     * classified data class.  This label can be found in the ClassedMultipleFixedRangeModel class.
     * If BB_Parameters.pseudo_Creation = ture replaces original pixel values with the pseudo color representing
     * the determined data class for that pixel.
     * @param fixedRangeModels
     * @param img
     * @return 2d array representing labeled image of classes
     */
    static int[][] classifyImageIntoDataClasses(Vector<ClassedMultipleFixedRangeModel> fixedRangeModels, Mat img, Mat roadBinaryImage, int[][]classArray, int ignore){
        int count_NO_CLASS = 0;
        int count_Classes[] = new int[fixedRangeModels.size()];

        for(int c =0; c< fixedRangeModels.size(); c++)
            count_Classes[c] = 0;

        // Step 1: Cycle through every feature vector (each feature vector corresponds to a pixel)
        // and classify it using the fixedRangeModels, one fixedRangeModel per data class (one gmm for sky, one for road, ...)
        Mat nextFeatureVector;
        double[] sample = new double[BB_Parameters.featureVectorDimension];
        int rowNum = 0;
        int colNum = 0;


        nextFeatureVector = img;
        for(int i = 0; i < nextFeatureVector.rows(); i++){
            for(int j = 0; j < nextFeatureVector.cols(); j++){
                float center[] = new float[3];
                float best_center[] = {-1.0f, -1.0f, -1.0f};

                // WILLIAM: SWITCHING RGB TO BGR -- gmm uses order of b,g,r not the traditional r,g,b, this is accurate I have tested
                sample[2] = nextFeatureVector.get(i, j)[0]; // R
                sample[1] = nextFeatureVector.get(i, j)[1]; // G
                sample[0] = nextFeatureVector.get(i, j)[2]; // B

                //Log.i("RGB", Double.toString(sample[2]) + Double.toString(sample[1]) + Double.toString(sample[0]));

                // Cycle through fixedRangeModels list to figure out which class has the highest probability
                //  This is simply measured by its distance the center of the "best" range that belongs to
                //  each data class.
                int class_index = -1;

                for(int g = 0; g < fixedRangeModels.size(); g++){
                    //see if sample falls in one of the fixed ranges of current model g
                    // this method returns the center of the best fit range (closest to center) OR a vector 0f -1,-1,-1 if does
                    // not fall into any range of this model
                    center = fixedRangeModels.elementAt(g).is_In_Ranges((int)sample[2],(int)sample[1],(int)sample[0]);
                    if (center[0] == -1) //sample does not belong to this model
                        continue;
                    else {
                        //make sure that this is the best class so far
                        // This means it is class g at this point
                        if(best_center[0] < 0){
                            class_index = g;
                            best_center = center;
                        }
                        else if(ClassedMultipleFixedRangeModel.betterCenter(sample[2], sample[1], sample[0], center, best_center)) {
                            class_index = g;
                            best_center = center;
                        }
                    }

                }


                //SOme counting AND
                //create binary image of road/not road pixels for future processing
                if(class_index == -1 ) {
                    count_NO_CLASS += 1;
                    roadBinaryImage.put(i, j, 0);
                }
                else {
                    count_Classes[class_index] += 1;
                    if(isRoadClass(class_index)) //MEANS WE ALWAYS need to keep this meaning road
                        roadBinaryImage.put(i, j, 255);
                    else
                        roadBinaryImage.put(i,j,0);
                }
                classArray[rowNum][colNum] = class_index;


                colNum++;
                if(colNum%img.cols() == 0){
                    colNum = 0;
                    rowNum++;
                }

            }
        }

        /*// If pseudo creation parameter is true then create pseudo image
        if(BB_Parameters.displaypseudoLabelImage)
            createPseudoImage(classArray, fixedRangeModels, img, 1);*/

        return classArray;

    }

    static Vector<GMM> readParametersForMixtureOfGaussiansForAllDataClasses(String gmmsfilename, Context context, double standard_Deviation_Factor){
        Vector<GMM> gmms = new Vector();
        // Step 1: Cycle through each class's file representing its GMM (Gaussian Mixture Model)

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(gmmsfilename)));
            BufferedReader br2;
            StringTokenizer st;
            // Read in first line containing number of data classes
            int numDataClasses = Integer.parseInt(br.readLine());

            String s;
            // Read in name, pseudocolor, and gmm data file name For Each Data Class
            for(int i = 0; i < numDataClasses; i ++){
                GMM gmm = new GMM();
                // Read in name of class
                gmm.className = br.readLine();

                double[] meansForApache = null;
                double[][] covForApache = null;

                // Read in string representing RGB of pseudocolor
                s = br.readLine();
                st = new StringTokenizer(s);
                gmm.pseudocolor[0] = Integer.parseInt(st.nextToken());
                gmm.pseudocolor[1] = Integer.parseInt(st.nextToken());
                gmm.pseudocolor[2] = Integer.parseInt(st.nextToken());
                gmm.pseudocolor[3] = Integer.parseInt(st.nextToken());

                // Read in filename for gmm parameters
                gmm.fileName = br.readLine();


                // Step 2: Read in current class's GMM and create an instance of the jMEF.PVectorMatrix for use
                // in the call of MultivariateGaussian.density(*)
                // Also store weight for GMM
                br2 = new BufferedReader(new InputStreamReader(context.getAssets().open(gmm.fileName)));

                gmm.numCluster = Integer.parseInt(br2.readLine());

                s = br2.readLine();
                s = s.replace(",", "");
                s = s.replace("[",  "");
                s = s.replace("]", "");
                st = new StringTokenizer(s);
                Vector weights = new Vector();
                Vector meansForClusters = new Vector();


                for(int w = 0; w < gmm.numCluster; w++){
                    weights.add(Double.parseDouble(st.nextToken())); // reading in weights for each cluster
                }
                gmm.weights = weights;
                // Read in means vector for all clusters

                for(int q=0; q < gmm.numCluster; q++)  //cycle each cluster
                {
                    double[] means = new double[BB_Parameters.featureVectorDimension];
                    s = br2.readLine();
                    s = s.replace(",", "");
                    s = s.replace("[",  "");
                    s = s.replace("]", "");
                    s = s.replace(";", "");
                    st = new StringTokenizer(s);
                    for(int w = 0; w < BB_Parameters.featureVectorDimension; w++){
                        means[w] = (Double.parseDouble(st.nextToken()));
                    }
                    meansForClusters.add(means);
                }


                // Read in covariance matrix for each cluster

                for(int q=0; q < gmm.numCluster; q++)  //cycle each cluster
                {
                    double[][] covMat = new double[BB_Parameters.featureVectorDimension][BB_Parameters.featureVectorDimension];
                    for(int z = 0; z < BB_Parameters.featureVectorDimension; z++) // for each row in this cluster's covariance matrix
                    {
                        s = br2.readLine();
                        s = s.replace(",", "");
                        s = s.replace("[",  "");
                        s = s.replace("]", "");
                        s = s.replace(";", "");
                        st = new StringTokenizer(s);
                        for(int w = 0; w < BB_Parameters.featureVectorDimension; w++){ // Cycling through each row
                            covMat[z][w] = (Double.parseDouble(st.nextToken()));
                        }
                    }

                    // Now store this PVectorMatrix and corresponding Means vector inside the gmm
                    gmm.addGaussian((double[]) meansForClusters.get(q), covMat);
                }


                // Calculate minimum probability value for each cluster of this gmm to be within +-(2 * the std dev)
                for(int q=0; q < gmm.numCluster; q++)  //cycle each cluster
                {
                    double[][] std = new double[BB_Parameters.featureVectorDimension][BB_Parameters.featureVectorDimension];
                    for(int v = 0; v < BB_Parameters.featureVectorDimension; v++){
                        for(int w = 0; w < BB_Parameters.featureVectorDimension; w++){
                            std[v][w] = standard_Deviation_Factor * Math.sqrt(gmm.gaussians.elementAt(q).getCovariances().getData()[v][w]); //TODO: MAKE SURE WORKS
                        }
                    }
                    RealMatrix stdMat = MatrixUtils.createRealMatrix(std);
                    // Calculate minimum  sample value = mean - std to be considered part of this cluster
                    double[] identity = new double[BB_Parameters.featureVectorDimension];
                    for(int v = 0; v < BB_Parameters.featureVectorDimension; v++){
                        identity[v] = 1.0;
                    }

                    RealVector min = new ArrayRealVector();
                    RealVector meansMat = new ArrayRealVector(gmm.gaussians.elementAt(q).getMeans());
                    RealVector stdVector = new ArrayRealVector(stdMat.operate(identity));
                    min = meansMat.subtract(stdVector);

                    double temp = gmm.gaussians.elementAt(q).density(gmm.gaussians.elementAt(q).getMeans());
                    if(BB_Parameters.VERBOSE)
                        System.out.println(" Prob at mean" + temp  );

                    //determine minimum probability value corresponding to this minimum
                    gmm.minimum_probability_to_be_in_cluster.add(gmm.gaussians.elementAt(q).density(min.toArray()));

                    if(BB_Parameters.VERBOSE)
                        System.out.println(" Prob at 2 std out is " + gmm.minimum_probability_to_be_in_cluster.elementAt(q) );

                }

                //add created gmm to the ggms vector
                gmms.add(gmm);
                br2.close();
            }
            br.close();

            return gmms;





        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.i("WILLGMM", e.toString());
            e.printStackTrace();
            return null;
        }




    }

    /**
     * Creates an adaptive model of the mean and standard deviations of the r,g,b values for the pixels in the labeled image
     * that are labeled detect_class_index.  Using the mean and standard deviation we detect outliers as those being more than
     * BB_Parameters.adaptive_std_dev_range_factor * std away from the mean.  For every outlier detected of the label detect_class_index
     * we reclassify the pixel and update the returned labeled image as well as updating roadBinaryImage as appropriate.
     * @param labeledImage
     * @param gmms
     * @param originalImg
     * @param roadBinaryImage
     * @return
     */
    static int[][] adaptiveOutlierElimination(int[][] labeledImage, Vector<GMM> gmms, Mat originalImg, Mat roadBinaryImage){
        float meanR = 0f, meanG = 0f, meanB = 0f, stdR = 0f, stdG = 0f, stdB = 0f;
        int counter = 0;
        int count_Outliers=0;
        double[] sample = new double[BB_Parameters.featureVectorDimension];
        int[][] newLabeledImage;

        newLabeledImage = new int[labeledImage.length][labeledImage[0].length];

        // Calculate mean
        for(int i = 0; i < labeledImage.length; i++) { // cycle through rows
            for (int j = 0; j < labeledImage[0].length; j++) { // cycle through columns
                if(isRoadClass(labeledImage[i][j])){
                    // WILLIAM: CHECK RGB CORRECT
                    meanR += originalImg.get(i, j)[0]; // R
                    meanG += originalImg.get(i, j)[1]; // G
                    meanB += originalImg.get(i, j)[2]; // B
                    counter++;
                }
            }
        }

        meanR = ((float)meanR)/counter;
        meanG = ((float)meanG)/counter;
        meanB = ((float)meanB)/counter;

       double x;
        // Calculate std
        for(int i = 0; i < labeledImage.length; i++) { // cycle through rows
            for (int j = 0; j < labeledImage[0].length; j++) { // cycle through columns
                if(isRoadClass(labeledImage[i][j])){
                    // WILLIAM: CHECK RGB CORRECT
                    x = originalImg.get(i,j)[0];

                    stdR += (x - meanR)*(x - meanR); // R
                    stdG += (originalImg.get(i, j)[1] - meanG)*(originalImg.get(i, j)[1] - meanG); // G
                    stdB += (originalImg.get(i, j)[2] - meanB)*(originalImg.get(i, j)[2] - meanB); // B
                }
            }
        }

        stdR = (float)Math.sqrt(stdR / (counter - 1));
        stdG = (float)Math.sqrt(stdG / (counter - 1));
        stdB = (float)Math.sqrt(stdB / (counter - 1));

        // Visit every pixel labeled class_index and test to see if it is an outlier
        for(int i = 0; i < labeledImage.length; i++) { // cycle through rows
            for (int j = 0; j < labeledImage[0].length; j++) { // cycle through columns
                newLabeledImage[i][j] = labeledImage[i][j];
                if (isRoadClass(labeledImage[i][j])) {
                    if(  Math.abs(originalImg.get(i, j)[0] - meanR) > BB_Parameters.adaptive_std_dev_range_factor * stdR ||
                            Math.abs(originalImg.get(i, j)[1] - meanG) > BB_Parameters.adaptive_std_dev_range_factor * stdG ||
                            Math.abs(originalImg.get(i, j)[2] - meanB) > BB_Parameters.adaptive_std_dev_range_factor * stdB ){
                        // outlier detected
                        Log.v("Outlier", "At " + i + ", " + j + " = " + originalImg.get(i,j)[0] + "," + originalImg.get(i,j)[1] + ", " + originalImg.get(i,j)[2]);
                        //START OF RECLASSIFICATION
                        // Cycle through GMM list to figure out which class has the highest probability
                        // AND IS NOT class_index
                        double max = -10;
                        int class_index = -1;
                        double tmp;
                        // WILLIAM: SWITCHING RGB TO BGR -- gmm uses order of b,g,r not the traditional r,g,b
                        sample[2] = originalImg.get(i, j)[0]; // R
                        sample[1] = originalImg.get(i, j)[1]; // G
                        sample[0] = originalImg.get(i, j)[2]; // B

                        for(int g = 0; g < gmms.size(); g++){
                            if(isRoadClass(g)) continue;
                            if((tmp = gmms.elementAt(g).predict(sample)) > max && tmp >= 0.0){
                                // This means it is class g at this point
                                class_index = g;
                                max = tmp;
                            }

                        }
                        //END OF RECLASSIFICATION
                        count_Outliers++;

                        //SOme counting AND
                        //create binary image of road/not road pixels for future processing
                        if(class_index == -1 ) {
                            roadBinaryImage.put(i, j, 0);
                        }
                        else {

                            if(isRoadClass(class_index)) //MEANS WE ALWAYS need to keep this meaning road
                                roadBinaryImage.put(i, j, 255);
                            else
                                roadBinaryImage.put(i,j,0);
                        }
                        newLabeledImage[i][j] = class_index;


                    }

                }
            }
        }

        Log.v("NumOutliers", Integer.toString(count_Outliers));
        return newLabeledImage;
    }

    /**
     * Creates an adaptive model of the mean and standard deviations of the h,s,v values for the pixels in the labeled image
     * that are labeled detect_class_index.  Using the mean and standard deviation we detect outliers as those being more than
     * BB_Parameters.adaptive_std_dev_range_factor * std away from the mean.  For every outlier detected of the label detect_class_index
     * we reclassify the pixel and update the returned labeled image as well as updating roadBinaryImage as appropriate.
     * @param labeledImage
     * @param gmms
     * @param originalImg
     * @param roadBinaryImage
     * @return
     */
    static int[][] adaptiveOutlierEliminationHSV(int[][] labeledImage, Vector<GMM> gmms, Mat originalImg, Mat roadBinaryImage){
        float meanH = 0f, meanS = 0f, meanV = 0f, stdH = 0f, stdS = 0f, stdV = 0f;
        int counter = 0;
        int count_Outliers=0;
        double[] sample = new double[BB_Parameters.featureVectorDimension];
        int[][] newLabeledImage;

        newLabeledImage = new int[labeledImage.length][labeledImage[0].length];

        // Calculate mean
        for(int i = 0; i < labeledImage.length; i++) { // cycle through rows
            for (int j = 0; j < labeledImage[0].length; j++) { // cycle through columns
                if(isRoadClass(labeledImage[i][j])){
                    meanH += originalImg.get(i, j)[0]; // H
                    meanS += originalImg.get(i, j)[1]; // S
                    meanV += originalImg.get(i, j)[2]; // V
                    counter++;
                }
            }
        }

        meanH = ((float)meanH)/counter;
        meanS = ((float)meanS)/counter;
        meanV = ((float)meanV)/counter;

        double x;
        // Calculate std
        for(int i = 0; i < labeledImage.length; i++) { // cycle through rows
            for (int j = 0; j < labeledImage[0].length; j++) { // cycle through columns
                if(isRoadClass(labeledImage[i][j])){
                    // WILLIAM: CHECK RGB CORRECT
                    x = originalImg.get(i,j)[0];

                    stdH += (x - meanH)*(x - meanH); // R
                    stdS += (originalImg.get(i, j)[1] - meanS)*(originalImg.get(i, j)[1] - meanS); // G
                    stdV += (originalImg.get(i, j)[2] - meanV)*(originalImg.get(i, j)[2] - meanV); // B
                }
            }
        }

        stdH = (float)Math.sqrt(stdH / (counter - 1));
        stdS = (float)Math.sqrt(stdS / (counter - 1));
        stdV = (float)Math.sqrt(stdV / (counter - 1));

        // Visit every pixel labeled class_index and test to see if it is an outlier
        for(int i = 0; i < labeledImage.length; i++) { // cycle through rows
            for (int j = 0; j < labeledImage[0].length; j++) { // cycle through columns
                newLabeledImage[i][j] = labeledImage[i][j];
                if (isRoadClass(labeledImage[i][j])) {
                    if(  Math.abs(originalImg.get(i, j)[0] - meanH) > BB_Parameters.adaptive_std_dev_range_factor * stdH ||
                            Math.abs(originalImg.get(i, j)[1] - meanS) > BB_Parameters.adaptive_std_dev_range_factor * stdS ||
                            Math.abs(originalImg.get(i, j)[2] - meanV) > BB_Parameters.adaptive_std_dev_range_factor * stdV ){
                        // outlier detected
                        Log.v("Outlier", "At " + i + ", " + j + " = " + originalImg.get(i,j)[0] + "," + originalImg.get(i,j)[1] + ", " + originalImg.get(i,j)[2]);
                        //START OF RECLASSIFICATION
                        // Cycle through GMM list to figure out which class has the highest probability
                        // AND IS NOT class_index
                        double max = -10;
                        int class_index = -1;
                        double tmp;
                        // WILLIAM: SWITCHING RGB TO BGR -- gmm uses order of b,g,r not the traditional r,g,b
                        sample[2] = originalImg.get(i, j)[0]; // R
                        sample[1] = originalImg.get(i, j)[1]; // G
                        sample[0] = originalImg.get(i, j)[2]; // B

                        for(int g = 0; g < gmms.size(); g++){
                            if(isRoadClass(g)) continue;
                            if((tmp = gmms.elementAt(g).predict(sample)) > max && tmp >= 0.0){
                                // This means it is class g at this point
                                class_index = g;
                                max = tmp;
                            }

                        }
                        //END OF RECLASSIFICATION
                        count_Outliers++;

                        //SOme counting AND
                        //create binary image of road/not road pixels for future processing
                        if(class_index == -1 ) {
                            roadBinaryImage.put(i, j, 0);
                        }
                        else {

                            if(isRoadClass(class_index)) //MEANS WE ALWAYS need to keep this meaning road
                                roadBinaryImage.put(i, j, 255);
                            else
                                roadBinaryImage.put(i,j,0);
                        }
                        newLabeledImage[i][j] = class_index;


                    }

                }
            }
        }

        Log.v("NumOutliers", Integer.toString(count_Outliers));
        return newLabeledImage;
    }

    /**
     *
     * @param labeledImage
     * @param fixedRangeModels
     * @param originalImg
     * @param roadBinaryImage
     * @param ignore
     * @return
     */
    static int[][] adaptiveOutlierElimination(int[][] labeledImage, Vector<ClassedMultipleFixedRangeModel> fixedRangeModels, Mat originalImg, Mat roadBinaryImage, int ignore){
        return labeledImage;
    }

        /**
         * This method takes as input a matrix of same dimensions as original image
         * that contains the class label of each pixel, the vector of GMMs, and
         * a matrix to put the pseudo image.  The method uses the classArray values
         * to modify the pixels in pseudoImage to represent the r,g,b,a value associated
         * with that class.
         * @param classArray
         * @param gmms
         * @param pseudoImage
         */
    static void createPseudoImage(int[][] classArray, Vector<GMM> gmms, Mat pseudoImage){
        int imgWidth = classArray.length;
        int imgHeight = classArray[1].length;

        double[] c = {0,255,0,255};

        for(int i = 0; i < imgWidth; i++){
            for(int j = 0; j < imgHeight; j++){
                c = pseudoImage.get(i, j);
                if(classArray[i][j] == -1){
                    for(int k = 0; k < c.length; k++) {
                        c[k] += BB_Parameters.otherClassPseudocolor[k];
                        c[k] = c[k] / 2;
                        if(c[k] > 255)
                            c[k] = 255;
                    }
                    pseudoImage.put(i, j, c);
                }
                else{
                    for(int k = 0; k < c.length; k++) {
                        c[k] += gmms.elementAt(classArray[i][j]).pseudocolor[k];
                        c[k] = c[k] / 2;
                        if(c[k] > 255)
                            c[k] = 255;
                    }
                    pseudoImage.put(i, j, c);
                }
                    //pseudoImage.put(i, j, gmms.elementAt(classArray[i][j]).pseudocolor); // Note: each gmm in the gmm vector has associated with it a data class gmm.className
            }
        }
    }

    /**
     * This method takes as input a matrix of same dimensions as original image
     * that contains the class label of each pixel, the vector of GMMs, and
     * a matrix to put the pseudo image.  The method uses the classArray values
     * to modify the pixels in pseudoImage to represent the r,g,b,a value associated
     * with that class.
     * @param classArray
     * @param fixedRangeModels
     * @param pseudoImage
     */
    static void createPseudoImage(int[][] classArray, Vector<ClassedMultipleFixedRangeModel> fixedRangeModels, Mat pseudoImage, int ignore){
        int imgWidth = classArray.length;
        int imgHeight = classArray[1].length;

        double[] c = {0,255,0,255};

        for(int i = 0; i < imgWidth; i++){
            for(int j = 0; j < imgHeight; j++){
                c = pseudoImage.get(i, j);
                if(classArray[i][j] == -1){
                    //pseudoImage.put(i, j, BB_Parameters.otherClassPseudocolor);
                    for(int k = 0; k < c.length; k++) {
                        c[k] += BB_Parameters.otherClassPseudocolor[k];
                        c[k] = c[k] / 2;
                        if(c[k] > 255)
                            c[k] = 255;
                    }
                    pseudoImage.put(i, j, c);
                }
                else{
                    for(int k = 0; k < c.length; k++) {
                        c[k] += fixedRangeModels.elementAt(classArray[i][j]).pseudocolor[k];
                        c[k] = c[k] / 2;
                        if(c[k] > 255)
                            c[k] = 255;
                    }
                    pseudoImage.put(i, j, c);
                }
                    //pseudoImage.put(i, j, fixedRangeModels.elementAt(classArray[i][j]).pseudocolor); // Note: each gmm in the gmm vector has associated with it a data class gmm.className
            }
        }
    }

    /**
     * This function takes in a boolean image(1 channel) of values 0 and nonzero
     * and an outImage of the same size(rows/cols) and places the corresponding
     * color (classColor for nonzero, otherColor for zero) into outImage
     * @param boolImage
     * @param outImage
     * @param classColor
     * @param otherColor
     */
    static void displayBooleanImage(Mat boolImage, Mat outImage, double[] classColor, double[] otherColor){
        for(int i = 0; i < boolImage.rows(); i++){
            for(int j = 0; j < boolImage.cols(); j++){
                if(boolImage.get(i,j)[0] != 0)
                    outImage.put(i,j, classColor);
                else
                    outImage.put(i,j, otherColor);
            }
        }
    }
}

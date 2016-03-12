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
import org.opencv.core.Mat;

import edu.csueb.ilab.blindbike.blindbike.BB_Parameters;

/**
 *
 */
public class Filter_Utility {
    /**
     * Classify the pixels of the image based on their feature vectors into 0 - no class or
     * 255 - the class.  The class represented by the gmm.
     * @param gmm
     * @param inputImg
     * @param outputImg
     */
    static void classifyImageIntoDataClass(GMM gmm, Mat inputImg, Mat outputImg){
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
                }else {
                    // The pixel at this location does not belong to this class
                    outputImg.put(i,j, 0);
                }

            }

        }
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
    static int[][] classifyImageIntoDataClasses(Vector<GMM> gmms, Mat img, Mat roadBinaryImage){

            int[][] classArray = null;
            int imgHeight = -1;
            int imgWidth = -1;

            int count_NO_CLASS = 0;
            int count_Classes[] = new int[gmms.size()];

            for(int c =0; c< gmms.size(); c++)
                count_Classes[c] = 0;


                imgHeight = img.rows();
                imgWidth = img.cols();
                classArray = new int[imgHeight][imgWidth];

            // Step 1: Cycle through every feature vector (each feature vector corresponds to a pixel)
            // and classify it using the GMMs, one GMM per data class (one gmm for sky, one for road, ...)
            Mat nextFeatureVector;
            double[] sample = new double[BB_Parameters.featureVectorDimension];
            int rowNum = 0;
            int colNum = 0;

            nextFeatureVector = img;
            for(int i = 0; i < nextFeatureVector.rows(); i++){
                for(int j = 0; j < nextFeatureVector.cols(); j++){
                    // WILLIAM: SWITCHING RGB TO BGR
                    sample[2] = nextFeatureVector.get(i, j)[0]; // R
                    sample[1] = nextFeatureVector.get(i, j)[1]; // G
                    sample[0] = nextFeatureVector.get(i, j)[2]; // B

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
                        count_NO_CLASS += 1;
                        roadBinaryImage.put(i, j, 0);
                    }
                    else {
                        count_Classes[class_index] += 1;
                        if (class_index == 1) //MEANS WE ALWAYS need to keep this meaning road
                            roadBinaryImage.put(i, j, 255);
                        else
                            roadBinaryImage.put(i,j,0);
                    }
                    classArray[rowNum][colNum] = class_index;


                    colNum++;
                    if(colNum%imgWidth == 0){
                        colNum = 0;
                        rowNum++;
                    }

                }
            }

        // If pseudo creation parameter is true then create pseudo image
        if(BB_Parameters.displaypseudoLabelImage)
            createPseudoImage(classArray, gmms, img);

        return classArray;
    }

    /**
     * Classify the pixels of the image based on their feature vectors into 0 - no class or
     * 255 - the class.  The class represented by the gmm.
     * @param fixedRangeModel
     * @param inputImg
     * @param outputImg
     */
    static void classifyImageIntoDataClass(ClassedMultipleFixedRangeModel fixedRangeModel, Mat inputImg, Mat outputImg){ }

    /**
     * This method will classify the input images based on examining each pixel's feature vector
     * into one of a set of gmms.  Creates array where each pixel is represented by the label of its
     * classified data class.  This label can be found in the gmm class.
     * If BB_Parameters.pseudo_Creation = ture replaces original pixel values with the pseudo color representing
     * the determined data class for that pixel.
     * @param fixedRangeModels
     * @param img
     * @return 2d array representing labeled image of classes
     */
    static int[][] classifyImageIntoDataClasses(Vector<ClassedMultipleFixedRangeModel> fixedRangeModels, Mat img, Mat roadBinaryImage, int ignore){

        int[][] classArray = null;
        int imgHeight = -1;
        int imgWidth = -1;

        int count_NO_CLASS = 0;
        int count_Classes[] = new int[fixedRangeModels.size()];

        for(int c =0; c< fixedRangeModels.size(); c++)
            count_Classes[c] = 0;


        imgHeight = img.rows();
        imgWidth = img.cols();
        classArray = new int[imgHeight][imgWidth];

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

                // WILLIAM: SWITCHING RGB TO BGR, this is accurate I have tested
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
                    if (class_index == 1) //MEANS WE ALWAYS need to keep this meaning road
                        roadBinaryImage.put(i, j, 255);
                    else
                        roadBinaryImage.put(i,j,0);
                }
                classArray[rowNum][colNum] = class_index;


                colNum++;
                if(colNum%imgWidth == 0){
                    colNum = 0;
                    rowNum++;
                }

            }
        }

        // If pseudo creation parameter is true then create pseudo image
        if(BB_Parameters.displaypseudoLabelImage)
            createPseudoImage(classArray, fixedRangeModels, img, 1);

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

        for(int i = 0; i < imgWidth; i++){
            for(int j = 0; j < imgHeight; j++){
                if(classArray[i][j] == -1){
                    pseudoImage.put(i, j, BB_Parameters.otherClassPseudocolor);
                }
                else
                    pseudoImage.put(i, j, gmms.elementAt(classArray[i][j]).pseudocolor); // Note: each gmm in the gmm vector has associated with it a data class gmm.className
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

        for(int i = 0; i < imgWidth; i++){
            for(int j = 0; j < imgHeight; j++){
                if(classArray[i][j] == -1){
                    pseudoImage.put(i, j, BB_Parameters.otherClassPseudocolor);
                }
                else
                    pseudoImage.put(i, j, fixedRangeModels.elementAt(classArray[i][j]).pseudocolor); // Note: each gmm in the gmm vector has associated with it a data class gmm.className
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

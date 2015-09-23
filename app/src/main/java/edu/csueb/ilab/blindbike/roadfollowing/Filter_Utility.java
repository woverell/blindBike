package edu.csueb.ilab.blindbike.roadfollowing;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

/**
 * Created by Will on 9/21/2015.
 */
public class Filter_Utility {
    static void classifyImageIntoDataClasses(Mat featureMat, Vector<GMM> gmms, String filename, Vector imgVector){
        try {
            File f = new File(filename + ".dat");
            FileOutputStream fo = new FileOutputStream(f);

            DataOutputStream dos = new DataOutputStream(fo);
            f = new File(filename + "_Summary.dat");
            FileOutputStream fo2 = new FileOutputStream(f);

            DataOutputStream dos2 = new DataOutputStream(fo2);

            boolean oneImage = false; // this will be true if there is only one image being processed, if only one image then output pseudocolor image
            int[][] classArray = null;
            int imgHeight = -1;
            int imgWidth = -1;

            int count_NO_CLASS = 0;
            int count_Classes[] = new int[gmms.size()];

            for(int c =0; c< gmms.size(); c++)
                count_Classes[c] = 0;

            if(imgVector.size() == 1){
                oneImage = true;
                imgHeight = ((Mat)imgVector.elementAt(0)).rows();
                imgWidth = ((Mat)imgVector.elementAt(0)).cols();
                classArray = new int[imgHeight][imgWidth];
            }

            long startTime = System.currentTimeMillis();
            // Step 1: Cycle through every feature vector (each feature vector corresponds to a pixel)
            // and classify it using the GMMs, one GMM per data class (one gmm for sky, one for road, ...)
            Mat nextFeatureVector;
            double[] sample = new double[BB_Test_Train_Util.featureVectorDimension];
            int rowNum = 0;
            int colNum = 0;

            nextFeatureVector = (Mat)imgVector.firstElement();
            for(int i = 0; i < nextFeatureVector.rows(); i++){
                for(int j = 0; j < nextFeatureVector.cols(); j++){
                    sample = nextFeatureVector.get(i, j);

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


                    if(class_index == -1 )
                        count_NO_CLASS += 1;
                    else
                        count_Classes[class_index] += 1;

                    if(oneImage){
                        classArray[rowNum][colNum] = class_index;
                        colNum++;
                        if(colNum%imgWidth == 0){
                            colNum = 0;
                            rowNum++;
                        }
                    }


                    // THIS MEANS THIS SAMPLE BELONGS TO CLASS_INDEX WHAT DO I DO WITH THIS INFORMATION???
                    if(BB_Test_Train_Util.VERBOSE)
                        System.out.println("Sample:" + sample.toString() + "\r\n" + "class = " + class_index + " with probability " + max + "\r\n");
                    if(BB_Test_Train_Util.VERBOSE == true)
                        dos.writeBytes("Sample:" + sample.toString() + "\r\n" + "class = " + class_index + " with probability " + max + "\r\n");
                }
            }

            long endTime   = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.out.println((double)totalTime/1000);

            if(oneImage){
                Mat pseudoImage = createPseudoImage(classArray, gmms);
                Highgui.imwrite(BB_Test_Train_Util.filepath_to_code_source + "pseudoColor.png", pseudoImage);
            }

            dos2.writeBytes("Number of Total Samples = " + featureMat.height() + "\r\n\r\n"  );
            if(BB_Test_Train_Util.VERBOSE)
                System.out.println("Number of Total Samples = " + featureMat.height() + "\r\n\r\n"  );

            dos2.writeBytes("For class index= -1  w/ name= NO_CLASS  #classified = " + count_NO_CLASS + "\r\n\r\n" );
            if(BB_Test_Train_Util.VERBOSE)
                System.out.println("For class index= -1  w/ name= NO_CLASS" + count_NO_CLASS + "\r\n\r\n" );

            for(int g = 0; g < gmms.size(); g++){
                dos2.writeBytes("For class index= " + g + " w/ name= " + gmms.elementAt(g).className + "  # classified = " + count_Classes[g] + "\r\n\r\n" );
                if(BB_Test_Train_Util.VERBOSE)
                    System.out.println("For class index= " + g + " w/ name= " + gmms.elementAt(g).className + "  # classified = " + count_Classes[g] + "\r\n\r\n" );

            }


            dos.close();
            fo.close();
            dos2.flush();
            dos2.close();
            fo2.close();

        }catch(IOException e){}

    }

    static Vector<GMM> readParametersForMixtureOfGaussiansForAllDataClasses(String gmmsfilename){
        Vector<GMM> gmms = new Vector();
        // Step 1: Cycle through each class's file representing its GMM (Gaussian Mixture Model)

        try {
            File f = new File(gmmsfilename);
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);

            FileReader fr2;
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

                // Read in filename for gmm parameters
                gmm.fileName = br.readLine();


                // Step 2: Read in current class's GMM and create an instance of the jMEF.PVectorMatrix for use
                // in the call of MultivariateGaussian.density(*)
                // Also store weight for GMM
                f = new File(gmm.fileName);
                fr2 = new FileReader(f);
                br2 = new BufferedReader(fr2);

                gmm.numCluster = Integer.parseInt(br2.readLine());

                s = br2.readLine();
                s = s.replace(",", "");
                s = s.replace("[",  "");
                s = s.replace("]", "");
                st = new StringTokenizer(s);
                Vector weights = new Vector();

                for(int w = 0; w < gmm.numCluster; w++){
                    weights.add(Double.parseDouble(st.nextToken())); // reading in weights for each cluster
                }
                gmm.weights = weights;
                // Read in means vector for all clusters
                double[] means = new double[BB_Test_Train_Util.featureVectorDimension];
                for(int q=0; q < gmm.numCluster; q++)  //cycle each cluster
                {
                    s = br2.readLine();
                    s = s.replace(",", "");
                    s = s.replace("[",  "");
                    s = s.replace("]", "");
                    s = s.replace(";", "");
                    st = new StringTokenizer(s);
                    for(int w = 0; w < BB_Test_Train_Util.featureVectorDimension; w++){
                        means[w] = (Double.parseDouble(st.nextToken()));
                    }
                }


                // Read in covariance matrix for each cluster
                double[][] covMat = new double[BB_Test_Train_Util.featureVectorDimension][BB_Test_Train_Util.featureVectorDimension];
                for(int q=0; q < gmm.numCluster; q++)  //cycle each cluster
                {
                    for(int z = 0; z < BB_Test_Train_Util.featureVectorDimension; z++) // for each row in this cluster's covariance matrix
                    {
                        s = br2.readLine();
                        s = s.replace(",", "");
                        s = s.replace("[",  "");
                        s = s.replace("]", "");
                        s = s.replace(";", "");
                        st = new StringTokenizer(s);
                        for(int w = 0; w < BB_Test_Train_Util.featureVectorDimension; w++){ // Cycling through each row
                            covMat[z][w] = (Double.parseDouble(st.nextToken()));
                        }
                    }
                }
                // Now store this PVectorMatrix inside the gmm
                gmm.addGaussian(means, covMat);

                // Calculate minimum probability value for each cluster of this gmm to be within +-(2 * the std dev)
                for(int q=0; q < gmm.numCluster; q++)  //cycle each cluster
                {
                    double[][] std = new double[BB_Test_Train_Util.featureVectorDimension][BB_Test_Train_Util.featureVectorDimension];
                    for(int v = 0; v < BB_Test_Train_Util.featureVectorDimension; v++){
                        for(int w = 0; w < BB_Test_Train_Util.featureVectorDimension; w++){
                            std[v][w] = BB_Test_Train_Util.std_dev_range_factor * Math.sqrt(gmm.gaussians.elementAt(q).getCovariances().getData()[v][w]); //TODO: MAKE SURE WORKS
                        }
                    }
                    RealMatrix stdMat = MatrixUtils.createRealMatrix(std);
                    // Calculate minimum  sample value = mean - std to be considered part of this cluster
                    double[] identity = new double[BB_Test_Train_Util.featureVectorDimension];
                    for(int v = 0; v < BB_Test_Train_Util.featureVectorDimension; v++){
                        identity[v] = 1.0;
                    }

                    RealVector min = new ArrayRealVector();
                    RealVector meansMat = new ArrayRealVector(gmm.gaussians.elementAt(q).getMeans());
                    RealVector stdVector = new ArrayRealVector(stdMat.operate(identity));
                    min = meansMat.subtract(stdVector);

                    double temp = gmm.gaussians.elementAt(q).density(gmm.gaussians.elementAt(q).getMeans());
                    if(BB_Test_Train_Util.VERBOSE)
                        System.out.println(" Prob at mean" + temp  );

                    //determine minimum probability value corresponding to this minimum
                    gmm.minimum_probability_to_be_in_cluster.add(gmm.gaussians.elementAt(q).density(min.toArray()));

                    if(BB_Test_Train_Util.VERBOSE)
                        System.out.println(" Prob at 2 std out is " + gmm.minimum_probability_to_be_in_cluster.elementAt(q) );

                }

                //add created gmm to the ggms vector
                gmms.add(gmm);
                br2.close();
                fr2.close();
            }
            br.close();
            fr.close();

            return gmms;





        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }




    }

    /**
     * This method takes as input a matrix of same dimensions as original image
     * that contains the class label of each pixel and returns a pseudo-color
     * image.
     * @param classArray, gmms
     */
    static Mat createPseudoImage(int[][] classArray, Vector<GMM> gmms){
        int imgWidth = classArray.length;
        int imgHeight = classArray[1].length;

        Mat pseudoImage = new Mat(imgWidth, imgHeight, CvType.CV_8UC3);

        for(int i = 0; i < imgWidth; i++){
            for(int j = 0; j < imgHeight; j++){
                if(classArray[i][j] == -1){
                    pseudoImage.put(i, j, BB_Test_Train_Util.otherClassPseudocolor);
                }
                else
                    pseudoImage.put(i, j, gmms.elementAt(classArray[i][j]).pseudocolor); // Note: each gmm in the gmm vector has associated with it a data class gmm.className
            }
        }

        return pseudoImage;
    }
}

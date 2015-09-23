package edu.csueb.ilab.blindbike.roadfollowing;

import java.util.Vector;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

/**
 * Created by Will on 9/21/2015.
 */
public class GMM {
    // This vector will store all the gaussians in the gaussian mixture model
    Vector<MultivariateNormalDistribution> gaussians = new Vector<MultivariateNormalDistribution>();

    // Number of clusters
    int numCluster;

    // Name of corresponding data class this gmm is modeling
    String className;

    // Pseudocolor used to represent this class
    double pseudocolor[] = new double[3];

    // Name of gmm data file containing gmm parameters
    String fileName;

    // Vector representing a weight of a gaussian
    Vector weights = new Vector();

    // Vector of minimum probability to be within - 2 * std dev of mean
    Vector minimum_probability_to_be_in_cluster = new Vector();

    // Using the density method of the multivariate gaussian class to calculate for each cluster the probability
    // multiplied by its weight and sum them up to return the probability the sample belongs to this gmm
    double predict(double[] sample){
        double probability = 0.0;
        double temp;

        //variable telling us that at least ONE cluster yields a probability > minimum_probability_to_be_in_cluster
        boolean atLeastOneClusterAbove_MinProb = false;

        // Cycle through each cluster of GMM
        for(int i = 0; i < numCluster; i++){

            temp = gaussians.elementAt(i).density(sample);
            probability += (double)weights.elementAt(i) * temp;
            if (temp > (double) this.minimum_probability_to_be_in_cluster.elementAt(i))
            {
                atLeastOneClusterAbove_MinProb = true;
            }

        }

        if(probability < 0){
            System.out.println("Warning: Probability less than 0 in GMM.predict");
        }
        else if(atLeastOneClusterAbove_MinProb == false)
            probability = -100.0; //never a possible probability
        return probability;
    }

    public void addGaussian(double[] means, double[][] covarianceMat)
    {
        MultivariateNormalDistribution toAdd = new MultivariateNormalDistribution(means, covarianceMat);
        this.gaussians.add(toAdd);
    }
}

package edu.csueb.ilab.blindbike.roadfollowing;

import org.opencv.core.Mat;

/**
 * Created by williamoverell on 5/4/16.
 */
public class Utils {

    /**
     * Calculate the gradient of the specified pixel location
     * returns theta from -pi/2 to pi/2
     * and magnitude
     * if a border pixel returns value of 200
     * @param binary_image
     * @param x
     * @param y
     * @param neighborhoodSize
     * @return orientationAndMagnitude - double[2] 0 - orientation, 1 - magnitude
     */
    static double[] calculateGradientOrientationAndMagnitude(Mat binary_image, int x, int y, int neighborhoodSize){
        int halfNeighborhood = (int)Math.floor(neighborhoodSize / 2);
        double dx = 0, dy = 0;
        double[] tempPixelValue;
        double[] orientationAndMagnitude = new double[2];

        // Check if pixel is an edge pixel
        if(x - halfNeighborhood <= 0 || x + halfNeighborhood >= binary_image.cols() || y - halfNeighborhood <= 0 || y + halfNeighborhood >= binary_image.rows()){
            // Return 0 magnitude and out of bounds angle
            orientationAndMagnitude[0] = 200;
            orientationAndMagnitude[1] = 0;
            return orientationAndMagnitude;
        }else {

            // Loop through neighborhood
            for (int row = y - halfNeighborhood; row <= y + halfNeighborhood; row++) {
                for (int col = x - halfNeighborhood; col <= x + halfNeighborhood; col++) {
                    // Calculate DX
                    if (col > x) { // if in the top half rows of the neighborhood
                        tempPixelValue = binary_image.get(row, col);
                        dx += tempPixelValue[0]; // add the pixel value
                    }
                    else if (col < x) { // if in the bottom half rows of the neighborhood
                        tempPixelValue = binary_image.get(row, col);
                        dx -= tempPixelValue[0]; // subtract the pixel value
                    }

                    // Calculate DY
                    if (row > y) { // if in the top half rows of the neighborhood
                        tempPixelValue = binary_image.get(row, col);
                        dy += tempPixelValue[0]; // add the pixel value
                    }
                    else if (row < y) { // if in the bottom half rows of the neighborhood
                        tempPixelValue = binary_image.get(row, col);
                        dy -= tempPixelValue[0]; // subtract the pixel value
                    }
                }
            }
        }

        // Calculate Magnitude
        orientationAndMagnitude[1] = Math.sqrt((dx * dx) + (dy * dy));

        // Calculate Orientation

        // Check if dx or dy is too small to be an edge, if so return an out of bounds value
        // If dx is 0 then it is a vertical line return 0, remember polar so a 0 degree theta is a vertical line
        if(Math.abs(dx) < 1 && Math.abs(dy) < 1) { // WILL: what value to use??
            orientationAndMagnitude[0] = 200; // out of bounds value
            orientationAndMagnitude[1] = 0;
        }
        else if(dx == 0)
            orientationAndMagnitude[0] = Math.PI / 2;
        else
            orientationAndMagnitude[0] = Math.atan(-dy/dx); // otherwise calculate the angle

        return orientationAndMagnitude;
    }

    /**
     * Calculate the color gradient magnitude of the specified pixel location
     * returns theta from -pi/2 to pi/2
     * if a border pixel returns value of 200
     * @param rgba_image
     * @param x
     * @param y
     * @param neighborhoodSize
     * @return orientationAndMagnitude - double[2] where index 0 is gradient, index 1 is magnitude
     */
    static double[] calculateColorGradientOrientationAndMagnitude(Mat rgba_image, int x, int y, int neighborhoodSize){
        int halfNeighborhood = (int)Math.floor(neighborhoodSize / 2);
        double dx_red = 0, dy_red = 0;
        double dx_green = 0, dy_green = 0;
        double dx_blue = 0, dy_blue = 0;
        double mag_red, mag_green, mag_blue;
        double grad_red, grad_green, grad_blue;
        double[] orientationAndMagnitude = new double[2];
        double[] tempPixelValue;

        // Check if pixel is an edge pixel
        if(x - halfNeighborhood <= 0 || x + halfNeighborhood >= rgba_image.cols() || y - halfNeighborhood <= 0 || y + halfNeighborhood >= rgba_image.rows()){
            // Do what?
            // Give out of bounds orientation and 0 magnitude
            orientationAndMagnitude[0] = 200;
            orientationAndMagnitude[1] = 0;
            return orientationAndMagnitude;
        }else {

            // Loop through neighborhood
            for (int row = y - halfNeighborhood; row <= y + halfNeighborhood; row++) {
                for (int col = x - halfNeighborhood; col <= x + halfNeighborhood; col++) {
                    // Calculate DX
                    if (col > x) { // if in the top half rows of the neighborhood
                        tempPixelValue = rgba_image.get(row, col);
                        dx_red += tempPixelValue[0]; // add the pixel value for red
                        dx_green += tempPixelValue[1]; // add the pixel value for green
                        dx_blue += tempPixelValue[2]; // add the pixel value for blue
                    } else if (col < x) { // if in the bottom half rows of the neighborhood
                        tempPixelValue = rgba_image.get(row, col);
                        dx_red -= tempPixelValue[0]; // subtract the pixel value for red
                        dx_green -= tempPixelValue[1]; // subtract the pixel value for green
                        dx_blue -= tempPixelValue[2]; // subtract the pixel value for blue
                    }

                    // Calculate DY
                    if (row > y) { // if in the top half row of the neighborhood
                        tempPixelValue = rgba_image.get(row, col);
                        dy_red += tempPixelValue[0]; // add the pixel value for red
                        dy_green += tempPixelValue[1]; // add the pixel value for green
                        dy_blue += tempPixelValue[2]; // add the pixel value for blue
                    } else if (row < y) { // if in the bottom half row of the neighborhood
                        tempPixelValue = rgba_image.get(row, col);
                        dy_red -= tempPixelValue[0]; // subtract the pixel value for red
                        dy_green -= tempPixelValue[1]; // subtract the pixel value for green
                        dy_blue -= tempPixelValue[2]; // subtract the pixel value for blue
                    }
                }
            }


            // Calculate magnitudes
            mag_red = Math.sqrt((dx_red * dx_red) + (dy_red * dy_red));
            mag_green = Math.sqrt((dx_green * dx_green) + (dy_green * dy_green));
            mag_blue = Math.sqrt((dx_blue * dx_blue) + (dy_blue * dy_blue));

            orientationAndMagnitude[1] = (mag_red + mag_green + mag_blue); // Sum magnitudes for whole magnitude

            // Calculate gradients
            if(dx_red == 0)
                grad_red = Math.PI / 2;
            else
                grad_red = Math.atan(-dy_red / dx_red);

            if(dx_green == 0)
                grad_green = Math.PI / 2;
            else
                grad_green = Math.atan(-dy_green / dx_green);

            if(dx_blue == 0)
                grad_blue = Math.PI / 2;
            else
                grad_blue = Math.atan(-dy_blue / dx_blue);

            orientationAndMagnitude[0] = (mag_red * grad_red + mag_green * grad_green + mag_blue * grad_blue)/(mag_red+mag_green+mag_blue);

            return orientationAndMagnitude;
        }
    }
}

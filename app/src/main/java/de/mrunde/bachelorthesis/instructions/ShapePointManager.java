package de.mrunde.bachelorthesis.instructions;

import com.mapquest.android.maps.GeoPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.mrunde.bachelorthesis.basics.RouteSegment;

/**
 * Created by Admin on 3/23/2016.
 */
public class ShapePointManager {

    /**
     * if the JSON extraction is succesful this will be set to true
     * else it will be set to false
     */
    public boolean isImportSuccessful;

    /**
     * Constructor for ShapePointManager
     * Takes JSONObject as parameter and pulls out the shape points
     * @param json
     */
    public ShapePointManager(JSONObject json){
        this.isImportSuccessful = true;
        try{
            JSONObject guidance = json.getJSONObject("guidance");

            JSONArray shapePointCollection = guidance.getJSONArray("shapePoints");

            double shapePoints[][] = new double[shapePointCollection.length()/2][2];

            for(int i = 0; i < shapePointCollection.length(); i = i + 2){
                shapePoints[i/2][0] = shapePointCollection.getDouble(i);
                shapePoints[i/2][1] = shapePointCollection.getDouble(i+1);
            }

        }catch(JSONException e){
            this.isImportSuccessful = false;
        }

    }

    public Boolean isImportSuccessful(){
        return true;
    }

   /* public List<RouteSegment> getSegmentswithShapePoints(double currentSPLat, double currentSPLng,List<RouteSegment> segmentscopy,GeoPoint[] decisionPoints){

        double startptLat,startptLng,endptLat,endptLng;
        GeoPoint[] segmentShapePoints;
        List<storeSegments> completeSegment = new ArrayList<storeSegments>();
        //Array through each Segment for start and end point
        for(int i=0; i<segmentscopy.size();i++)
        {
            if(segmentscopy.get(i).getStartPoint()==null)
            {
                //Search for currentSPLat and currentSPLng in decisionPoints
                startptLat = currentSPLat;
                startptLng = currentSPLng;
            }
            else
            {
                //Fetch the start point from the segmentscopy
                startptLat = segmentscopy.get(i).getStartPoint().getLatitude();
                startptLng = segmentscopy.get(i).getEndPoint().getLongitude();
            }
            //The end points for either will be fetched
            endptLat = segmentscopy.get(i).getEndPoint().getLatitude();
            endptLng = segmentscopy.get(i).getEndPoint().getLongitude();

            //Array through the decisionPoints array and copy all the points into a new array

            int startindex=0;

            //For now I am assuming it is 10 untill I figure out a way to have dynamic value for the array size
            segmentShapePoints = new GeoPoint[50];
            for(int j=0;j<decisionPoints.length;j++)
            {
                if(decisionPoints[j].getLatitude() == startptLat && decisionPoints[j].getLongitude() == startptLng)
                {
                    startindex = j;
                    break;
                }
            }
            int si=0;
            //Array through decisionPoints looking for endpoint and putting
            for(int j=startindex;j<=decisionPoints.length-startindex;j++)
            {

                if(decisionPoints[j].getLatitude() == endptLat && decisionPoints[j].getLongitude() == endptLng)
                {
                    segmentShapePoints[si] = decisionPoints[j];
                    break;
                }
                else {
                    segmentShapePoints[si] = decisionPoints[j];
                    si++;
                }
            }
     //       completeSegment.add(i,segmentscopy);
     //       completeSegment.add(i,segmentShapePoints);
            storeSegments ss = new storeSegments(segmentscopy,segmentShapePoints);
            completeSegment.add(i,ss);

        }


        return null;
    }

    class storeSegments{

        private List<RouteSegment> segments;
        private GeoPoint[] segmentPoints;

        public storeSegments(List<RouteSegment> segments, GeoPoint[] segmentPoints){
            this.segments = segments;
            this.segmentPoints = segmentPoints;
        }

        public List<RouteSegment> getSegments(){
            return segments;
        }

        public GeoPoint[] getSegmentPoints(){
            return segmentPoints;
        }
    }*/

}

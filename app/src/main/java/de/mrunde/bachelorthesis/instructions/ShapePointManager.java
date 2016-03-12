package de.mrunde.bachelorthesis.instructions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by williamoverell on 3/9/16.
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
}

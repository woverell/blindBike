package edu.csueb.ilab.blindbike.lightdetection;

import com.mapquest.android.maps.GeoPoint;

/**
 * Created by Admin on 10/28/2015.
 */
public class Traffic_light_Data {
    private GeoPoint center;
    private double lat;
    private double lng;
    private String category;
    private double altitude;
    private Boolean crossed;
    //If manTypeand linkId is -1 then we havn't found anything or no value was added
    private int manType=-1;
    private int linkId=-1;

    public Traffic_light_Data()
    {
        category="";
        altitude=0;
        crossed=false;
    }

    /*public GeoPoint getCenter()
    {
        return center;
    }*/

  /*  public void setCenter(GeoPoint center)
    {
        this.center = center;
    }*/

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public double getAltitude()
    {
        return altitude;
    }

    public void setAltitude(double altitude)
    {
        this.altitude = altitude;
    }

    public boolean getCrossed()
    {
        return crossed;
    }

    public void setCrossed(Boolean crossed)
    {
        this.crossed = crossed;
    }

    public void setLat(double lat)
    {
        this.lat = lat;
    }
    public double getLat()
    {
        return lat;
    }

    public void setLng(double lng)
    {
        this.lng=lng;
    }

    public double getLng()
    {
        return lng;
    }

    public void setManType(int manType)
    {
        this.manType = manType;
    }

    public int getManType()
    {
        return manType;
    }

    public void setLinkId(int linkId)
    {
        this.linkId = linkId;
    }

    public int getLinkId()
    {
        return linkId;
    }
}

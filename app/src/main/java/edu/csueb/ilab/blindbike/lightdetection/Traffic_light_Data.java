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

    public Traffic_light_Data()
    {
        category="";
        altitude=0;
        crossed=false;
    }

    public GeoPoint getCenter()
    {
        return center;
    }

    public void setCenter(GeoPoint center)
    {
        this.center = center;
    }

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
}

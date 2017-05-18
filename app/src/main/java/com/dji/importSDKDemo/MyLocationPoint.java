package com.dji.importSDKDemo;

import com.amap.api.maps2d.model.LatLng;

import dji.common.mission.waypoint.Waypoint;

/**
 * Created by FQ on 17/4/19.
 */

public class MyLocationPoint {
    private double Latitude;
    private double Longitude;
    private double Altitude;

    public MyLocationPoint(double lat, double lng, double alt) {
        setLatitude(lat);
        setLongitude(lng);
        setAltitude(alt);
    }

    public double getAltitude() {
        return Altitude;
    }

    public double getLatitude() {
        return Latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setAltitude(double altitude) {
        Altitude = altitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }

    public LatLng getPoint() {
        return new LatLng(getLatitude(), getLongitude());
    }

    public Waypoint getWaypoint() {
        return new Waypoint(getLatitude(), getLongitude(), (float) getAltitude());
    }

    public LatLng getlatLng() {
        return new LatLng(getLatitude(), getLongitude());
    }

    @Override
    public String toString() {
        return "(" + getLatitude() + "," + getLongitude() + "," + getAltitude() + ")";
    }
}

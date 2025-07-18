package com.example.medialert.data;

public class PlaceItem {
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private double distanceInKm;

    public PlaceItem(String name, String address, double latitude, double longitude, double distanceInKm) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceInKm = distanceInKm;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistanceInKm() {
        return distanceInKm;
    }

}

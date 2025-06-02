package com.example.foodorderappcustomer.API;

public class PlaceDetailResponse {
    private Result result;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public static class Result {

        private String formatted_address;
        private Geometry geometry;

       private String place_id ;

        public void setPlace_id(String place_id) {
            this.place_id = place_id;
        }

        public String getPlace_id() {
            return place_id;
        }

        public String getFormattedAddress() {
            return formatted_address;
        }

        public void setFormattedAddress(String formatted_address) {
            this.formatted_address = formatted_address;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public void setGeometry(Geometry geometry) {
            this.geometry = geometry;
        }
    }



    public static class Geometry {
        private Location location;

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }
    public static class Location {
        private double latitude; // Latitude as a double
        private double longitude; // Longitude as a double

        public double getLatitude() {
            return latitude;
        }

        public void setLat(double lat) {
            this.latitude = lat;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double lng) {
            this.longitude = lng;
        }
    }

}

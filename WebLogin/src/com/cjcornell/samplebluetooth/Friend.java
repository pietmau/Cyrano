/**
 * CLASS: Friend
 *   This class represents a friend of a user of Cyrano
 */

package com.cjcornell.samplebluetooth;

import com.cjcornell.samplebluetooth.data.AppSettings;

public class Friend {
    /** Attributes */
    private String id;
    private String firstname;
    private String lastname;
    private String email;
    private double distance;
    private double latitude;
    private double longitude;
    private String details1;
    private String details2;
    private String details3;
    
    /** Constructors */
    public Friend(String id, String firstname, String lastname, String email, double distance, double latitude, double longitude,
            String details1, String details2, String details3) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.setDistance(distance);
        this.setLatitude(latitude);
        this.setLongitude(longitude);
        this.details1 = details1;
        this.details2 = details2;
        this.details3 = details3;
    }

    public Friend(String id, String firstname, String lastname, String email) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
    }
    
    /** Getters */
    public String getId() {
        return id;
    }
    public String getName() {
        return firstname + " " + lastname;
    }
    public String getFirstName() {
        return firstname;
    }
    
    public String getEmail() {
        return email;
    }
    
    public double getDistance() {
        return distance;
    }
    
    public String getDistanceString() {
        return AppSettings.formatter.format(distance) + " meters away";
    }

    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }

    public String getDetails1() {
        return details1 == null || "null".equalsIgnoreCase(details1) ? "" : details1;
    }

    public String getDetails2() {
        return details2 == null || "null".equalsIgnoreCase(details2) ?  "" : details2;
    }
    

    public String getDetails3() {
        return details3 == null || "null".equalsIgnoreCase(details3) ? "" : details3;
    }
    
    /** Setters */
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public void setDetails1(String details1) {
        this.details1 = details1;
    }
    
    public void setDetails2(String details2) {
        this.details2 = details2;
    }
    
    public void setDetails3(String details3) {
        this.details3 = details3;
    }
    
    /** 
     * toString
     *   The toString method will return the friend's name and the distance from the user in parentheses
     */
    public String toString() {
        return getName() + " (" + getDistanceString() + ")";
    }
}

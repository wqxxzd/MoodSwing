package com.example.moodswing.customDataTypes;


import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * This Class is the blueprint for a moodEvent object
 */
public class MoodEvent {
    private String uniqueID; // this value is used to identify moodEvent
    private Long timeStamp;
    // required fields
    private int moodType; // for this design im assuming each moodType has been assigned an unique int
    private DateJar date;
    private TimeJar time;

    // optional fields
    private String reason;
    private String socialSituation;
    private Double Latitude;
    private Double Longitude;

    public MoodEvent() {
    }

    public Long getTimeStamp(){
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }

    public int getMoodType() {
        return moodType;
    }

    public void setMoodType(int moodType) {
        this.moodType = moodType;
    }

    public DateJar getDate() {
        return date;
    }

    public void setDate(DateJar date) {
        this.date = date;
    }

    public TimeJar getTime() {
        return time;
    }

    public void setTime(TimeJar time) {
        this.time = time;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setSocialSituation(String socialSituation) {this.socialSituation = socialSituation;}

    public String getSocialSituation() {
        return socialSituation;
    }

    public Double getLatitude() {
        return Latitude;
    }

    public Double getLongitude() {
        return Longitude;
    }

    public void setLatitude(Double latitude) {
        Latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        Longitude = longitude;
    }
}
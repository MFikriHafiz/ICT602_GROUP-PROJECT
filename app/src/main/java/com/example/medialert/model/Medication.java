package com.example.medialert.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Medication implements Parcelable {
    private String id;
    private String name;
    private double dosageQuantity;
    private String dosageUnit;
    private String instructions;
    private List<Long> alarmTimes; // Stored as milliseconds representing time of day
    private boolean isActive; // To determine if alarms should be active
    private String type; // e.g., Pill, Liquid
    private String frequency; // e.g., Daily, Weekly
    private long startDate; // Timestamp in milliseconds
    private long endDate; // Timestamp in milliseconds, 0 if ongoing
    private String imageUrl; // URL for medication image

    public Medication() {
        // Default constructor required for Firebase deserialization
        this.alarmTimes = new ArrayList<>(); // Initialize to avoid null
        this.isActive = true; // Default to active
        this.startDate = System.currentTimeMillis(); // Default to now for new medications
        this.endDate = 0; // Default to 0, indicating no specific end date
        this.imageUrl = null; // Default to null
    }

    // Full constructor for creating new Medications (ID usually set after Firebase adds it)
    public Medication(String name, double dosageQuantity, String dosageUnit, String instructions,
                      List<Long> alarmTimes, boolean isActive, String type, String frequency,
                      long startDate, long endDate, String imageUrl) {
        this.name = name;
        this.dosageQuantity = dosageQuantity;
        this.dosageUnit = dosageUnit;
        this.instructions = instructions;
        this.alarmTimes = alarmTimes != null ? alarmTimes : new ArrayList<>();
        this.isActive = isActive;
        this.type = type;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.imageUrl = imageUrl;
    }

    // --- Parcelable Implementation ---

    // Constructor used when re-creating a Medication object from a Parcel
    protected Medication(Parcel in) {
        id = in.readString();
        name = in.readString();
        dosageQuantity = in.readDouble();
        dosageUnit = in.readString();
        instructions = in.readString();
        // Read the list, providing the ClassLoader for correct deserialization
        alarmTimes = new ArrayList<>();
        in.readList(alarmTimes, Long.class.getClassLoader());
        isActive = in.readByte() != 0; // Read boolean as byte
        type = in.readString();
        frequency = in.readString();
        startDate = in.readLong();
        endDate = in.readLong();
        imageUrl = in.readString();
    }

    // This CREATOR field is required for Parcelable implementation.
    // It's used to generate instances of your Parcelable class from a Parcel.
    public static final Creator<Medication> CREATOR = new Creator<Medication>() {
        @Override
        public Medication createFromParcel(Parcel in) {
            return new Medication(in);
        }

        @Override
        public Medication[] newArray(int size) {
            return new Medication[size];
        }
    };

    @Override
    public int describeContents() {
        return 0; // No special contents (like FileDescriptors)
    }

    // Method to write the object's data to the Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeDouble(dosageQuantity);
        dest.writeString(dosageUnit);
        dest.writeString(instructions);
        dest.writeList(alarmTimes); // Write the List<Long>
        dest.writeByte((byte) (isActive ? 1 : 0)); // Write boolean as byte (1 for true, 0 for false)
        dest.writeString(type);
        dest.writeString(frequency);
        dest.writeLong(startDate);
        dest.writeLong(endDate);
        dest.writeString(imageUrl);
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDosageQuantity() {
        return dosageQuantity;
    }

    public void setDosageQuantity(double dosageQuantity) {
        this.dosageQuantity = dosageQuantity;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(String dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public List<Long> getAlarmTimes() {
        return alarmTimes;
    }

    public void setAlarmTimes(List<Long> alarmTimes) {
        this.alarmTimes = alarmTimes != null ? alarmTimes : new ArrayList<>();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "Medication{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", dosageQuantity=" + dosageQuantity +
                ", dosageUnit='" + dosageUnit + '\'' +
                ", instructions='" + instructions + '\'' +
                ", alarmTimes=" + alarmTimes +
                ", isActive=" + isActive +
                ", type='" + type + '\'' +
                ", frequency='" + frequency + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
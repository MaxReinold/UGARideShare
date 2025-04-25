package edu.uga.cs.ugarideshare;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class Ride {
    private String id;
    private RideType type;
    private LocalDateTime dateTime;
    private String from;
    private String to;
    private String driverEmail;
    private String riderEmail;
    private RideStatus status;
    private int pointsCost;

    public Ride(RideType type, LocalDateTime dateTime, String from, String to) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.dateTime = dateTime;
        this.from = from;
        this.to = to;
        this.status = RideStatus.PENDING;
        this.pointsCost = 0;
    }

    // Getters and setters for all fields
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RideType getType() {
        return type;
    }

    public void setType(RideType type) {
        this.type = type;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getDriverEmail() {
        return driverEmail;
    }

    public void setDriverEmail(String driverEmail) {
        this.driverEmail = driverEmail;
    }

    public String getRiderEmail() {
        return riderEmail;
    }

    public void setRiderEmail(String riderEmail) {
        this.riderEmail = riderEmail;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public int getPointsCost() {
        return pointsCost;
    }

    public void setPointsCost(int pointsCost) {
        this.pointsCost = pointsCost;
    }
}

enum RideType {
    OFFER, REQUEST
}

enum RideStatus {
    PENDING, ACCEPTED, COMPLETED
}

class User {
    private String email;
    private int points;
    private List<Ride> acceptedRides;
    private List<Ride> historyRides;

    public User(String email) {
        this.email = email;
        this.points = 0;
        this.acceptedRides = new ArrayList<>();
        this.historyRides = new ArrayList<>();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public List<Ride> getAcceptedRides() {
        return acceptedRides;
    }

    public void setAcceptedRides(List<Ride> acceptedRides) {
        this.acceptedRides = acceptedRides;
    }

    public List<Ride> getHistoryRides() {
        return historyRides;
    }

    public void setHistoryRides(List<Ride> historyRides) {
        this.historyRides = historyRides;
    }
}
package edu.uga.cs.ugarideshare;

import java.io.Serializable;
import java.util.Date;

public class Ride implements Serializable {
    public Date date;
    public String addressTo;
    public String addressFrom;
    public User userDriver;
    public User userRider;

    /**
     * Default constructor.
     */
    public Ride() {}

    /**
     * Constructs a Ride object.
     * @param date The date of the ride.
     * @param addressTo The destination address.
     * @param addressFrom The starting address.
     * @param userDriver The driver user.
     * @param userRider The rider user.
     */
    public Ride(Date date, String addressTo, String addressFrom, User userDriver, User userRider) {
        this.date = date;
        this.addressTo = addressTo;
        this.addressFrom = addressFrom;
        this.userDriver = userDriver;
        this.userRider = userRider;
    }

    /**
     * Returns a string representation of the Ride.
     * @return String representation.
     */
    @Override
    public String toString() {
        return "Ride{" +
                "date=" + date +
                ", addressTo='" + addressTo + '\'' +
                ", addressFrom='" + addressFrom + '\'' +
                ", userDriver=" + userDriver +
                ", userRider=" + userRider +
                '}';
    }
}


package com.entities;

import javax.persistence.Entity;

@Entity
public class Event {
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;

    public Event() {
    }

    public Event(String input) {
        String[] temp = input.split(",");
        this.startDate = temp[0].split("T")[0];
        this.endDate = temp[1].split("T")[0];
        this.startTime = temp[0].split("T")[1].substring(0, 5).replace("T", "");
        this.endTime = temp[1].split("T")[1].substring(0, 5).replace("T", "");
    }

    @Override
    public String toString() {
        return String.format("%s %s -> %s %s", startDate, startTime, endDate, endTime);
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}

package com.entities;


import com.google.api.services.calendar.Calendar;

import javax.persistence.Entity;
import java.util.List;

@Entity
public class User {
    private String name;
    private List<Integer> groups;
    private String pw;
    private String mail;
    private List<Event> events;

    public User() {
    }

    public User(String name, String pw) {
        this.name = name;
        this.pw = pw;
    }

    public User(String name, List<Integer> groups, String pw, String mail, List<Event> events) {
        this.name = name;
        this.groups = groups;
        this.pw = pw;
        this.mail = mail;
        this.events = events;
    }


    @Override
    public String toString() {
        String toString = String.format("%s,%s\n%s",name,mail,events.toString());
        return toString;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getGroups() {
        return groups;
    }

    public void addGroup(int groupID) {
        this.groups.add(groupID);
    }

    public void removeGroup(int groupID) {
        this.groups.remove((Integer) groupID);
    }

    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }
}

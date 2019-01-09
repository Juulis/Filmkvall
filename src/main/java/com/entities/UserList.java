package com.entities;

import javax.persistence.Entity;

@Entity
public class UserList {
    private User[] users;

    public UserList() {
    }

    public User[] getUsers() {
        return users;
    }
}

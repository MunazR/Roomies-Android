package com.munaz.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model for User object
 */
public class User {
    public String id;
    public String firstName;
    public String lastName;
    public String displayName;
    public String profilePictureUrl;

    public User(String id, String firstName, String lastName, String displayName, String profilePictureUrl) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.profilePictureUrl = profilePictureUrl;
    }

    public User(JSONObject user) throws JSONException {
        this.id = user.getString("_id");
        this.firstName = user.getString("firstName");
        this.lastName = user.getString("lastName");
        this.displayName = user.getString("displayName");
        this.profilePictureUrl = user.getString("profilePictureUrl");
    }
}

package com.munaz.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for GroupObject
 */
public class Group {
    public String id;
    public User owner;
    public List<User> members;

    public Group(String id, User owner, List<User> members) {
        this.id = id;
        this.owner = owner;
        this.members = members;
    }

    public Group(JSONObject group) throws JSONException {
        this.id = group.getString("_id");
        this.owner = new User(group.getJSONObject("owner"));

        this.members = new ArrayList<>();
        JSONArray members = group.getJSONArray("members");

        for (int i = 0; i < members.length(); i++) {
            this.members.add(new User(members.getJSONObject(i)));
        }
    }
}

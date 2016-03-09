package com.munaz.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model for Chore Object
 */
public class Chore {
    public String id;
    public String title;
    public String assignedTo;

    public Chore(String id, String title, String assignedTo) {
        this.id = id;
        this.title = title;
        this.assignedTo = assignedTo;
    }

    public Chore(JSONObject chore) throws JSONException {
        this.id = chore.getString("_id");
        this.title = chore.getString("title");
        this.assignedTo = chore.getString("assignedTo");
    }
}

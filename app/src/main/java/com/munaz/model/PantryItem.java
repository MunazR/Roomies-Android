package com.munaz.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model for Pantry Item Object
 */
public class PantryItem {
    public String id;
    public String title;

    public PantryItem(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public PantryItem(JSONObject pantryItem) throws JSONException {
        this.id = pantryItem.getString("_id");
        this.title = pantryItem.getString("title");
    }
}

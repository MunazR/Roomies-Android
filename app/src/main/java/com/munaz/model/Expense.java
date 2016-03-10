package com.munaz.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model for Expense object
 */
public class Expense {
    public String id;
    public String title;
    public int amount;
    public String expensedBy;

    public Expense(String id, String title, int amount, String expensedBy) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.expensedBy = expensedBy;
    }

    public Expense(JSONObject expense) throws JSONException {
        this.id = expense.getString("_id");
        this.title = expense.getString("title");
        this.amount = expense.getInt("amount");
        this.expensedBy = expense.getString("expensedBy");
    }
}

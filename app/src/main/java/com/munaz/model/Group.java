package com.munaz.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for Group object
 */
public class Group {
    public String id;
    public User owner;
    public List<User> members;
    public List<User> invited;
    public List<Chore> chores;
    public List<Expense> expenses;

    public Group(String id, User owner, List<User> members, List<User> invited, List<Chore> chores, List<Expense> expenses) {
        this.id = id;
        this.owner = owner;
        this.members = members;
        this.invited = invited;
        this.chores = chores;
        this.expenses = expenses;
    }

    public Group(JSONObject group) throws JSONException {
        this.id = group.getString("_id");
        this.owner = new User(group.getJSONObject("owner"));

        this.members = new ArrayList<>();
        JSONArray members = group.getJSONArray("members");

        for (int i = 0; i < members.length(); i++) {
            this.members.add(new User(members.getJSONObject(i)));
        }

        this.invited = new ArrayList<>();
        JSONArray invited = group.getJSONArray("invited");

        for (int i = 0; i < invited.length(); i++) {
            this.invited.add(new User(invited.getJSONObject(i)));
        }

        this.chores = new ArrayList<>();
        JSONArray chores = group.getJSONArray("chores");

        for (int i = 0; i < chores.length(); i++) {
            this.chores.add(new Chore(chores.getJSONObject(i)));
        }

        this.expenses = new ArrayList<>();
        JSONArray expenses = group.getJSONArray("expenses");

        for (int i = 0; i < expenses.length(); i++) {
            this.expenses.add(new Expense(expenses.getJSONObject(i)));
        }
    }
}

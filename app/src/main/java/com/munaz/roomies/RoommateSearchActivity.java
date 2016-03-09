package com.munaz.roomies;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.Profile;
import com.munaz.api.Server;
import com.munaz.model.User;
import com.munaz.roomies.adapters.RoommateArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RoommateSearchActivity extends AppCompatActivity {
    private final String TAG = "RoommateSearchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roommate_search);
        handleIntent(getIntent());

        Button backButton = (Button) findViewById(R.id.back_button);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
        }
    }

    private void doSearch(String query) {
        final ProgressDialog dialog = new ProgressDialog(this); // this = YourActivity
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Searching. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        String baseUrl = getString(R.string.base_url);
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", Profile.getCurrentProfile().getId());
            reqBody.put("name", query);
        } catch (JSONException e) {
            dialog.hide();
            handleError(e);
            return;
        }

        JsonObjectRequest userSearchRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.USERS_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dialog.hide();
                if (response.has("users")) {
                    try {
                        JSONArray usersJSON = response.getJSONArray("users");
                        final User[] users = new User[usersJSON.length()];

                        for (int i = 0; i < usersJSON.length(); i++) {
                            users[i] = new User(usersJSON.getJSONObject(i));
                        }

                        String[] displayNames = new String[users.length];

                        for (int i = 0; i < users.length; i++) {
                            displayNames[i] = users[i].displayName;
                        }

                        ListView userListView = (ListView) findViewById(R.id.users);
                        userListView.setAdapter(new RoommateArrayAdapter(getApplicationContext(), users, displayNames));

                        userListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                User selectedUser = users[position];
                                inviteUser(getApplicationContext(), selectedUser);
                            }
                        });
                    } catch (JSONException e) {
                        handleError(e);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No users found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.hide();
                handleError(error);
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(userSearchRequest);
    }

    private void inviteUser(Context context, User user) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Inviting user. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        String baseUrl = context.getString(R.string.base_url);
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", Profile.getCurrentProfile().getId());
            reqBody.put("invitedId", user.id);
        } catch (JSONException e) {
            handleError(e);
            return;
        }

        JsonObjectRequest createGroupRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.GROUP_INVITE_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dialog.hide();
                Toast.makeText(getApplicationContext(), "User invited", Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.hide();
                handleError(error);
            }
        });

        Server.getInstance(context).addToRequestQueue(createGroupRequest);
    }

    private void handleError(Exception error) {
        Log.e(TAG, error.getMessage(), error);
        Toast.makeText(getApplicationContext(), "An unexpected error occurred getting your roommates", Toast.LENGTH_SHORT).show();
    }
}

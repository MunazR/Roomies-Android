package com.munaz.roomies;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.Profile;
import com.munaz.api.Server;
import com.munaz.db.Db;
import com.munaz.model.Group;
import com.munaz.model.User;
import com.munaz.util.AppUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Register buttons
        final Button createGroupButton = (Button) findViewById(R.id.create_group);
        createGroupButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createGroup(v.getContext());
            }
        });

        final Button leaveGroupButton = (Button) findViewById(R.id.leave_group);
        leaveGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Group group = Db.getInstance(getApplicationContext()).getGroup();
                if (group.owner.id.equals(Profile.getCurrentProfile().getId())) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    deleteGroup();
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.leave_group_owner).setPositiveButton(R.string.yes, dialogClickListener)
                            .setNegativeButton(R.string.no, dialogClickListener).show();
                }
            }
        });

        refreshGroup(getApplicationContext());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            refreshGroup(getApplicationContext());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.roommates) {
            // Handle the room mates action
        } else if (id == R.id.chores) {
            // Handle the chores action
        } else if (id == R.id.expenses) {
            // Handle the expenses action
        } else if (id == R.id.pantry) {
            // Handle the pantry action
        } else if (id == R.id.nav_manage) {
            // Handle the settings action
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void refreshGroup(final Context context) {
        // Check if network is available
        if (!AppUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show();
            Group group = Db.getInstance(context).getGroup();

            // No network connection, update with what we have in database
            if (group != null) {
                updateViewWithGroup(group);
            }

            return;
        }

        showLoading();

        // Update group from server
        final String baseUrl = getString(R.string.base_url);
        Profile profile = Profile.getCurrentProfile();
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", profile.getId());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON object", e);
            Toast.makeText(context, "An unexpected error occurred", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest groupRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.GROUP_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.has("group")) {
                        // We got a group! Let's update our view
                        Group group = new Group(response.getJSONObject("group"));
                        Db.getInstance(context).insertGroup(group);
                        updateViewWithGroup(group);
                    } else {
                        // No group so lets see if anyone invited us to theirs
                        JSONObject reqBody = new JSONObject();
                        reqBody.put("facebookId", Profile.getCurrentProfile().getId());

                        JsonObjectRequest loginRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.LOGIN_URL, reqBody, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONObject userJSON = response.getJSONObject("user");
                                    JSONArray invitedToGroupsJSON = userJSON.getJSONArray("invitedTo");
                                    List<Group> invitedToGroups = new ArrayList<>();
                                    for (int i = 0; i < invitedToGroupsJSON.length(); i++) {
                                        invitedToGroups.add(new Group(invitedToGroupsJSON.getJSONObject(i)));
                                    }

                                    updatedViewWithInvites(invitedToGroups);
                                } catch (JSONException e) {
                                    handleError(e);
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                handleError(error);
                            }
                        });

                        Server.getInstance(context).addToRequestQueue(loginRequest);
                    }
                } catch (JSONException e) {
                    handleError(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                handleError(error);
            }
        });

        Server.getInstance(context).addToRequestQueue(groupRequest);
    }

    private void deleteGroup() {
        if (!AppUtils.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();

        String baseUrl = getString(R.string.base_url);
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", Profile.getCurrentProfile().getId());
        } catch (JSONException e) {
            handleError(e);
            return;
        }

        JsonObjectRequest deleteGroupRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.GROUP_DELETE_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                hideLoading();
                Toast.makeText(getApplicationContext(), "Disbanded group", Toast.LENGTH_SHORT).show();
                refreshGroup(getApplicationContext());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                handleError(error);
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(deleteGroupRequest);
    }

    private void updateViewWithGroup(Group group) {
        hideLoading();
        findViewById(R.id.no_group_exists).setVisibility(View.GONE);

        if (!Profile.getCurrentProfile().getId().equals(group.owner.id)) {
            findViewById(R.id.add_new_roommate).setVisibility(View.GONE);
        } else {
            findViewById(R.id.add_new_roommate).setVisibility(View.VISIBLE);
        }

        final User[] users = new User[group.members.size()];
        group.members.toArray(users);

        String[] displayNames = new String[users.length];

        for (int i = 0; i < users.length; i++) {
            displayNames[i] = users[i].displayName;
        }

        ListView roommatesListView = (ListView) findViewById(R.id.roommates);
        roommatesListView.setAdapter(new RoommateArrayAdapter(getApplicationContext(), users, displayNames));

        roommatesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User selectedUser = users[position];
                Toast.makeText(getApplicationContext(), "Selected: " + selectedUser.displayName, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.group_exists).setVisibility(View.VISIBLE);
    }

    private void updatedViewWithInvites(List<Group> groups) {
        hideLoading();
        findViewById(R.id.group_exists).setVisibility(View.GONE);

        TextView inviteTextView = (TextView) findViewById(R.id.invites_title);

        if (groups.size() == 0) {
            inviteTextView.setText(R.string.no_invites);
        } else {
            inviteTextView.setText(R.string.invites);
        }

        findViewById(R.id.no_group_exists).setVisibility(View.VISIBLE);
    }

    private void createGroup(final Context context) {
        showLoading();

        String baseUrl = context.getString(R.string.base_url);
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", Profile.getCurrentProfile().getId());
        } catch (JSONException e) {
            handleError(e);
            return;
        }

        JsonObjectRequest createGroupRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.GROUP_CREATE_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject groupJSON = response.getJSONObject("group");
                    Group group = new Group(groupJSON);
                    Db.getInstance(context).insertGroup(group);
                    updateViewWithGroup(group);
                    Toast.makeText(getApplicationContext(), "Created new group", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    handleError(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                handleError(error);
            }
        });

        Server.getInstance(context).addToRequestQueue(createGroupRequest);
    }

    private void handleError(Exception error) {
        Log.e(TAG, error.getMessage(), error);
        Toast.makeText(getApplicationContext(), "An unexpected error occurred getting your roommates", Toast.LENGTH_SHORT).show();
    }

    private void showLoading() {
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }
}

package com.munaz.roomies;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.munaz.roomies.adapters.RoommateArrayAdapter;
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
                } else {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    leaveGroup();
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.leave_group_prompt).setPositiveButton(R.string.yes, dialogClickListener)
                            .setNegativeButton(R.string.no, dialogClickListener).show();
                }
            }
        });

        final Button inviteRoommateButton = (Button) findViewById(R.id.add_new_roommate);
        inviteRoommateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtils.isNetworkAvailable(getApplicationContext())) {
                    onSearchRequested();
                } else {
                    Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_SHORT).show();
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
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            refreshGroup(getApplicationContext());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean returnVal = false;

        if (Db.getInstance(getApplicationContext()).getGroup() == null) {
            Toast.makeText(getApplicationContext(), "Please join or create a group first", Toast.LENGTH_SHORT).show();
        } else if (id != R.id.roommates) {
            Intent intent = null;

            if (id == R.id.chores) {
                intent = new Intent(this, ChoresActivity.class);
            } else if (id == R.id.expenses) {
                intent = new Intent(this, ExpensesActivity.class);
            } else if (id == R.id.pantry) {
                intent = new Intent(this, PantryActivity.class);
            } else if (id == R.id.nav_manage) {
                // Handle the settings action
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            returnVal = true;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return returnVal;
    }

    private void refreshGroup(final Context context) {
        if (!AppUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show();
            Group group = Db.getInstance(context).getGroup();

            if (group != null) {
                updateViewWithGroup(group);
            }

            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Retrieving group. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        final String baseUrl = getString(R.string.base_url);
        Profile profile = Profile.getCurrentProfile();
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", profile.getId());
        } catch (JSONException e) {
            dialog.hide();
            handleError(e);
            return;
        }

        JsonObjectRequest groupRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.GROUP_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.has("group")) {
                        // We got a group! Let's update our view
                        dialog.hide();
                        Group group = new Group(response.getJSONObject("group"));
                        Db.getInstance(context).insertGroup(group);
                        updateViewWithGroup(group);
                    } else {
                        // No group so lets see if anyone invited us to theirs
                        Db.getInstance(getApplicationContext()).emptyDb();
                        JSONObject reqBody = new JSONObject();
                        reqBody.put("facebookId", Profile.getCurrentProfile().getId());

                        JsonObjectRequest loginRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.INVITES_URL, reqBody, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                dialog.hide();
                                List<Group> invitedGroups = new ArrayList<>();
                                if (response.has("groups")) {
                                    try {
                                        JSONArray groups = response.getJSONArray("groups");
                                        for (int i = 0; i < groups.length(); i++) {
                                            invitedGroups.add(new Group(groups.getJSONObject(i)));
                                        }
                                    } catch (JSONException e) {
                                        handleError(e);
                                        return;
                                    }
                                }
                                updatedViewWithInvites(invitedGroups);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                dialog.hide();
                                handleError(error);
                            }
                        });

                        Server.getInstance(context).addToRequestQueue(loginRequest);
                    }
                } catch (JSONException e) {
                    dialog.hide();
                    handleError(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.hide();
                handleError(error);
            }
        });

        Server.getInstance(context).addToRequestQueue(groupRequest);
    }

    private void updateViewWithGroup(final Group group) {
        findViewById(R.id.no_group_exists).setVisibility(View.GONE);

        if (!Profile.getCurrentProfile().getId().equals(group.owner.id)) {
            findViewById(R.id.add_new_roommate).setVisibility(View.GONE);
        } else {
            findViewById(R.id.add_new_roommate).setVisibility(View.VISIBLE);
        }

        final User[] members = new User[group.members.size()];
        group.members.toArray(members);

        String[] displayNames = new String[members.length];

        for (int i = 0; i < members.length; i++) {
            displayNames[i] = members[i].displayName;
        }

        ListView roommatesListView = (ListView) findViewById(R.id.roommates);
        roommatesListView.setAdapter(new RoommateArrayAdapter(getApplicationContext(), members, displayNames));

        roommatesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final User selectedUser = members[position];
                String profileId = Profile.getCurrentProfile().getId();
                if (group.owner.id.equals(profileId) && !selectedUser.id.equals(profileId)) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    kickFromGroup(selectedUser);
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(getString(R.string.kick_prompt) + selectedUser.displayName).setPositiveButton(R.string.yes, dialogClickListener)
                            .setNegativeButton(R.string.no, dialogClickListener).show();
                }
            }
        });

        final User[] invited = new User[group.invited.size()];
        group.invited.toArray(invited);

        if (invited.length == 0) {
            findViewById(R.id.invited_label).setVisibility(View.GONE);
            findViewById(R.id.invited).setVisibility(View.GONE);
        } else {
            findViewById(R.id.invited_label).setVisibility(View.VISIBLE);
            findViewById(R.id.invited).setVisibility(View.VISIBLE);

            String[] invitedDisplayNames = new String[invited.length];

            for (int i = 0; i < invited.length; i++) {
                invitedDisplayNames[i] = invited[i].displayName;
            }

            ListView invitedListView = (ListView) findViewById(R.id.invited);
            invitedListView.setAdapter(new RoommateArrayAdapter(getApplicationContext(), invited, invitedDisplayNames));

            invitedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final User selectedUser = invited[position];
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    uninvite(selectedUser);
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(getString(R.string.uninvite_prompt) + selectedUser.displayName).setPositiveButton(R.string.yes, dialogClickListener)
                            .setNegativeButton(R.string.no, dialogClickListener).show();
                }
            });
        }

        findViewById(R.id.group_exists).setVisibility(View.VISIBLE);
    }

    private void updatedViewWithInvites(List<Group> groups) {
        findViewById(R.id.group_exists).setVisibility(View.GONE);
        ListView invitesListView = (ListView) findViewById(R.id.invites);

        TextView inviteTextView = (TextView) findViewById(R.id.invites_title);

        if (groups.size() == 0) {
            inviteTextView.setText(R.string.no_invites);
            invitesListView.setVisibility(View.INVISIBLE);
        } else {
            inviteTextView.setText(R.string.invites);

            final User[] owners = new User[groups.size()];
            String[] displayNames = new String[groups.size()];

            for (int i = 0; i < groups.size(); i++) {
                owners[i] = groups.get(i).owner;
                displayNames[i] = groups.get(i).owner.displayName;
            }

            invitesListView.setVisibility(View.VISIBLE);
            invitesListView.setAdapter(new RoommateArrayAdapter(getApplicationContext(), owners, displayNames));

            invitesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final User selectedOwner = owners[position];

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    acceptInvite(selectedOwner);
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Are you sure you want to join " + selectedOwner.displayName + "'s group").setPositiveButton(R.string.yes, dialogClickListener)
                            .setNegativeButton(R.string.no, dialogClickListener).show();
                }
            });
        }

        findViewById(R.id.no_group_exists).setVisibility(View.VISIBLE);
    }

    private void acceptInvite(User owner) {
        if (!AppUtils.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Accept invite. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        String baseUrl = getString(R.string.base_url);
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", Profile.getCurrentProfile().getId());
            reqBody.put("ownerId", owner.id);
        } catch (JSONException e) {
            dialog.hide();
            handleError(e);
            return;
        }

        JsonObjectRequest acceptInviteRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.GROUP_ACCEPT_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dialog.hide();
                Toast.makeText(getApplicationContext(), "Joined group", Toast.LENGTH_SHORT).show();
                refreshGroup(getApplicationContext());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.hide();
                handleError(error);
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(acceptInviteRequest);
    }

    private void uninvite(User user) {
        if (!AppUtils.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Uninviting from group. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        String baseUrl = getString(R.string.base_url);
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", Profile.getCurrentProfile().getId());
            reqBody.put("invitedId", user.id);
        } catch (JSONException e) {
            dialog.hide();
            handleError(e);
            return;
        }

        JsonObjectRequest uninviteRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.GROUP_UNINVITE_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dialog.hide();
                Toast.makeText(getApplicationContext(), "Uninvited from group", Toast.LENGTH_SHORT).show();
                refreshGroup(getApplicationContext());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.hide();
                handleError(error);
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(uninviteRequest);
    }

    private void kickFromGroup(User user) {
        if (!AppUtils.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Kicking from group. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        String baseUrl = getString(R.string.base_url);
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", Profile.getCurrentProfile().getId());
            reqBody.put("kickId", user.id);
        } catch (JSONException e) {
            handleError(e);
            return;
        }

        JsonObjectRequest kickRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.GROUP_KICK_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dialog.hide();
                Toast.makeText(getApplicationContext(), "Kicked from group", Toast.LENGTH_SHORT).show();
                refreshGroup(getApplicationContext());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.hide();
                handleError(error);
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(kickRequest);
    }

    private void createGroup(final Context context) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Creating group. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

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
                    dialog.hide();
                    updateViewWithGroup(group);
                    Toast.makeText(getApplicationContext(), "Created new group", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    dialog.hide();
                    handleError(e);
                }
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

    private void deleteGroup() {
        if (!AppUtils.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Disbanding group. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

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
                dialog.hide();
                Toast.makeText(getApplicationContext(), "Disbanded group", Toast.LENGTH_SHORT).show();
                refreshGroup(getApplicationContext());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.hide();
                handleError(error);
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(deleteGroupRequest);
    }

    private void leaveGroup() {
        if (!AppUtils.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Leaving group. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        String baseUrl = getString(R.string.base_url);
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", Profile.getCurrentProfile().getId());
        } catch (JSONException e) {
            handleError(e);
            return;
        }

        JsonObjectRequest leaveGroupRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.GROUP_LEAVE_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dialog.hide();
                Toast.makeText(getApplicationContext(), "Left group", Toast.LENGTH_SHORT).show();
                refreshGroup(getApplicationContext());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.hide();
                handleError(error);
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(leaveGroupRequest);
    }

    private void handleError(Exception error) {
        Log.e(TAG, error.getMessage(), error);
        Toast.makeText(getApplicationContext(), "An unexpected error occurred getting your roommates", Toast.LENGTH_SHORT).show();
    }
}

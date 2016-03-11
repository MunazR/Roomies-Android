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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.Profile;
import com.munaz.api.Server;
import com.munaz.db.Db;
import com.munaz.model.Chore;
import com.munaz.model.Group;
import com.munaz.model.User;
import com.munaz.roomies.adapters.ChoreArrayAdapter;
import com.munaz.util.AppUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ChoresActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private final String TAG = "ChoresActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chores);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Button createChoreButton = (Button) findViewById(R.id.create_chore);
        createChoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createChore(getApplicationContext());
            }
        });

        refreshChores(getApplicationContext());
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
            refreshChores(getApplicationContext());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean returnVal = false;

        if (id != R.id.chores) {
            Intent intent = null;
            if (id == R.id.roommates) {
                intent = new Intent(this, MainActivity.class);
            } else if (id == R.id.expenses) {
                intent = new Intent(this, ExpensesActivity.class);
            } else if (id == R.id.pantry) {
                intent = new Intent(this, PantryActivity.class);
            } else if (id == R.id.nav_manage) {
                intent = new Intent(this, SettingsActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            returnVal = true;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return returnVal;
    }

    private void refreshChores(final Context context) {
        if (!AppUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show();
            Group group = Db.getInstance(context).getGroup();

            if (group != null) {
                updateViewWithChores(group);
            }

            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Retrieving chores. Please wait...");
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
                dialog.hide();

                try {
                    if (response.has("group")) {
                        Group group = new Group(response.getJSONObject("group"));
                        Db.getInstance(context).insertGroup(group);
                        updateViewWithChores(group);
                    } else {
                        Toast.makeText(context, "You are not part of any group!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
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

    private void createChore(final Context context) {
        if (!AppUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final List<User> members = Db.getInstance(context).getGroup().members;
            String[] memberNames = new String[members.size()];

            for (int i = 0; i < memberNames.length; i++) {
                memberNames[i] = members.get(i).displayName;
            }

            LayoutInflater layoutInflater = LayoutInflater.from(this);
            final View inflator = layoutInflater.inflate(R.layout.dialog_chore, null);
            final EditText titleText = (EditText) inflator.findViewById(R.id.chore_title);

            builder.setTitle(R.string.create_chore)
                    .setIcon(R.drawable.icon_chore)
                    .setView(inflator)
                    .setSingleChoiceItems(memberNames, 0, null)
                    .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            User assignedToUser = members.get(((AlertDialog) dialog).getListView().getCheckedItemPosition());
                            String choreTitle = titleText.getText().toString().trim();

                            if (choreTitle.equals("")) {
                                Toast.makeText(context, R.string.no_chore_title_message, Toast.LENGTH_SHORT).show();
                            } else {
                                final ProgressDialog loadingDialog = new ProgressDialog(ChoresActivity.this);
                                loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                loadingDialog.setMessage("Creating chore. Please wait...");
                                loadingDialog.setIndeterminate(true);
                                loadingDialog.setCanceledOnTouchOutside(false);
                                loadingDialog.show();

                                final String baseUrl = getString(R.string.base_url);
                                Profile profile = Profile.getCurrentProfile();
                                JSONObject reqBody = new JSONObject();

                                try {
                                    reqBody.put("facebookId", profile.getId());
                                    JSONObject chore = new JSONObject();
                                    chore.put("title", choreTitle);
                                    chore.put("assignedTo", assignedToUser.id);
                                    reqBody.put("chore", chore);
                                } catch (JSONException e) {
                                    loadingDialog.hide();
                                    handleError(e);
                                    return;
                                }

                                JsonObjectRequest createChoreRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.CHORE_CREATE_URL, reqBody, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        loadingDialog.hide();
                                        Toast.makeText(context, R.string.chore_created, Toast.LENGTH_SHORT).show();
                                        refreshChores(context);
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        loadingDialog.hide();
                                        handleError(error);
                                    }
                                });

                                Server.getInstance(context).addToRequestQueue(createChoreRequest);
                            }
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void deleteChore(Chore chore) {
        final ProgressDialog loadingDialog = new ProgressDialog(ChoresActivity.this);
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingDialog.setMessage("Removing chore. Please wait...");
        loadingDialog.setIndeterminate(true);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();

        final String baseUrl = getString(R.string.base_url);
        Profile profile = Profile.getCurrentProfile();
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", profile.getId());
            reqBody.put("choreId", chore.id);
        } catch (JSONException e) {
            loadingDialog.hide();
            handleError(e);
            return;
        }

        JsonObjectRequest deleteChoreRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.CHORE_DELETE_URL, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingDialog.hide();
                Toast.makeText(getApplicationContext(), R.string.chore_removed, Toast.LENGTH_SHORT).show();
                refreshChores(getApplicationContext());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loadingDialog.hide();
                handleError(error);
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(deleteChoreRequest);
    }

    private void updateViewWithChores(final Group group) {
        ListView choresListView = (ListView) findViewById(R.id.chores_list);

        if (group.chores.size() == 0) {
            findViewById(R.id.no_chores).setVisibility(View.VISIBLE);
            choresListView.setAdapter(null);
        } else {
            findViewById(R.id.no_chores).setVisibility(View.GONE);

            User[] users = new User[group.members.size()];
            final Chore[] chores = new Chore[group.chores.size()];
            String[] choreTitles = new String[chores.length];

            for (int i = 0; i < users.length; i++) {
                users[i] = group.members.get(i);
            }

            for (int i = 0; i < chores.length; i++) {
                chores[i] = group.chores.get(i);
            }

            for (int i = 0; i < choreTitles.length; i++) {
                choreTitles[i] = chores[i].title;
            }

            choresListView.setAdapter(new ChoreArrayAdapter(getApplicationContext(), chores, users, choreTitles));

            choresListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final Chore selectedChore = chores[position];

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    deleteChore(selectedChore);
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(ChoresActivity.this);
                    builder.setMessage(R.string.remove_chore_prompt).setPositiveButton(R.string.yes, dialogClickListener)
                            .setNegativeButton(R.string.no, dialogClickListener).show();
                }
            });
        }
    }

    private void handleError(Exception error) {
        Log.e(TAG, error.getMessage(), error);
        Toast.makeText(getApplicationContext(), "An unexpected error occurred getting your roommates", Toast.LENGTH_SHORT).show();
    }
}

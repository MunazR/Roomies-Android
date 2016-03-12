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
import com.munaz.model.Group;
import com.munaz.model.PantryItem;
import com.munaz.roomies.adapters.PantryArrayAdapter;
import com.munaz.util.AppUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class PantryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private final String TAG = "PantryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Button createPantryItemButton = (Button) findViewById(R.id.create_pantry_item);
        createPantryItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPantryItem(getApplicationContext());
            }
        });

        refreshPantryItems(getApplicationContext());
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
            refreshPantryItems(getApplicationContext());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean returnVal = false;

        if (id != R.id.pantry) {
            Intent intent = null;
            if (id == R.id.roommates) {
                intent = new Intent(this, MainActivity.class);
            } else if (id == R.id.expenses) {
                intent = new Intent(this, ExpensesActivity.class);
            } else if (id == R.id.chores) {
                intent = new Intent(this, ChoresActivity.class);
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

    private void refreshPantryItems(final Context context) {
        if (!AppUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show();
            Group group = Db.getInstance(context).getGroup();

            if (group != null) {
                updateViewWithPantryItems(group);
            }

            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Retrieving pantry. Please wait...");
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
                        updateViewWithPantryItems(group);
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

    private void createPantryItem(final Context context) {
        if (!AppUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            final View inflator = layoutInflater.inflate(R.layout.dialog_pantry, null);
            final EditText titleText = (EditText) inflator.findViewById(R.id.pantry_item_title);

            builder.setTitle("Create pantry item")
                    .setIcon(R.drawable.icon_pantry)
                    .setView(inflator)
                    .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String pantryItemTitle = titleText.getText().toString().trim();

                            if (pantryItemTitle.equals("")) {
                                Toast.makeText(context, R.string.no_pantry_title_message, Toast.LENGTH_SHORT).show();
                            } else {
                                final ProgressDialog loadingDialog = new ProgressDialog(PantryActivity.this);
                                loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                loadingDialog.setMessage("Creating pantry item. Please wait...");
                                loadingDialog.setIndeterminate(true);
                                loadingDialog.setCanceledOnTouchOutside(false);
                                loadingDialog.show();

                                final String baseUrl = getString(R.string.base_url);
                                Profile profile = Profile.getCurrentProfile();
                                JSONObject reqBody = new JSONObject();

                                try {
                                    reqBody.put("facebookId", profile.getId());
                                    JSONObject item = new JSONObject();
                                    item.put("title", pantryItemTitle);
                                    reqBody.put("item", item);
                                } catch (JSONException e) {
                                    loadingDialog.hide();
                                    handleError(e);
                                    return;
                                }

                                JsonObjectRequest createPantryItemRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.PANTRY_CREATE_URL, reqBody, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        loadingDialog.hide();
                                        Toast.makeText(context, R.string.pantry_item_created, Toast.LENGTH_SHORT).show();
                                        refreshPantryItems(context);
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        loadingDialog.hide();
                                        handleError(error);
                                    }
                                });

                                Server.getInstance(context).addToRequestQueue(createPantryItemRequest);
                            }
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void deletePantryItem(PantryItem pantryItem) {
        if (!AppUtils.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_SHORT).show();
        } else {
            final ProgressDialog loadingDialog = new ProgressDialog(PantryActivity.this);
            loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loadingDialog.setMessage("Removing pantry item. Please wait...");
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCanceledOnTouchOutside(false);
            loadingDialog.show();

            final String baseUrl = getString(R.string.base_url);
            Profile profile = Profile.getCurrentProfile();
            JSONObject reqBody = new JSONObject();

            try {
                reqBody.put("facebookId", profile.getId());
                reqBody.put("itemId", pantryItem.id);
            } catch (JSONException e) {
                loadingDialog.hide();
                handleError(e);
                return;
            }

            JsonObjectRequest deletePantryItemRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.PANTRY_DELETE_URL, reqBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    loadingDialog.hide();
                    Toast.makeText(getApplicationContext(), R.string.pantry_item_removed, Toast.LENGTH_SHORT).show();
                    refreshPantryItems(getApplicationContext());
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    loadingDialog.hide();
                    handleError(error);
                }
            });

            Server.getInstance(getApplicationContext()).addToRequestQueue(deletePantryItemRequest);
        }
    }

    private void updateViewWithPantryItems(final Group group) {
        ListView pantryListView = (ListView) findViewById(R.id.pantry_list);

        if (group.pantryItems.size() == 0) {
            findViewById(R.id.no_pantry_items).setVisibility(View.VISIBLE);
            pantryListView.setAdapter(null);
        } else {
            findViewById(R.id.no_pantry_items).setVisibility(View.GONE);

            final PantryItem[] pantryItems = new PantryItem[group.pantryItems.size()];
            String[] pantryItemTitles = new String[pantryItems.length];

            for (int i = 0; i < pantryItems.length; i++) {
                pantryItems[i] = group.pantryItems.get(i);
            }

            for (int i = 0; i < pantryItemTitles.length; i++) {
                pantryItemTitles[i] = pantryItems[i].title;
            }

            pantryListView.setAdapter(new PantryArrayAdapter(getApplicationContext(), pantryItems, pantryItemTitles));

            pantryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final PantryItem selectedPantryItem = pantryItems[position];

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    deletePantryItem(selectedPantryItem);
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(PantryActivity.this);
                    builder.setMessage(R.string.remove_pantry_item_prompt).setPositiveButton(R.string.yes, dialogClickListener)
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

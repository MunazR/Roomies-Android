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
import com.munaz.model.Expense;
import com.munaz.model.Group;
import com.munaz.model.User;
import com.munaz.roomies.adapters.ExpenseArrayAdapter;
import com.munaz.util.AppUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class ExpensesActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private final String TAG = "ExpensesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Button createExpenseButton = (Button) findViewById(R.id.create_expense);
        createExpenseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createExpense(getApplicationContext());
            }
        });

        refreshExpenses(getApplicationContext());
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
            refreshExpenses(getApplicationContext());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id != R.id.expenses) {
            Intent intent = null;
            if (id == R.id.roommates) {
                intent = new Intent(this, MainActivity.class);
            } else if (id == R.id.pantry) {
                intent = new Intent(this, PantryActivity.class);
            } else if (id == R.id.chores) {
                intent = new Intent(this, ChoresActivity.class);
            } else if (id == R.id.nav_manage) {
                intent = new Intent(this, SettingsActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    private void refreshExpenses(final Context context) {
        if (!AppUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show();
            Group group = Db.getInstance(context).getGroup();

            if (group != null) {
                updateViewWithExpenses(group);
            }

            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Retrieving expenses. Please wait...");
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
                        updateViewWithExpenses(group);
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

    private void createExpense(final Context context) {
        if (!AppUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show();
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            final View inflator = layoutInflater.inflate(R.layout.dialog_expense, null);
            final EditText expenseTitleText = (EditText) inflator.findViewById(R.id.expense_title);
            final EditText expenseAmountText = (EditText) inflator.findViewById(R.id.expense_amount);

            builder.setTitle(R.string.create_expense)
                    .setIcon(R.drawable.icon_expense)
                    .setView(inflator)
                    .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String expenseTitle = expenseTitleText.getText().toString().trim();
                            String expenseAmount = expenseAmountText.getText().toString().trim();

                            if (expenseTitle.equals("")) {
                                Toast.makeText(context, R.string.no_expense_title, Toast.LENGTH_SHORT).show();
                            } else if (!AppUtils.isInt(expenseAmount)) {
                                Toast.makeText(context, R.string.invalid_expense_amount, Toast.LENGTH_SHORT).show();
                            } else {
                                final ProgressDialog loadingDialog = new ProgressDialog(ExpensesActivity.this);
                                loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                loadingDialog.setMessage("Creating expense. Please wait...");
                                loadingDialog.setIndeterminate(true);
                                loadingDialog.setCanceledOnTouchOutside(false);
                                loadingDialog.show();

                                final String baseUrl = getString(R.string.base_url);
                                Profile profile = Profile.getCurrentProfile();
                                JSONObject reqBody = new JSONObject();

                                try {
                                    reqBody.put("facebookId", profile.getId());
                                    JSONObject expense = new JSONObject();
                                    expense.put("title", expenseTitle);
                                    expense.put("amount", Integer.parseInt(expenseAmount));
                                    expense.put("expensedBy", profile.getId());
                                    reqBody.put("expense", expense);
                                } catch (JSONException e) {
                                    loadingDialog.hide();
                                    handleError(e);
                                    return;
                                }

                                JsonObjectRequest createExpenseRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.EXPENSE_CREATE_URL, reqBody, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        loadingDialog.hide();
                                        Toast.makeText(context, R.string.expense_created, Toast.LENGTH_SHORT).show();
                                        refreshExpenses(context);
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        loadingDialog.hide();
                                        handleError(error);
                                    }
                                });

                                Server.getInstance(context).addToRequestQueue(createExpenseRequest);
                            }
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void deleteExpense(Expense expense) {
        if (!AppUtils.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "No network connection", Toast.LENGTH_SHORT).show();
        } else {
            final ProgressDialog loadingDialog = new ProgressDialog(ExpensesActivity.this);
            loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loadingDialog.setMessage("Removing expense. Please wait...");
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCanceledOnTouchOutside(false);
            loadingDialog.show();

            final String baseUrl = getString(R.string.base_url);
            Profile profile = Profile.getCurrentProfile();
            JSONObject reqBody = new JSONObject();

            try {
                reqBody.put("facebookId", profile.getId());
                reqBody.put("expenseId", expense.id);
            } catch (JSONException e) {
                loadingDialog.hide();
                handleError(e);
                return;
            }

            JsonObjectRequest deleteExpenseRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + Server.EXPENSE_DELETE_URL, reqBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    loadingDialog.hide();
                    Toast.makeText(getApplicationContext(), R.string.expense_removed, Toast.LENGTH_SHORT).show();
                    refreshExpenses(getApplicationContext());
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    loadingDialog.hide();
                    handleError(error);
                }
            });

            Server.getInstance(getApplicationContext()).addToRequestQueue(deleteExpenseRequest);
        }
    }

    private void updateViewWithExpenses(final Group group) {
        ListView expensesListView = (ListView) findViewById(R.id.expense_list);

        if (group.expenses.size() == 0) {
            findViewById(R.id.no_expenses).setVisibility(View.VISIBLE);
            expensesListView.setAdapter(null);
        } else {
            findViewById(R.id.no_expenses).setVisibility(View.GONE);

            User[] users = new User[group.members.size()];
            final Expense[] expenses = new Expense[group.expenses.size()];
            String[] expenseTitles = new String[expenses.length];

            for (int i = 0; i < users.length; i++) {
                users[i] = group.members.get(i);
            }

            for (int i = 0; i < expenses.length; i++) {
                expenses[i] = group.expenses.get(i);
            }

            for (int i = 0; i < expenseTitles.length; i++) {
                expenseTitles[i] = expenses[i].title;
            }

            expensesListView.setAdapter(new ExpenseArrayAdapter(getApplicationContext(), expenses, users, expenseTitles));

            expensesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final Expense selectedExpense = expenses[position];

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    deleteExpense(selectedExpense);
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(ExpensesActivity.this);
                    builder.setMessage(R.string.remove_expense_prompt).setPositiveButton(R.string.yes, dialogClickListener)
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

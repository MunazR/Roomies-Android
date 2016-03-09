package com.munaz.roomies;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.munaz.api.Server;
import com.munaz.db.Db;
import com.munaz.model.User;
import com.munaz.util.AppUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    private final String TAG = "LoginActivity: ";

    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Intent mainActivity = new Intent(this, MainActivity.class);

        super.onCreate(savedInstanceState);

        // Set up the login form.
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_login);

        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        // Other app specific specialization

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                login(mainActivity);
            }

            @Override
            public void onCancel() {
                Log.w(TAG, "Login cancelled");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e(TAG, "Error logging in using Facebook", exception);
            }
        });

        // Check if user is already logged in
        if (AppUtils.isLoggedInFacebook()) {
            // If user exists in database take them to MainActivity
            if (Db.getInstance(getApplicationContext()).getUser(Profile.getCurrentProfile().getId())
                    != null) {
                startActivity(mainActivity);
                finish();
            } else { // Log them into server
                login(mainActivity);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {
            startActivity(data);
            finish();
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void login(final Intent intent) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Logging in. Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        Profile profile = Profile.getCurrentProfile();

        if (profile == null) {
            dialog.hide();
            Toast.makeText(getApplicationContext(), "Login failed, try again later", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if network is connected
        if (!AppUtils.isNetworkAvailable(getApplicationContext())) {
            dialog.hide();
            Toast.makeText(getApplicationContext(), "No network connection, try again later", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = getString(R.string.base_url) + Server.LOGIN_URL;
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", profile.getId());
            reqBody.put("firstName", profile.getFirstName());
            reqBody.put("lastName", profile.getLastName());
            reqBody.put("displayName", profile.getName());
            reqBody.put("profilePictureUrl", profile.getProfilePictureUri(64, 64).toString());
        } catch (JSONException e) {
            dialog.hide();
            Log.e(TAG, "Error creating JSON object", e);
            Toast.makeText(getApplicationContext(), "An unexpected error occurred logging in", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest loginRequest = new JsonObjectRequest(Request.Method.POST, url, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(TAG, "Login to server successful");
                dialog.hide();
                try {
                    JSONObject user = response.getJSONObject("user");
                    Db.getInstance(getApplicationContext()).insertUser(new User(user));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "An unexpected error occurred logging in", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(intent);
                finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error logging into server", error);
                dialog.hide();
                Toast.makeText(getApplicationContext(), "An unexpected error occurred logging in", Toast.LENGTH_SHORT).show();
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(loginRequest);
    }
}


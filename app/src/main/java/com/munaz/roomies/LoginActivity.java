package com.munaz.roomies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.munaz.api.Server;

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

        if (isLoggedIn()) {
            login(mainActivity);
            return;
        }

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {
            startActivity(data);
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null;
    }

    private void login(final Intent intent) {
        showLoading();
        String url = getString(R.string.base_url) + "/login";
        Profile profile = Profile.getCurrentProfile();
        JSONObject reqBody = new JSONObject();

        try {
            reqBody.put("facebookId", profile.getId());
            reqBody.put("facebookToken", AccessToken.getCurrentAccessToken().getToken());
            reqBody.put("firstName", profile.getFirstName());
            reqBody.put("lastName", profile.getLastName());
            reqBody.put("name", profile.getName());
            reqBody.put("profilePictureUrl", profile.getProfilePictureUri(64, 64).toString());
        } catch (JSONException e) {
            hideLoading();
            Log.e(TAG, "Error creating JSON object", e);
            Toast.makeText(getApplicationContext(), "An unexpected error occurred logging in", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest loginRequest = new JsonObjectRequest(Request.Method.POST, url, reqBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(TAG, "Login to server successful");
                hideLoading();
                startActivity(intent);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error logging into server", error);
                hideLoading();
                Toast.makeText(getApplicationContext(), "An unexpected error occurred logging in", Toast.LENGTH_SHORT).show();;
            }
        });

        Server.getInstance(getApplicationContext()).addToRequestQueue(loginRequest);
    }

    private void showLoading() {
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }

    private void hideLoading() {
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }
}


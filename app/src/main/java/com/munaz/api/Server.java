package com.munaz.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Singleton used for queueing requests
 */
public class Server {
    public static final String LOGIN_URL = "/login";
    public static final String USERS_URL = "/users";
    public static final String GROUP_URL = "/group";
    public static final String INVITES_URL = "/invites";
    public static final String GROUP_CREATE_URL = "/group/create";
    public static final String GROUP_DELETE_URL = "/group/delete";
    public static final String GROUP_LEAVE_URL = "/group/leave";
    public static final String GROUP_UNINVITE_URL = "/group/uninvite";
    public static final String GROUP_KICK_URL = "/group/kick";
    public static final String GROUP_INVITE_URL = "/group/invite";
    public static final String GROUP_ACCEPT_URL = "/group/accept";

    private static Server mInstance = null;
    private static Context mCtx;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private Server(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();

        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap>
                    cache = new LruCache<>(20);

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });
    }

    public static synchronized Server getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Server(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }
}

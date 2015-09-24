// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.net;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.projectbuendia.client.utils.Logger;

/** Wraps Volley up in a singleton object. */
public class VolleySingleton {

    private static final Logger LOG = Logger.create();
    private static VolleySingleton sInstance;
    private static final String[] sMethodNames = new String[6];

    static {
        sMethodNames[Request.Method.GET] = "GET";
        sMethodNames[Request.Method.POST] = "POST";
        sMethodNames[Request.Method.PUT] = "PUT";
        sMethodNames[Request.Method.DELETE] = "DELETE";
        sMethodNames[Request.Method.HEAD] = "HEAD";
    }

    private final RequestQueue mRequestQueue;

    /**
     * Get the VolleySingleton instance for doing multiple operations on a single context.
     * In general prefer convenience methods unless doing multiple operations.
     * @param context the Android Application context
     * @return the Singleton for accessing Volley.
     */
    public static synchronized VolleySingleton getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new VolleySingleton(context);
        }
        return sInstance;
    }

    /**
     * A convenience method for adding a request to the Volley request queue getting all singleton
     * handling and contexts correct.
     */
    public <T> void addToRequestQueue(Request<T> req) {
        LOG.i("%s %s", getMethodName(req.getMethod()), req.getUrl());
        getRequestQueue().add(req);
    }

    /** Gets the string name of a Request.Method constant. */
    public static String getMethodName(int method) {
        String methodName = method < sMethodNames.length ? sMethodNames[method] : null;
        return methodName == null ? "" + method : methodName;
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    private VolleySingleton(Context context) {
        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }
}

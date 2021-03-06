/*
 * Copyright 2018 Soojeong Shin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.udacity.gradle.builditbigger;

import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.udacity.gradle.builditbigger.backend.myApi.MyApi;

import java.io.IOException;
import java.util.List;

import timber.log.Timber;

public class EndpointsAsyncTask extends AsyncTask<String, Void, List<String>> {

    private static MyApi myApiService = null;

    /** Member variable for exception to handle errors */
    private Exception mException = null;

    /**
     * Define a new interface EndpointAsyncTaskListener that triggers a Callback.
     * The callback is a method named onComplete that contains String joke and exception.
     */
    private EndpointsAsyncTaskListener mListener = null;

    public interface EndpointsAsyncTaskListener {
        void onComplete(List<String> jokeResult, Exception e);
    }

    /**
     * Define a new interface OnTaskComplete that triggers a Callback. Calls methods in the
     * MainActivity named onTaskComplete and onPreTask
     *
     * Reference: @see "https://discussions.udacity.com/t/unable-to-start-activity-componentinfo-free-flavor/243702/10"
     * @see "https://discussions.udacity.com/t/waiting-for-the-task-to-get-the-joke-before-launching-android-library-activity-in-build-it-bigger/166488"
     * @see "https://stackoverflow.com/questions/9963691/android-asynctask-sending-callbacks-to-ui"
     */
    private OnTaskComplete mCallback;

    public interface OnTaskComplete {
        void onTaskComplete(List<String> jokeResult);
        void onPreTask();
    }

    /**
     * Constructor for EndpointsAsyncTask.
     * @param callback The callback interface
     */
    public EndpointsAsyncTask(OnTaskComplete callback) {
        mCallback = callback;
    }

    /**
     * Sets the EndpointsAsyncTaskListener and returns it.
     */
    public EndpointsAsyncTask setListener(EndpointsAsyncTaskListener listener) {
        this.mListener = listener;
        return this;
    }

    @Override
    protected void onPreExecute() {
        if (mCallback != null) {
            // Trigger the callback onPreTask to show a loading indicator
            mCallback.onPreTask();
        }
    }

    @Override
    protected List<String> doInBackground(String... params) {
        if (myApiService == null) { // Only do this once
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null)
                    // options for running against local dev app server
                    // - 10.0.2.2 is localhost's IP address in Android emulator
                    // - turn off compression when running against local dev app server
                    .setRootUrl(Constants.ROOT_URL)
                    .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                        @Override
                        public void initialize(AbstractGoogleClientRequest<?> request) throws IOException {
                            request.setDisableGZipContent(true);
                        }
                    });
                    // end options for dev app server

            myApiService = builder.build();
        }

        String category = params[0];

        try {
            return myApiService.pullJokes(category).execute().getListData();
        } catch (IOException e) {
            mException = e;
            Timber.e( "Failed to fetch jokes: "+ e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<String> result) {
        if (mListener != null) {
            // Trigger the callback onComplete after the background computation finishes
            mListener.onComplete(result, mException);
        }
        // Trigger the callback onTaskComplete
        if (mCallback != null) {
            mCallback.onTaskComplete(result);
        }
    }

    @Override
    protected void onCancelled() {
        if (mListener != null) {
            // Constructs an InterruptedException with the "AsyncTask cancelled" message.
            mException = new InterruptedException(Constants.EXCEPTION_MESSAGE);
            mListener.onComplete(null, mException);
        }
    }
}
package com.flonk.weatherapp;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Jacob on 07/11/2017.
 */

public class WeatherQueryHelper {

    private boolean mDownloading = false;
    private DownloadTask mDownloadTask;
    static final String OPEN_WEATHER_API_KEY = "4aa9cb9ba2eedc113cae1b13d346bb97";
    private ConnectivityManager connectivityManager;
    private WeatherQueryCallback callback;

    public WeatherQueryHelper(ConnectivityManager manager, WeatherQueryCallback callback){
        connectivityManager = manager;
        this.callback = callback;
    }

    public void Query(String cityName) throws JSONException {
        String OPEN_WEATHER_API_CITY =
                "http://api.openweathermap.org/data/2.5/weather?q=" + cityName
                        + "&APPID=" + OPEN_WEATHER_API_KEY;

        mDownloadTask = new DownloadTask(callback);
        mDownloadTask.execute(OPEN_WEATHER_API_CITY);





    }

    /**
     * Wrapper class that serves as a union of a result value and an exception. When the download
     * task has completed, either the result value or exception can be a non-null value.
     * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
     */
    public class WeatherQueryResult {
        public String mResultValue;
        public Exception mException;
        public WeatherQueryResult(String resultValue) {
            mResultValue = resultValue;
        }
        public WeatherQueryResult(Exception exception) {
            mException = exception;
        }
    }

    //A subclass of AsyncTask as a private inner class inside your Fragment

    private class DownloadTask extends AsyncTask<String, Integer, WeatherQueryResult> {

        private WeatherQueryCallback mCallback;

        DownloadTask(WeatherQueryCallback callback) {
            setCallback(callback);
        }

        void setCallback(WeatherQueryCallback callback) {
            mCallback = callback;
        }



        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            if (mCallback != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                        (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                                && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback.QueryResult(null);
                    cancel(true);
                }
            }
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected WeatherQueryResult doInBackground(String... urls) {
            WeatherQueryResult result = null;
            if (!isCancelled() && urls != null && urls.length > 0) {
                String urlString = urls[0];
                try {
                    URL url = new URL(urlString);
                    String resultString = downloadUrl(url);
                    if (resultString != null) {
                        result = new WeatherQueryResult(resultString);
                    } else {
                        throw new IOException("No response received.");
                    }
                } catch(Exception e) {
                    result = new WeatherQueryResult(e);
                }
            }
            return result;
        }

        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(WeatherQueryResult result) {
            if (result != null && mCallback != null) {
                if (result.mException != null) {
                    mCallback.QueryResult(result);
                } else if (result.mResultValue != null) {
                    mCallback.QueryResult(result);
                }
                mDownloading = false;
            }
        }

        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(WeatherQueryResult result) {
        }

        /**
         * Given a URL, sets up a connection and gets the HTTP response body from the server.
         * If the network request is successful, it returns the response body in String form. Otherwise,
         * it will throw an IOException.
         */
        private String downloadUrl(URL url) throws IOException {
            InputStream stream = null;
            HttpURLConnection connection = null;
            String result = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                connection.setReadTimeout(3000);
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connection.setConnectTimeout(3000);
                // For this use case, set HTTP method to GET.
                connection.setRequestMethod("GET");
                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                connection.setDoInput(true);
                // Open communications link (network traffic occurs here).
                connection.connect();
                int responseCode = connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }
                // Retrieve the response body as an InputStream.
                stream = connection.getInputStream();


                if (stream != null) {
                    // Converts Stream to String with max length of 500.
                    result = readStream(stream, 500);
                }
            } finally {
                // Close Stream and disconnect HTTPS connection.
                if (stream != null) {
                    stream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return result;
        }

        /**
         * Converts the contents of an InputStream to a String.
         */
        public String readStream(InputStream stream, int maxReadSize)
                throws IOException {
            Reader reader;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] rawBuffer = new char[maxReadSize];
            int readSize;
            StringBuffer buffer = new StringBuffer();
            while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
                if (readSize > maxReadSize) {
                    readSize = maxReadSize;
                }
                buffer.append(rawBuffer, 0, readSize);
                maxReadSize -= readSize;
            }
            return buffer.toString();
        }
    }

}

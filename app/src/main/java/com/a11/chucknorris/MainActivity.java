package com.a11.chucknorris;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private String baseUrl;
    private String joke;
    private String defaultJoke;
    private MyApplication application;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        baseUrl = "http://api.icndb.com/jokes/random/";
        defaultJoke = getResources().getString(R.string.default_joke);

        View loadIcon = findViewById(R.id.load_icon);
        loadIcon.setAlpha(0);
        application = (MyApplication) getApplication();
    }

    protected void onResume() {
        super.onResume();
        joke = defaultJoke;
        loadJoke();
        setScreen(joke);

        if (application.requestIsRunning) {
            application.myTask.activity = this;
            animateLoad();
        } else {
            stopLoad();
        }
    }

    protected void onPause() {
        saveJoke(joke);
        super.onPause();
    }

    public void setScreen(String joke) {
        joke = joke.replaceAll("&quot;", "\"");
        ((TextView) findViewById(R.id.textView)).setText(joke);
        stopLoad();
    }

    public void animateLoad() {
        RotateAnimation rotate;
        rotate = new RotateAnimation(0f, -360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);
        rotate.setRepeatMode(Animation.START_ON_FIRST_FRAME);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        View loadIcon = findViewById(R.id.load_icon);
        loadIcon.setAlpha(1);
        loadIcon.startAnimation(rotate);
    }

    private void stopLoad() {
        View loadIcon = findViewById(R.id.load_icon);
        loadIcon.setAlpha(0);
        loadIcon.clearAnimation();
    }

    class MyTask extends AsyncTask<Void, Void, Boolean> {
        private String result;
        private MainActivity activity;


        protected void onPreExecute() {
            super.onPreExecute();
            activity = MainActivity.this;
            animateLoad();
            ((MyApplication) getApplication()).requestIsRunning = true;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(baseUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                result = IOUtils.toString(in);

                result = result.substring(result.indexOf("joke") + 8);
                result = result.substring(0, result.indexOf("\""));

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            ((MyApplication) getApplication()).requestIsRunning = false;

            if (!result) {
                Toast.makeText(MainActivity.this, "Error - no connection.", Toast.LENGTH_SHORT).show();
                stopLoad();
                return;
            }
            activity.stopLoad();
            activity.saveJoke(this.result);
            activity.setScreen(this.result);
            activity.joke = this.result;
        }
    }

    public void onClick(View v) {

        if (application.requestIsRunning) {
            application.myTask.cancel(true);
            application.requestIsRunning = false;
        }

        application.myTask = new MyTask();
        application.myTask.execute();
    }

    public void saveJoke(String joke) {
        SharedPreferences sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        editor.putString("joke", joke);
        editor.apply();
    }

    public void loadJoke() {
        SharedPreferences sPref = getPreferences(MODE_PRIVATE);
        try {
            joke = sPref.getString("joke", defaultJoke);
        } catch (Exception e) {
            Log.e("Exception", e.toString());
        }
    }
}

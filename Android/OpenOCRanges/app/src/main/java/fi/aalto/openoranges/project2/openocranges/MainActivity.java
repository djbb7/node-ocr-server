package fi.aalto.openoranges.project2.openocranges;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static fi.aalto.openoranges.project2.openocranges.LoginActivity.JSON;

public class MainActivity extends AppCompatActivity {
    private ImageButton mReadButton;
    private Button mLogoutButton;

    //Variables for Logout
    private UserLogoutTask mAuthTask = null;
    private View mProgressView;
    private View mListView;
    private String mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialize fresco for downloading icon images
        Fresco.initialize(this);

        setContentView(R.layout.activity_main);

        mReadButton = (ImageButton) findViewById(R.id.read);
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, TakePictureActivity.class);
                startActivity(i);
                finish();
            }
        });

        mLogoutButton = (Button) findViewById(R.id.logout);
        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogout();
            }
        });

        mListView = findViewById(R.id.oo_AppsListView);
        mProgressView = findViewById(R.id.logout_progress);

    }

    //Logout activity
    private void attemptLogout() {
        if (mAuthTask != null) {
            return;
        }

        boolean cancel = false;
        View focusView = null;

        showProgress(true);
        mAuthTask = new UserLogoutTask();
        mAuthTask.execute((Void) null);
    }

//    public Response post(String url) throws IOException {
//        RequestBody body = RequestBody.create(JSON, "");
//        Request request = new Request.Builder()
//                .url(url)
//                .addHeader("Authorization", mToken)
//                .post(body)
//                .build();
//        return client.newCall(request).execute();
//    }

    /**
     * Represents an asynchronous logout task used to logout the user, make token invalid
     */
    public class UserLogoutTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                //String server_url = getString(R.string.server);
                //Response response = post(server_url + "users/logout");
                //int code = response.code();
                //if (code == 200) {
                    return true;
                //} else {
                  //  return false;
                //}
            } catch (Exception i) {
                i.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(MainActivity.this, "Logout successfully!", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Server connection failed!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }


    /**
     * Shows the progress bar
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
            mListView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mListView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}


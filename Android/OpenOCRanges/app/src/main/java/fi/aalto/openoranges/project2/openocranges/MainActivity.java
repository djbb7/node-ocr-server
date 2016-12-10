package fi.aalto.openoranges.project2.openocranges;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class MainActivity extends AppCompatActivity {
    private ImageButton mReadButton;
    private ImageButton mRefreshButton;
    private Button mLogoutButton;
    private TimeoutOperation mSleeper = null;
    private String mSleeperAction = null;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    //Variables for Logout
    private UserLogoutTask mAuthTask = null;
    private View mProgressView;
    private View mListView;
    private String mToken;

    private HistoryList mHistoryList = null;

    private ArrayList<JSONObject> arrays = null;
    private List<OcrResult> myOcrResultsList = new ArrayList<>();

    private int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context= getApplicationContext();
        OkHttpClient okHttpClient = new OkHttpClient();

        final OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        final Request.Builder requestBuilder = chain.request().newBuilder();
                        requestBuilder.addHeader("Authorization", mToken);
                        return chain.proceed(requestBuilder.build());
                    }
                });
        final ImagePipelineConfig imagePipelineConfig = OkHttpImagePipelineConfigFactory.newBuilder(
                getApplicationContext(),
                okHttpClientBuilder.build())
                .build();
     try {
         Fresco.initialize(context, imagePipelineConfig);
     }catch(Exception e){
         e.printStackTrace();
     }
        setContentView(R.layout.activity_main);


        //post Token from previous activity
        mToken = getIntent().getStringExtra("token");

        mReadButton = (ImageButton) findViewById(R.id.read);
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSleeperAction = "read";
                mSleeper = new TimeoutOperation();
                mSleeper.execute((Void) null);
                showProgress(true);
            }
        });


        mLogoutButton = (Button) findViewById(R.id.logout);
        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogout();
            }
        });

        mRefreshButton = (ImageButton) findViewById(R.id.refresh);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSleeperAction = "refresh";
                mSleeper = new TimeoutOperation();
                mSleeper.execute((Void) null);
                showProgress(true);
                mHistoryList = new HistoryList();
                mHistoryList.execute((Void) null);
            }
        });

        mListView = findViewById(R.id.oo_AppsListView);
        mProgressView = findViewById(R.id.logout_progress);

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            return;
        }
        showProgress(true);
        mHistoryList = new HistoryList();
        mHistoryList.execute((Void) null);

        registerClickCallback();
    }

    //Method for showing a history entry and register which entry is clicked
    private void registerClickCallback() {
        ListView list = (ListView) findViewById(R.id.oo_AppsListView);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {
                OcrResult clickedApp = myOcrResultsList.get(position);
                Intent i = new Intent(MainActivity.this, ShowActivity.class);
                i.putExtra("token", mToken);
                i.putExtra("timestamp", clickedApp.getCreatedAt());
                i.putExtra("text", clickedApp.getExtractedText());
                i.putExtra("imageUrl", clickedApp.getImageUrl());
                i.putExtra("mModus", "Remote");
                startActivity(i);
            }
        });
    }


    //adding the applications in the list to the ListView
    private void addingAppsToListView() {
        ArrayAdapter<OcrResult> adapter = new ListAdapter();
        ListView list = (ListView) findViewById(R.id.oo_AppsListView);
        list.setAdapter(adapter);
    }



    /**
     * class to add applications to the list
     */
    private class ListAdapter extends ArrayAdapter<OcrResult> {
        public ListAdapter() {
            super(MainActivity.this, R.layout.item_view, myOcrResultsList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Ensure to have a view which is not null
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            //finding app in the list on position
            OcrResult currentResult = myOcrResultsList.get(position);

            //Fill the view with the apps using inflater
            Uri uri = Uri.parse(getString(R.string.serverWithoutSlash) + "" + currentResult.getThumbnailUrl());
            SimpleDraweeView view = (SimpleDraweeView) itemView.findViewById(R.id.imageView);
            try {
                view.setImageURI(uri);
            } catch (Exception i) {
                i.printStackTrace();
            }


            //Fill the textview with the name of the app
            TextView nameText = (TextView) itemView.findViewById(R.id.nameText);
            String filename =  currentResult.getExtractedText();
            if(filename.equals("")){
                nameText.setText("No text recognized!");
            }
            else{
                nameText.setText(Html.fromHtml(filename));
            }
            return itemView;
        }
    }

    //fill arraylist with the results of the images, received from the server
    private void populatOcrList() {
        myOcrResultsList = new ArrayList<>();
        for (int j = 0; j < arrays.size(); j++) {
            String extractedText = null;
            Object error = null;
            String processingStarted = null;
            String processingFinished = null;
            Double processingTime = null;
            String thumbnailUrl = null;
            String imageUrl = null;
            String createdAt = null;

            try {
                error = arrays.get(j).get("error");
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                extractedText = arrays.get(j).getString("extractedText");
                processingStarted = arrays.get(j).getString("processingStarted");
                processingFinished = arrays.get(j).getString("processingFinished");
                processingTime = arrays.get(j).getDouble("processingTime");
                thumbnailUrl = arrays.get(j).getString("thumbnailUrl");
                imageUrl = arrays.get(j).getString("imageUrl");
                createdAt = arrays.get(j).getString("createdAt");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            myOcrResultsList.add(new OcrResult(extractedText, processingStarted, processingFinished, processingTime, thumbnailUrl, imageUrl, createdAt));

        }
        showProgress(false);
        addingAppsToListView();
    }

    //Sends a json request to the server and returns the response
    //receives as parameters a string which represents the url of the server
    public Response get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", mToken)
                .build();
        return client.newCall(request).execute();
    }

    public Response getList(String url) throws IOException {
        return get(url);
    }

    /**
     * Represents an asynchronous task used to post a list of applications from the server
     */
    public class HistoryList extends AsyncTask<Void, Void, ArrayList<JSONObject>> {

        @Override
        protected ArrayList<JSONObject> doInBackground(Void... params) {

            try {
                String server_url = getString(R.string.server) + "history";
                Response response = getList(server_url);
                int code = response.code();
                if (code == 200) {
                    try {
                        JSONObject myjson = new JSONObject(response.body().string().toString());

                        JSONArray the_json_array = myjson.getJSONArray("transactions");
                        int size = the_json_array.length();
                        arrays = new ArrayList<JSONObject>();
                        for (int i = 0; i < size; i++) {
                            JSONObject another_json_object = the_json_array.getJSONObject(i);

                            JSONArray ocrResultsArray = another_json_object.getJSONArray("files");


                            JSONObject json_results = ocrResultsArray.getJSONObject(0);

                            //merging the two JsonObjects
                            JSONObject mergedObj = new JSONObject();

                            Iterator i1 = another_json_object.keys();
                            Iterator i2 = json_results.keys();
                            String tmp_key;
                            while (i1.hasNext()) {
                                tmp_key = (String) i1.next();
                                mergedObj.put(tmp_key, another_json_object.get(tmp_key));
                            }
                            while (i2.hasNext()) {
                                tmp_key = (String) i2.next();
                                mergedObj.put(tmp_key, json_results.get(tmp_key));
                            }
                            arrays.add(mergedObj);
                        }
                        JSONObject[] json = new JSONObject[arrays.size()];
                        arrays.toArray(json);
                        return arrays;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    return arrays;
                }
            } catch (Exception i) {
                i.printStackTrace();
                return null;
            }
            return arrays;
        }


        protected void onPostExecute(ArrayList<JSONObject> list) {
            //checks if received list is empty or not
            if (list == null) {
                showProgress(false);
                Toast.makeText(MainActivity.this, "List could not be refreshed due to missing internet connection!", Toast.LENGTH_LONG).show();
            } else {
                mHistoryList = null;
                arrays = list;
                populatOcrList();
            }
        }

        @Override
        protected void onCancelled() {
            mHistoryList = null;

        }
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

    public Response post(String url) throws IOException {
        RequestBody body = RequestBody.create(JSON, "");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", mToken)
                .post(body)
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Represents an asynchronous logout task used to logout the user, make token invalid
     */
    public class UserLogoutTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String server_url = getString(R.string.server);
                Response response = post(server_url + "users/logout/");
                int code = response.code();
                if (code == 200) {
                    return true;
                } else {
                    return false;
                }
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

    //Permission handling
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {

                    } else {

                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                }
            }
        }
    }

    //Sleeper for refreshing function
    private class TimeoutOperation extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(mSleeperAction.equals("read")){
                Intent i = new Intent(MainActivity.this, TakePictureActivity.class);
                i.putExtra("token", mToken);
                startActivity(i);
            }
        }
    }

}


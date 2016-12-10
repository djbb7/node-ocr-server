package fi.aalto.openoranges.project2.openocranges;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShowActivity extends AppCompatActivity {

    private TextView mTextResult;
    private TextView mCreatedTime;
    private String mTimestamp;
    private ImageView mImageResult;
    private Button mBackToMain;
    private Button mRetake;
    private ImageButton mShowImage;
    private Button mSaveText;
    private TimeoutOperation mSleeper = null;
    private String mSleeperAction = null;

    private View mProgressView;
    private View mListView;
    private String mToken;
    private Uri mPictureUri;
    private String[] mPictureUriList;
    private String mText;
    private String mImageUrl;
    private int mClickCounter = 0;
    private String mModus;
    private ArrayList<? extends OcrResult> myOcrResultsList = new ArrayList<>();
    private ArrayList<OcrResult> myOcrResultsList_option = new ArrayList<>();

    public static final String path = Environment.getExternalStorageDirectory().toString() + "/OpenTxtFiles";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_show);

        //post Token from previous activity
        mToken = getIntent().getStringExtra("token");
        mText = getIntent().getStringExtra("text");
        mTimestamp = getIntent().getStringExtra("timestamp");
        mModus = getIntent().getStringExtra("mModus");
        mPictureUriList = getIntent().getStringArrayExtra("mPictureUriList");
        mImageUrl = getIntent().getStringExtra("imageUrl");
        myOcrResultsList = getIntent().getParcelableArrayListExtra("myOcrResultsList");
        myOcrResultsList_option = (ArrayList<OcrResult>) getIntent().getSerializableExtra("myOcrResultsList");

        //Finding ImageUri from server
        final Uri mImageUri = Uri.parse(getString(R.string.serverWithoutSlash) + "" + mImageUrl);

        initializeFresco();

        //Setting result for text
        mTextResult = (TextView) findViewById(R.id.textViewResult);
        if (mText.equals("")){
            mTextResult.setText("No text recognized!");
            mTextResult.setGravity(View.TEXT_ALIGNMENT_CENTER);
        }
        else{
        mTextResult.setText(mText);
        }
        mTextResult.setMovementMethod(new ScrollingMovementMethod());

        //Preparing view for source image
        mImageResult = (ImageView) findViewById(R.id.imageViewResult);
        mImageResult.setVisibility(View.INVISIBLE);

        //Displaying timestamp
        mCreatedTime = (TextView) findViewById((R.id.createdTime));
        mCreatedTime.setText("Created at: " + mTimestamp);

        mBackToMain = (Button) findViewById(R.id.backToMain);
        mBackToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ShowActivity.this, MainActivity.class);
                i.putExtra("token", mToken);
                startActivity(i);
                finish();
            }
        });

        //Button to retake picture
        mRetake = (Button) findViewById(R.id.retake);
        mRetake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSleeperAction = "read";
                mSleeper = new TimeoutOperation();
                mSleeper.execute((Void) null);
                showProgress(true);
            }
        });

        //Button to showing image
        mShowImage = (ImageButton) findViewById(R.id.showImage);
        mShowImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mClickCounter == 0){
                    //View for taken picture
                    mTextResult.setVisibility(View.INVISIBLE);
                    mImageResult.setVisibility(View.VISIBLE);

                    if (mPictureUriList != null){

                        if (mPictureUriList != null && mPictureUriList.length == 1) {
                            mPictureUri = Uri.parse(mPictureUriList[0]);
                            mImageResult.setImageURI(mPictureUri);
                        } else {
                            mPictureUri = Uri.parse(mPictureUriList[1]);
                            mImageResult.setImageURI(mPictureUri);
                        }

                    } else if( mImageUri == null){
                        mImageResult.setImageResource(R.drawable.no_image);
                    } else{
                        mImageResult.setImageURI(mImageUri);
                    }
                    mClickCounter = 1;
                    mShowImage.setImageResource(R.drawable.show_text);
                } else{
                    mTextResult.setVisibility(View.VISIBLE);
                    mImageResult.setVisibility(View.INVISIBLE);
                    mShowImage.setImageResource(R.drawable.icon_ocranges_white);
                    mClickCounter = 0;
                }
            }
        });

        mSaveText = (Button) findViewById(R.id.saveTxt);
        mSaveText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSleeperAction = "save";
                mSleeper = new TimeoutOperation();
                mSleeper.execute((Void) null);
                showProgress(true);

                //Creates a text file properly but the textfile is empty...need to be fixed
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File file = new File(path + "/" + timeStamp + ".txt");
                String[] text = new String[1];
                text[0] = String.valueOf(mText);
                try {
                    save(file, text);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mListView = findViewById(R.id.mainView);
        mProgressView = findViewById(R.id.save_progress);
    }

    public void initializeFresco(){
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
            Fresco.initialize(getApplicationContext(), imagePipelineConfig);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void save(File file, String[] data) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            try {
                for (int i = 0; i < data.length; i++) {
                    fos.write(data[i].getBytes());
                    if (i < data.length - 1) {
                        fos.write("\n".getBytes());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                Intent i = new Intent(ShowActivity.this, TakePictureActivity.class);
                i.putExtra("token", mToken);
                startActivity(i);
            }
            else{
                showProgress(false);
                Toast.makeText(ShowActivity.this, "Textfile saved", Toast.LENGTH_SHORT).show();
            }
        }
    }

}

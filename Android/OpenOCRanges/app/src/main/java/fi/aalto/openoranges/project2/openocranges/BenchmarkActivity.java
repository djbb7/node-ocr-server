package fi.aalto.openoranges.project2.openocranges;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BenchmarkActivity extends AppCompatActivity {

    private TextView mTextResult;
    private Button mBackToMain;
    private TimeoutOperation mSleeper = null;
    private int mNumberImages;
    private double mTimeLocal;
    private double mTimeRemote;
    private double mTimeBenchmark;
    private long[] mTimeSingleLocal;
    private double[] mTimeSingleRemote;
    private int[] mByteSingleRemote;
    private double mTimeFastLocal;
    private double mTimeSlowLocal;
    private int mPictureFastLocal;
    private int mPictureSlowLocal;
    private double mMeanTimeLocal;
    private int mPictureFastRemote;
    private int mPictureSlowRemote;
    private double mTimeFastRemote;
    private double mTimeSlowRemote;
    private double mMeanTimeRemote;
    private int mBytesMin;
    private int mBytesMax;
    private int mPictureMax;
    private int mPictureMin;
    private int mDataExchange;
    private int mMeanDataExchange;


    private View mProgressView;
    private View mListView;
    private String mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_benchmark);

        //post Token from previous activity
        mToken = getIntent().getStringExtra("token");
        mNumberImages = getIntent().getIntExtra("processedImages",0);
        mTimeLocal = getIntent().getDoubleExtra("localProcessingTime", 9999);
        mTimeRemote = getIntent().getDoubleExtra("remoteProcessingTime", 9999);
        mTimeBenchmark = getIntent().getDoubleExtra("benchmarkProcessingTime", 9999);

        try {
            mTimeSingleLocal = getIntent().getLongArrayExtra("localSingleTimes");
            mTimeSingleRemote = getIntent().getDoubleArrayExtra("remoteSingleTimes");
            mByteSingleRemote = getIntent().getIntArrayExtra("remoteSingleBytes");
        }
        catch (Exception e) {

        }


        if(mTimeSingleLocal != null){
        int size = mTimeSingleLocal.length;
        for (int i = 0; i < size; i++) {
            if(i==0){
                mPictureFastLocal = i;
                mPictureSlowLocal = i;
                mTimeFastLocal = mTimeSingleLocal[i];
                mTimeSlowLocal = mTimeSingleLocal[i];
            }
            else{
                if(mTimeSingleLocal[i] < mTimeFastLocal){
                    mPictureFastLocal = i;
                    mTimeFastLocal = mTimeSingleLocal[i];
                }
                else if(mTimeSingleLocal[i] > mTimeSlowLocal){
                    mPictureFastLocal = i;
                    mTimeSlowLocal = mTimeSingleLocal[i];
                }
            }
        }

        mMeanTimeLocal = mTimeLocal/size;}

        else{
            mMeanTimeLocal = mTimeLocal;
            mTimeFastLocal = mTimeLocal;
            mTimeSlowLocal = mTimeLocal;
        }

        if(mTimeSingleRemote != null) {
            int size = mTimeSingleRemote.length;
            for (int i = 0; i < size; i++) {
                if (i == 0) {
                    mPictureFastRemote = i;
                    mPictureSlowRemote = i;
                    mTimeFastRemote = mTimeSingleRemote[i];
                    mTimeSlowRemote = mTimeSingleRemote[i];
                } else {
                    if (mTimeSingleRemote[i] < mTimeFastRemote) {
                        mPictureFastRemote = i;
                        mTimeFastRemote = mTimeSingleLocal[i];
                    } else if (mTimeSingleRemote[i] > mTimeSlowRemote) {
                        mPictureFastRemote = i;
                        mTimeSlowRemote = mTimeSingleRemote[i];
                    }
                }
            }
            mMeanTimeRemote = mTimeRemote/size;
        }

        if(mByteSingleRemote != null) {
            int size = mByteSingleRemote.length;
            for (int i = 0; i < size; i++) {
                if (i == 0) {
                    mPictureMin = i;
                    mPictureMax = i;
                    mBytesMax = mByteSingleRemote[i];
                    mBytesMin = mByteSingleRemote[i];
                } else {
                    if (mByteSingleRemote[i] < mBytesMin) {
                        mPictureMin = i;
                        mBytesMin = mByteSingleRemote[i];
                    } else if (mByteSingleRemote[i] > mBytesMax) {
                        mPictureMax = i;
                        mBytesMax = mByteSingleRemote[i];
                    }
                }
                mDataExchange = mDataExchange + mByteSingleRemote[i];
            }
            mMeanDataExchange = mDataExchange/size;
        }

        mTextResult = (TextView) findViewById(R.id.Benchmarking);
        //Setting benchmark text with variables
        mTextResult.setText("\nNumber of processed images: " + mNumberImages + "\n" +
                "\n" +
                "Local\n" +
                "Processing time: " + mTimeLocal + " (" + mMeanTimeLocal + ") ms\n" +
                "Minimum: " + mTimeFastLocal + " ms (" + mPictureFastLocal + "); Maximum: " + mTimeSlowLocal + " (" + mPictureSlowLocal + ")\n" +
                "\n" +
                "Remote\n" +
                "Processing time: " + mTimeRemote + " (" + mMeanTimeRemote + ") ms\n" +
                "Minimum: " + mTimeFastRemote*1000 + " ms (" + mPictureFastRemote + "); Maximum: " + mTimeSlowRemote*1000 + " (" + mPictureSlowRemote + ")\n" +
                "Exchanged data: " + mDataExchange + " (" + mMeanDataExchange + ") bytes\n" +
                "Minimum: " + mBytesMin + " bytes (" + mPictureMin + "); Maximum: " + mBytesMax + " bytes (" + mPictureMax + ")");

        mBackToMain = (Button) findViewById(R.id.backToMain);
        mBackToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(BenchmarkActivity.this, MainActivity.class);
                i.putExtra("token", mToken);
                startActivity(i);
                finish();
            }
        });

        mListView = findViewById(R.id.Benchmarking);
        mProgressView = findViewById(R.id.save_progress);
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
            showProgress(false);
            Toast.makeText(BenchmarkActivity.this, "Textfile saved", Toast.LENGTH_SHORT).show();
        }
    }

}

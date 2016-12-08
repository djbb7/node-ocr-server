package fi.aalto.openoranges.project2.openocranges;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LocalActivity extends AppCompatActivity {

    private TextView mTextResult;
    private ImageView mImageResult;
    private Button mBackToMain;
    private Button mSaveText;
    private TimeoutOperation mSleeper = null;

    private View mProgressView;
    private View mListView;
    private String mToken;
    private Uri mPictureUri;
    private String[] mPictureUriList;
    private String mText;
    public static final String path = Environment.getExternalStorageDirectory().toString() + "/OpenTxtFiles";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_local);

        //post Token from previous activity
        mToken = getIntent().getStringExtra("token");
        mText = getIntent().getStringExtra("text");

        //View for taken picture
        mImageResult = (ImageView) findViewById(R.id.imageViewResult);
        if (mPictureUriList != null && mPictureUriList.length == 1) {
            mPictureUri = Uri.parse(mPictureUriList[0]);
            mImageResult.setImageURI(mPictureUri);
        } else {
            mPictureUri = Uri.parse(mPictureUriList[1]);
            mImageResult.setImageURI(mPictureUri);
        }

        mTextResult = (TextView) findViewById(R.id.textViewResult);
        //Setting benchmark text with variables
        mTextResult.setText(mText);

        mBackToMain = (Button) findViewById(R.id.backToMain);
        mBackToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(LocalActivity.this, MainActivity.class);
                i.putExtra("token", mToken);
                startActivity(i);
                finish();
            }
        });


        mSaveText = (Button) findViewById(R.id.saveTxt);
        mSaveText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**This is the code how it was described in the video:https://www.youtube.com/watch?v=x3pyyQbwLko
                 * but it is not working...

                 String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                 File file = new File(path + "/" + timeStamp + ".txt");
                 try {
                 String [] saveText = String.valueOf(textView.getText()).split(System.getProperty("line.seperator"));
                 save(file, saveText);
                 } catch (Exception e) {
                 e.printStackTrace();
                 }
                 */

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

                File root = new File(path);
                File gpxfile = new File(root, "yourFileName.txt");
                try {
                    FileWriter writer = new FileWriter(gpxfile);
                    writer.append(mText);
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                mListView = findViewById(R.id.ResultView);
                mProgressView = findViewById(R.id.save_progress);
            }
        });
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
            showProgress(false);
            Toast.makeText(LocalActivity.this, "Textfile saved", Toast.LENGTH_SHORT).show();
        }
    }

}

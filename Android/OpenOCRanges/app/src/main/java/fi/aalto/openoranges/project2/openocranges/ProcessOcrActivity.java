package fi.aalto.openoranges.project2.openocranges;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProcessOcrActivity extends Activity {

    private Button mRetake;
    private Button mProcessOcr;

    private CropImageView mPictureView;
    private Uri mPictureUri;
    private String[] mPictureUriList;
    private TessOCR mTessOCR;
    private static final String TAG = "ProcessOcrActivity";
    private TextView textView;
    public static final String lang = "eng";
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/OpenOCRanges/";
    public static final String path = Environment.getExternalStorageDirectory().toString() + "/OpenTxtFiles";

    private String mToken;
    private String mModus;
    private String mText;

    private uploadImagesTask mUploadImagesTask = null;

    private String mTransactionID;

    private View mProgressView;
    private View mListView;

    //For the communication with the server
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processocr);

        textView = (TextView) findViewById(R.id.editText_view);
        try {
            mPictureUriList = getIntent().getStringArrayExtra("mPictureUriList");
        } catch (Exception e) {

        }

        //get Token from previous activity
        mToken = getIntent().getStringExtra("token");
        mModus = getIntent().getStringExtra("mModus");

        //View for taken picture
        mPictureView = (CropImageView) findViewById(R.id.picture_view);
        if (getIntent().getStringExtra("mOrientation").equals("1")) {
            mPictureUri = Uri.parse(getIntent().getStringExtra("mPictureUri"));
            mPictureView.setImageUriAsync(mPictureUri);
        } else if (mPictureUriList != null && mPictureUriList.length == 1) {

            mPictureUri = Uri.parse(mPictureUriList[0]);
            mPictureView.setImageUriAsync(mPictureUri);
        } else {
            mPictureUri = Uri.parse(mPictureUriList[1]);
            mPictureView.setImageUriAsync(mPictureUri);
        }

        String[] paths = new String[]{DATA_PATH, DATA_PATH + "tessdata/"};

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v("Main", "ERROR: Creation of directory " + path + " on sdcard failed");
                    break;
                } else {
                    Log.v("Main", "Created directory " + path + " on sdcard");
                }
            }

        }
        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();

                InputStream in = assetManager.open(lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                // Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                // Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }

        mTessOCR = new TessOCR();


        //Button to retake picture
        mRetake = (Button) findViewById(R.id.Retake);
        mRetake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ProcessOcrActivity.this, TakePictureActivity.class);
                startActivity(i);
                finish();
            }
        });


        //Button to process OCR
        mProcessOcr = (Button) findViewById(R.id.ProcessOcr);
        mProcessOcr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if remote modus is chosen do ocr processing on server
                if (mModus.equals("Remote")) {
                    showProgress(true);
                    mUploadImagesTask = new uploadImagesTask();
                    mUploadImagesTask.execute((Void) null);
                }
                else if (mModus.equals("Local")) {
                    showProgress(true);
                    try {
                        Bitmap cropped = mPictureView.getCroppedImage(500, 500);
                        if (cropped != null)
                            mPictureView.setImageBitmap(cropped);

                        //mImage.setImageBitmap(converted);
                        doOCR(convertColorIntoBlackAndWhiteImage(cropped));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else{
                    //Not possible
                }

            }
        });

        //Creates Directory for saving the text files from OCR
        File dir = new File(path);
        dir.mkdirs();

        mListView = findViewById(R.id.ResultView);
        mProgressView = findViewById(R.id.load_progress);
    }


    public void doOCR(final Bitmap bitmap) {
        Thread t = new Thread(new Runnable() {
            public void run() {

                final String result = mTessOCR.getOCRResult(bitmap).toLowerCase();

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if (result != null && !result.equals("")) {
                            String s = result.trim();
                            mText = result;
                            Intent i = new Intent(ProcessOcrActivity.this, LocalActivity.class);
                            i.putExtra("mPictureUri", mPictureUri.toString());
                            i.putExtra("token", mToken);
                            i.putExtra("text", mText);
                            startActivity(i);
                            finish();
                        }
                        else {
                            textView.setText("Any text could be identified. Please retake picture or zoom closer to text!");
                        }

                        showProgress(false);
                    }

                });

            }

            ;
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private Bitmap convertColorIntoBlackAndWhiteImage(Bitmap orginalBitmap) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);

        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(
                colorMatrix);

        Bitmap blackAndWhiteBitmap = orginalBitmap.copy(
                Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        paint.setColorFilter(colorMatrixFilter);

        Canvas canvas = new Canvas(blackAndWhiteBitmap);
        canvas.drawBitmap(blackAndWhiteBitmap, 0, 0, paint);

        return blackAndWhiteBitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = getPickImageResultUri(data);

            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            boolean requirePermissions = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    isUriRequiresPermissions(imageUri)) {

                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true;
                mPictureUri = imageUri;
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }

            if (!requirePermissions) {
                mPictureView.setImageUriAsync(imageUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mPictureUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mPictureView.setImageUriAsync(mPictureUri);
        } else {
            Toast.makeText(this, "Required permissions are not granted", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Create a chooser intent to select the source to get image from.<br/>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br/>
     * All possible sources are added to the intent chooser.
     */
    public Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    /**
     * Get URI to image received from capture by camera.
     */
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }

    /**
     * Get the URI of the selected image from {@link #getPickImageChooserIntent()}.<br/>
     * Will return the correct URI for camera and gallery image.
     *
     * @param data the returned data of the activity result
     */
    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null && data.getData() != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    /**
     * Test if we can open the given Android URI to test if permission required error is thrown.<br>
     */
    public boolean isUriRequiresPermissions(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            stream.close();
            return false;
        } catch (FileNotFoundException e) {
            if (e.getCause() instanceof ErrnoException) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    //Sends a json request to the server and returns the response
    //receives as parameters a string which represents the url of the server
    public Response get(String url, Uri uri) throws IOException {
        RequestBody req = null;
        try {
            final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/jpeg");
            File file = new File(uri.getPath());
            req = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("photos[]", file.getName(), RequestBody.create(MEDIA_TYPE_PNG, file)).build();
        } catch (Exception i) {
            i.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", mToken)
                .post(req)
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Represents an asynchronous task used to get the address of the chosen application
     */
    public class uploadImagesTask extends AsyncTask<Void, Void, Boolean> {

        private boolean isInternetAvailable = true;


        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String server_url = getString(R.string.server);
                Response response = ProcessOcrActivity.this.get(server_url + "ocr/upload/", mPictureUri);
                int code = response.code();

                //if the code is 200 than everything is okay and vnc viewer can start
                //if the code is 202 wait for the VM to get started
                if (code == 200) {
                    final JSONObject myjson = new JSONObject(response.body().string().toString());
                    mTransactionID=  myjson.getJSONObject("transaction").get("id").toString();

                    return true;
                } else {
                    return false;
                }
            } catch (Exception i) {
                isInternetAvailable = false;
                i.printStackTrace();
                return false;
            }
        }


        protected void onPostExecute(Boolean success) {
            mUploadImagesTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(ProcessOcrActivity.this, "Transaction successfully processed!", Toast.LENGTH_LONG).show();
                Intent i = new Intent(ProcessOcrActivity.this, MainActivity.class);
                i.putExtra("token", mToken);
                finish();

            }
            else {
                if (isInternetAvailable == false) {
                    Toast.makeText(ProcessOcrActivity.this, "No processing due to missing internet connection!", Toast.LENGTH_LONG).show();
                    //mLogoutButton.setEnabled(true);
                    //mRefreshButton.setEnabled(true);
                } else {
                    Toast.makeText(ProcessOcrActivity.this, "FAILURE! Please try it again...", Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mUploadImagesTask = null;
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

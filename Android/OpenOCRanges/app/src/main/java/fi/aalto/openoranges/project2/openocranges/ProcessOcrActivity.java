package fi.aalto.openoranges.project2.openocranges;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;
import android.provider.OpenableColumns;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private Button mAddPicture;
    private Button mProcessOcr;

    private ImageView mPictureView;
    private TextView mMultipleImagesView;
    private Uri mPictureUri;
    private String[] mPictureUriList;
    private TessOCR mTessOCR;
    private String mTimestamp;
    private static final String TAG = "ProcessOcrActivity";
    public static final String lang = "eng";
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/OpenOCRanges/";
    public static final String path = Environment.getExternalStorageDirectory().toString() + "/OpenTxtFiles";
    public static final String path_images = Environment.getExternalStorageDirectory().toString() + "/UploadedImages";

    private String mToken;
    private String mModus;

    private uploadImagesTask mUploadImagesTask = null;
    private getResultsTask mGetResultsTask = null;

    private String mTransactionID;
    private String href;

    private String extractedText_multipleImages_local = "";

    private Boolean lastImageToProcess_multiple_local;

    private View mProgressView;
    private View mListView;

    private Boolean localOcrDone;
    private Boolean remoteOcrDone;

    private long remoteStart;
    private long remoteFinished;
    private double remoteDelta;
    private long localIntermediate;
    private long[] localSingleTimes;
    private double[] remoteSingleTimes;
    private int[] remoteSingleBytes;

    private long localStart;
    private long localFinished;
    private double localDelta;


    private long benchmarkStart;
    private long benchmarkFinished;
    private double benchmarkDelta;


    //For the communication with the server
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    private ArrayList<JSONObject> arrays = null;
    private List<OcrResult> myOcrResultsList = new ArrayList<>();

    String text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processocr);

        try {
            mPictureUriList = getIntent().getStringArrayExtra("mPictureUriList");
        } catch (Exception e) {

        }


        //post Token from previous activity
        mToken = getIntent().getStringExtra("token");
        mModus = getIntent().getStringExtra("mModus");


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

        //Creates Directory for saving the text files from OCR
        File dir = new File(path);
        dir.mkdirs();

        //View for taken picture
        mPictureView = (ImageView) findViewById(R.id.picture_view);
        mMultipleImagesView = (TextView) findViewById(R.id.multiple_images);
        if (getIntent().getStringExtra("mOrientation").equals("1")) {
            mPictureUri = Uri.parse(getIntent().getStringExtra("mPictureUri"));
            mPictureUriList = new String[1];
            mPictureUriList[0] = mPictureUri.toString();
            mPictureView.setImageURI(mPictureUri);

        } else if (mPictureUriList != null && mPictureUriList.length == 1) {
            mPictureUri = Uri.parse(mPictureUriList[0]);
            mPictureView.setImageURI(mPictureUri);
        } else if (mPictureUriList != null && mPictureUriList.length > 1){
            mMultipleImagesView.setText(mPictureUriList.length + " images selected\n"+"Modus: "+mModus);
            mMultipleImagesView.setVisibility(View.VISIBLE);
            mPictureView.setVisibility(View.INVISIBLE);
        }
        else{
            mPictureUri = Uri.parse(mPictureUriList[0]);
            mPictureView.setImageURI(mPictureUri);
        }


        //Button to retake picture
        mRetake = (Button) findViewById(R.id.Retake);
        mRetake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ProcessOcrActivity.this, TakePictureActivity.class);
                i.putExtra("token", mToken);
                i.putExtra("mode", mModus);
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
                    Toast.makeText(ProcessOcrActivity.this, "Remote OCR processing...", Toast.LENGTH_LONG).show();
                    showProgress(true);
                    mRetake.setEnabled(false);
                    mProcessOcr.setEnabled(false);
                    mUploadImagesTask = new uploadImagesTask();
                    mUploadImagesTask.execute((Void) null);
                } else if (mModus.equals("Local")) {
                    try {
                        //if User selects only one Picture do this otherwise go to else-part
                        if (mPictureUriList.length < 2) {
                            doOCR();
                        } else {
                            lastImageToProcess_multiple_local = false;

                            doOCRMultiple();

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (mModus.equals("Benchmark")) {
                    Toast.makeText(ProcessOcrActivity.this, "Benchmarking started...", Toast.LENGTH_LONG).show();
                    showProgress(true);
                    mRetake.setEnabled(false);
                    mProcessOcr.setEnabled(false);
                    benchmarkStart = System.currentTimeMillis();
                    doBenchmark();
                }

            }
        });

        mTessOCR = new TessOCR();

        mProgressView = (View) findViewById(R.id.load_progress);
        mListView = (View) findViewById(R.id.main_screen);
    }

    public void startBenchmarkActivity() {
        if (remoteOcrDone && localOcrDone) {
            benchmarkFinished = System.currentTimeMillis();
            benchmarkDelta = (benchmarkFinished-benchmarkStart);
            Intent i = new Intent(ProcessOcrActivity.this, BenchmarkActivity.class);
            i.putExtra("mModus", mModus);
            i.putExtra("token", mToken);
            //i.putExtra("processedImages", mPictureUriList.length);
            i.putExtra("remoteSingleTimes", remoteSingleTimes);
            i.putExtra("remoteSingleBytes", remoteSingleBytes);
            i.putExtra("processedImages", mPictureUriList.length);
            i.putExtra("mPictureUriList", mPictureUriList);
            i.putExtra("localSingleTimes", localSingleTimes);
            i.putExtra("localProcessingTime", localDelta);
            i.putExtra("benchmarkProcessingTime", benchmarkDelta);
            i.putExtra("remoteProcessingTime", remoteDelta);
            startActivity(i);
            finish();
        }
    }

    public void doBenchmark() {
        remoteStart = System.currentTimeMillis();

        remoteOcrDone = false;
        localOcrDone = false;

        //start remote OCR process
        mUploadImagesTask = new uploadImagesTask();
        mUploadImagesTask.execute((Void) null);

        localStart = System.currentTimeMillis();
        try {
            //if User selects only one Picture do this otherwise go to else-part
            if (mPictureUriList.length < 2) {
                doOCR();
            } else {
                lastImageToProcess_multiple_local = false;
                doOCRMultiple();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    /*    Thread t = new Thread(new Runnable() {
            public void run() {


                remoteStart = System.currentTimeMillis();
                //start remote OCR process
                mUploadImagesTask = new uploadImagesTask();
                mUploadImagesTask.execute((Void) null);

                localStart = System.currentTimeMillis();
                try {
                    //if User selects only one Picture do this otherwise go to else-part
                    if (mPictureUriList.length < 2) {
                        Bitmap cropped = mPictureView.getCroppedImage(500, 500);
                        if (cropped != null) {
                            mPictureView.setImageBitmap(cropped);
                        }
                        doOCR(convertColorIntoBlackAndWhiteImage(cropped));
                    } else {
                        lastImageToProcess_multiple_local = false;
                        doOCRMultiple();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        try {
            t.join();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
*/
    }

    public void doOCR() {
        showProgress(true);
        mRetake.setEnabled(false);
        mProcessOcr.setEnabled(false);

        Thread t = new Thread(new Runnable() {
            public void run() {

               // File f = new File(saveURItoTmpFile(mPictureUri, 0));

                InputStream is = null;
                Bitmap bitmap = null;
                try {
                    is = getContentResolver().openInputStream(mPictureUri);
                    bitmap = BitmapFactory.decodeStream(is);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                final Bitmap bitm = bitmap;

                final String result = mTessOCR.getOCRResult(bitm).toLowerCase();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        mPictureView.setImageBitmap(bitm);

                        if (result != null) {
                            extractedText_multipleImages_local += result;

                            if (mModus.equals("Local")) {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                mTimestamp = dateFormat.format(new Date());
                                Intent i = new Intent(ProcessOcrActivity.this, ShowActivity.class);
                                i.putExtra("mModus", mModus);
                                i.putExtra("token", mToken);
                                i.putExtra("timestamp", mTimestamp);
                                i.putExtra("text", result);
                                i.putExtra("mPictureUriList", mPictureUriList);
                                lastImageToProcess_multiple_local = false;

                                startActivity(i);
                                finish();


                            } else {
                                extractedText_multipleImages_local = extractedText_multipleImages_local + result;
                                localFinished = System.currentTimeMillis();
                                localDelta = (localFinished - localStart);
                                localOcrDone = true;
                                startBenchmarkActivity();
                            }
                        }
                        else {
                            showProgress(false);
                            mRetake.setEnabled(true);
                            mProcessOcr.setEnabled(true);
                        }
                    }

                });

            }
        });
        t.start();
    }

    public void doOCRMultiple() {
        showProgress(true);

        mRetake.setEnabled(false);
        mProcessOcr.setEnabled(false);
        lastImageToProcess_multiple_local = false;
        localIntermediate = localStart;
        int size = mPictureUriList.length;
        localSingleTimes = new long[mPictureUriList.length];
        String[] list = new String[mPictureUriList.length];
        for (int i = 0; i < size; i++) {
            if (i + 1 == size) {
                lastImageToProcess_multiple_local = true;
            }
            File f;
            if (mPictureUriList[i].startsWith("content://")) {

                f = new File(saveURItoTmpFile(mPictureUriList[i], i));

                list[i] = f.toURI().toString();
            }

            Bitmap bitm = BitmapFactory.decodeFile(Uri.parse(list[i]).getPath());
            mPictureView.setImageBitmap(bitm);
            final Bitmap bitmap = convertColorIntoBlackAndWhiteImage(bitm);

            String result = mTessOCR.getOCRResult(bitmap).toLowerCase();
            extractedText_multipleImages_local = extractedText_multipleImages_local + result;

                if (mModus.equals("Local")) {
                    if (lastImageToProcess_multiple_local == true) {
                        lastImageToProcess_multiple_local = false;

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        mTimestamp = dateFormat.format(new Date());

                        Intent j = new Intent(ProcessOcrActivity.this, ShowActivity.class);
                        j.putExtra("mModus", mModus);
                        j.putExtra("token", mToken);
                        j.putExtra("timestamp",mTimestamp);
                        j.putExtra("text", extractedText_multipleImages_local);
                        j.putExtra("mPictureUriList", mPictureUriList);
                        startActivity(j);
                        finish();
                    }
                } else {
                    if (lastImageToProcess_multiple_local == true) {
                        lastImageToProcess_multiple_local = false;
                        localFinished = System.currentTimeMillis();
                        localDelta = (localFinished - localStart);
                        Toast.makeText(this, "Local OCR has finished!", Toast.LENGTH_SHORT).show();
                        localOcrDone = true;
                        localSingleTimes[i] = (localFinished-localIntermediate);
                        startBenchmarkActivity();
                    }
                    else{
                        localFinished = System.currentTimeMillis();
                        localSingleTimes[i] = (localFinished-localIntermediate);
                        localIntermediate = System.currentTimeMillis();
                    }
                }

        }
    }

    private String saveURItoTmpFile(String contentURI, int i){
        Uri uri = Uri.parse(contentURI);

        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        File targetFile = new File(getCacheDir().getAbsolutePath() + "/targetFile" + i + ".tmp");
        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(targetFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        try {
            while ((bytesRead = is.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            is.close();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getCacheDir().getAbsolutePath() + "/targetFile" + i + ".tmp";
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
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we post error.
            boolean requirePermissions = false;

            if (!requirePermissions) {
                mPictureView.setImageURI(imageUri);
            }
        }
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
     * Get the URI of the selected image from
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
    public Response post(String url, String[] list) throws IOException {

        final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

        MultipartBody requestBody = null;

        ArrayList<File> files = new ArrayList();
        try {
            MultipartBody.Builder buildernew = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);   //Here you can add the fix number of data.

            for (int i = 0; i < list.length; i++) {
                Uri uri = Uri.parse(list[i]);

                File f;
                if (uri.toString().startsWith("content://")) {
                    InputStream is = getContentResolver().openInputStream(uri);

                    File targetFile = new File(getCacheDir().getAbsolutePath() + "/targetFile" + i + ".tmp");
                    OutputStream outStream = new FileOutputStream(targetFile);

                    byte[] buffer = new byte[8 * 1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                    is.close();
                    outStream.close();
                    f = new File(getCacheDir().getAbsolutePath() + "/targetFile" + i + ".tmp");
                    files.add(f);
                } else {
                    f = new File(uri.getPath());
                }

                buildernew.addFormDataPart("photos[]", f.getName(), RequestBody.create(MEDIA_TYPE_JPEG, f));
            }

            requestBody = buildernew.build();
        } catch (Exception i) {
            i.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", mToken)
                .post(requestBody)
                .build();
        Response result = client.newCall(request).execute();

        // clean up
        for (File file : files) {
            file.delete();
        }

        return result;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Represents an asynchronous task used to post the address of the chosen application
     */
    public class uploadImagesTask extends AsyncTask<Void, Void, Boolean> {

        private boolean isInternetAvailable = true;
        private String message = "Application can not be opened due to missing internet connection!";

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String server_url = getString(R.string.server);
                Response response = ProcessOcrActivity.this.post(server_url + "ocr/upload/", mPictureUriList);
                int code = response.code();

                //if the code is 200 than everything is okay and server received images and processes them
                if (code == 200) {
                    final JSONObject myjson = new JSONObject(response.body().string().toString());
                    mTransactionID = myjson.getJSONObject("transaction").get("id").toString();
                    href = myjson.getJSONObject("transaction").get("href").toString();

                    return true;
                } else if (code == 401) {
                    message = "Invalid Token! Session expired!";
                    return false;
                } else {
                    return false;
                }
            } catch (Exception i) {
                i.printStackTrace();
                return false;
            }
        }


        protected void onPostExecute(Boolean success) {
            mUploadImagesTask = null;

            if (success) {

                mGetResultsTask = new getResultsTask();
                mGetResultsTask.execute((Void) null);
                Toast.makeText(ProcessOcrActivity.this, "Processing image...", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(ProcessOcrActivity.this, message, Toast.LENGTH_LONG).show();
                mRetake.setEnabled(true);
                mProcessOcr.setEnabled(true);
                showProgress(false);
            }
        }

        @Override
        protected void onCancelled() {
            mUploadImagesTask = null;
            showProgress(false);
            mRetake.setEnabled(true);
            mProcessOcr.setEnabled(true);
        }
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

    /**
     * Represents an asynchronous task used to post the address of the chosen application
     */
    public class getResultsTask extends AsyncTask<Void, Void, ArrayList<JSONObject>> {

        @Override
        protected ArrayList<JSONObject> doInBackground(Void... params) {
            int counter = 0;
            try {
                String server_url = getString(R.string.serverWithoutSlash);
                Response response = ProcessOcrActivity.this.get(server_url + href);
                int code = response.code();

                //if the code is 200 than everything is okay
                //if the code is 202 wait for the server to process images
                if (code == 200) {
                    try {
                        JSONObject myjson = new JSONObject(response.body().string().toString());
                        text = myjson.getString("extractedText");
                        mTimestamp = myjson.getString("createdAt");
                        JSONArray the_json_array = myjson.getJSONArray("files");
                        int size = the_json_array.length();
                        arrays = new ArrayList<JSONObject>();
                        remoteSingleTimes = new double[size];
                        remoteSingleBytes = new int[size];
                        for (int i = 0; i < size; i++) {
                            JSONObject json_results = the_json_array.getJSONObject(i);
                            remoteSingleBytes[i] = json_results.toString().length() + getFileSizeFromUri(mPictureUriList[i]);
                            remoteSingleTimes[i] = Double.parseDouble(json_results.getString("processingTime"));
                            arrays.add(json_results);
                        }
                        JSONObject[] json = new JSONObject[arrays.size()];
                        arrays.toArray(json);
                        return arrays;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return arrays;
                } else if (code == 202) {
                    //waiting for the server to process images
                    while (counter < 6) {
                        Thread.sleep(4000);
                        response = ProcessOcrActivity.this.get(server_url + href);
                        if (response.code() == 200) {
                            try {
                                JSONObject myjson = new JSONObject(response.body().string().toString());
                                text = myjson.getString("extractedText");
                                mTimestamp = myjson.getString("createdAt");
                                JSONArray the_json_array = myjson.getJSONArray("files");
                                int size = the_json_array.length();
                                arrays = new ArrayList<JSONObject>();
                                remoteSingleTimes = new double[size];
                                remoteSingleBytes = new int[size];
                                for (int i = 0; i < size; i++) {
                                    JSONObject json_results = the_json_array.getJSONObject(i);
                                    remoteSingleBytes[i] = json_results.toString().length() + getFileSizeFromUri(mPictureUriList[i]);
                                    remoteSingleTimes[i] = Double.parseDouble(json_results.getString("processingTime"));
                                    arrays.add(json_results);
                                }
                                JSONObject[] json = new JSONObject[arrays.size()];
                                arrays.toArray(json);
                                return arrays;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return arrays;
                        } else if (response.code() == 202) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(ProcessOcrActivity.this, "Still waiting for the Server to process!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else if (response.code() == 401) {
                            return arrays;
                        }
                        counter++;
                        if (counter >= 6) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(ProcessOcrActivity.this, "Time exceeded, we are sorry!", Toast.LENGTH_SHORT).show();
                                    mRetake.setEnabled(true);
                                    mProcessOcr.setEnabled(true);
                                    showProgress(false);
                                }
                            });
                            return arrays;
                        }
                    }
                    return arrays;
                } else {
                    return arrays;
                }
            } catch (Exception i) {
                i.printStackTrace();
                return arrays;
            }
        }

        protected void onPostExecute(ArrayList<JSONObject> list) {
            mGetResultsTask = null;

            //checks if received list is empty or not
            if (list == null) {
                Toast.makeText(ProcessOcrActivity.this, "Results are not available due to missing internet connection!", Toast.LENGTH_LONG).show();
                mRetake.setEnabled(true);
                mProcessOcr.setEnabled(true);
                showProgress(false);
            } else {
                Toast.makeText(ProcessOcrActivity.this, "OCR SUCCESS", Toast.LENGTH_LONG).show();
                arrays = list;
                populatOcrList();

                if (mModus.equals("Benchmark")) {
                    Toast.makeText(ProcessOcrActivity.this, "Benchmark: Remote OCR has finished!", Toast.LENGTH_LONG).show();
                    remoteFinished = System.currentTimeMillis();
                    remoteDelta = (remoteFinished - remoteStart);
                    remoteOcrDone = true;
                    startBenchmarkActivity();
                } else {
                    Intent i = new Intent(ProcessOcrActivity.this, ShowActivity.class);
                    i.putExtra("mModus", mModus);
                    i.putExtra("token", mToken);
                    i.putExtra("text", text);
                    i.putExtra("timestamp", mTimestamp);
                    i.putExtra("mPictureUriList", mPictureUriList);
                    // i.putExtra("myOcrResultsList", (Serializable) myOcrResultsList);
                    startActivity(i);
                    finish();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mUploadImagesTask = null;
            showProgress(false);
            mRetake.setEnabled(true);
            mProcessOcr.setEnabled(true);
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
        mRetake.setEnabled(true);
        mProcessOcr.setEnabled(true);
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

            mMultipleImagesView.setVisibility(View.GONE);
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

    public int getFileSizeFromUri(String sUri){
        Uri uri = Uri.parse(sUri);

        try {
            ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r");
            long size = fd.getStatSize();
            if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE) {
                throw new IllegalArgumentException
                        (size + " cannot be cast to int without changing its value.");
            }
            return (int) size;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        return 0;
    }
}
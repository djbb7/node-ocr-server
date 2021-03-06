package fi.aalto.openoranges.project2.openocranges;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
/*import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;*/
import android.support.v7.widget.PopupMenu;
import android.system.ErrnoException;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;



public class TakePictureActivity extends AppCompatActivity {

    private SurfaceHolder mHolder;
    private Bundle tempBundle;
    private Camera mCamera;
    private ImageButton mBack;
    private ImageButton mOptions;
    private FloatingActionButton mGallery;
    private TextView mModus;
    private int MY_PERMISSIONS_REQUEST_CAMERA;
    private int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;
    private Uri mPictureUri;
    private String mOcrOption;
    private static final String TAG = "TakePictureActivity";
    public static final int MEDIA_TYPE_IMAGE = 1;

    String[] mImageUriList;

    private String mToken;
    private String mMode;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        loadActivity();

    }

    protected void startCamera(){
        try {
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e) {
            Log.d("ERROR", "Failed to post camera: " + e.getMessage());
        }

        if (mCamera != null) {
            CameraView mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
            camera_view.addView(mCameraView);//add the SurfaceView to the layout
        }
    }

    protected void loadActivity(){
        setContentView(R.layout.activity_takepicture);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Make to run your application only in portrait mode

        startCamera();

        //post Token from previous activity
        mToken = getIntent().getStringExtra("token");
        mMode = getIntent().getStringExtra("mode");
        if (mMode == null){
            mMode = "Remote";
        }
        mOcrOption = mMode;


        //Button to take picture
        FloatingActionButton mTakePicture = (FloatingActionButton) findViewById(R.id.takePicture);
        mTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null, null, mPicture);
            }
        });
        mTakePicture.bringToFront();

        //Button to set options
        mOptions = (ImageButton) findViewById(R.id.ocrOptions);
        mOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showOptions(view);
            }
        });
        mOptions.bringToFront();

        //Button to choose picture out of gallery
        mGallery = (FloatingActionButton) findViewById(R.id.importGallery);
        mGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent;

                /*intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, 200);*/

                if (Build.VERSION.SDK_INT < 19) {
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setType("image/*");
                    startActivityForResult(intent, 200);
                } else {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setType("image/*");
                    startActivityForResult(intent, 200);
                }
            }
        });
        mGallery.bringToFront();


        //Button to close the TakePictureActivity and go back to MainActivity
        mBack = (ImageButton) findViewById(R.id.mBack);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(TakePictureActivity.this, MainActivity.class);
                i.putExtra("token", mToken);
                startActivity(i);
                finish();
            }
        });


        //Default modus is remote
        mModus = (TextView) findViewById(R.id.modus);
        mModus.setText("Modus: " + mMode);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
     * Get the URI of the selected image(s)
     * Will return a list of strings with the URIs for gallery image(s).
     *
     * @param data the returned data of the activity result
     */
    public String[] getPickImageResultUri(Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (data.getClipData() == null) {
                mImageUriList = new String[1];
                boolean isCamera = true;
                if (data != null && data.getData() != null) {
                    String action = data.getAction();
                    isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                }
                mImageUriList[0] = String.valueOf(isCamera ? getCaptureImageOutputUri() : data.getData());
            } else {
                mImageUriList = new String[data.getClipData().getItemCount()];
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    mImageUriList[i] = data.getClipData().getItemAt(i).getUri().toString();
                }
            }
        }
        return mImageUriList;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            getPickImageResultUri(data);

            Intent i = new Intent(TakePictureActivity.this, ProcessOcrActivity.class);
            i.putExtra("mModus", mOcrOption);
            i.putExtra("mOrientation", "0");
            i.putExtra("token", mToken);
            i.putExtra("mPictureUriList", mImageUriList);

            startActivity(i);
            finish();
        }
        else
            loadActivity();
    }

   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == 200 && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                getPickImageResultUri(data);

                Intent i = new Intent(TakePictureActivity.this, ProcessOcrActivity.class);
                i.putExtra("mModus", mOcrOption);
                i.putExtra("mOrientation", "0");
                i.putExtra("token", mToken);
                i.putExtra("mPictureUriList", mImageUriList);

                startActivity(i);
                finish();

            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

    }*/



    @Override
    protected void onStart() {
        super.onStart();
        loadActivity();

    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            mPictureUri = Uri.fromFile(pictureFile);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions!");
                return;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = true;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            Matrix matrix = new Matrix();

            matrix.postRotate(90);



            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap .getWidth(), bitmap .getHeight(), matrix, true);
            try {
                FileOutputStream out = new FileOutputStream(pictureFile);
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            if (mOcrOption.equals("Benchmark")){
                Intent i = new Intent(TakePictureActivity.this, ProcessOcrActivity.class);
                i.putExtra("mPictureUri", mPictureUri.toString());
                i.putExtra("mModus", mOcrOption);
                i.putExtra("token", mToken);
                i.putExtra("mOrientation", "" + getResources().getConfiguration().orientation);

                startActivity(i);
            }
            else{
                Intent i = new Intent(TakePictureActivity.this, ProcessOcrActivity.class);
                i.putExtra("mPictureUri", mPictureUri.toString());
                i.putExtra("mModus", mOcrOption);
                i.putExtra("token", mToken);
                i.putExtra("mOrientation", "" + getResources().getConfiguration().orientation);

                startActivity(i);
            }
        }
    };

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "OpenOCRanges");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("OpenOCRanges", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void showOptions(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_ocr, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.local:
                        mModus.setText("Modus: Local");
                        mOcrOption = "Local";
                        ;
                        return true;
                    case R.id.remote:
                        mModus.setText("Modus: Remote");
                        mOcrOption = "Remote";
                        ;
                        return true;
                    case R.id.benchmark:
                        mModus.setText("Modus: Benchmark");
                        mOcrOption = "Benchmark";
                        return true;
                    default:
                        return false;
                }
            }
        });

        popupMenu.show();
    /*    MenuPopupHelper menuHelper = new MenuPopupHelper(this, (MenuBuilder) popupMenu.getMenu(), v);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();*/
    }


    @Override
    public void onStop() {
        super.onStop();

        client.disconnect();
    }

    class CameraView extends SurfaceView implements SurfaceHolder.Callback {

        public CameraView(Context context, Camera camera) {
            super(context);

            mCamera = camera;
            mCamera.setDisplayOrientation(90);
            //set camera to continually auto-focus
            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);
            //post the holder and set this class as the callback, so we can post camera data here
            mHolder = getHolder();
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mHolder.addCallback(this);

        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            try {
                //when the surface is created, we can set the camera to draw images in this surfaceholder
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d("ERROR", "Camera error on surfaceCreated " + e.getMessage());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            //before changing the application orientation, you need to stop the preview, rotate and then start it again
            if (mHolder.getSurface() == null)//check if the surface is ready to receive camera data
                return;

            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                //this will happen when you are trying the camera if it's not running
            }

            //now, recreate the camera preview
            try {
                mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                mCamera.setPreviewDisplay(mHolder);
                mCamera.setDisplayOrientation(90);

                mCamera.startPreview();
            } catch (IOException e) {
                Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            //our app has only one screen, so we'll destroy the camera in the surface
            //if you are unsing with more screens, please move this code your activity
            mCamera.stopPreview();
            mCamera.release();
        }
    }


}
package fi.aalto.openoranges.project2.openocranges;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class TakePictureActivity extends AppCompatActivity{

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private ImageButton mBack;
    private ImageButton mOptions;
    private TextView mModus;
    private int MY_PERMISSIONS_REQUEST_CAMERA;
    private int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_takepicture);
        //setHasOptionsMenu(true);

        try {
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e) {
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        if (mCamera != null) {
            TakePictureActivity.CameraView mCameraView = new TakePictureActivity.CameraView(this, mCamera);//create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
            camera_view.addView(mCameraView);//add the SurfaceView to the layout
        }

        //Button to take picture
        FloatingActionButton mTakePicture = (FloatingActionButton) findViewById(R.id.takePicture);
        mTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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


        //Button to close the TakePictureActivity and go back to MainActivity
        mBack = (ImageButton) findViewById(R.id.mBack);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(TakePictureActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        });



        //Default modus is remote
        mModus = (TextView) findViewById(R.id.modus);
        mModus.setText("Modus: Remote");
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Camera permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            return;
        }

        //Write on storage permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
        }
    }

    private void showOptions(View v){
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_ocr, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.local:
                        mModus.setText("Modus: Local");
                        Toast.makeText(TakePictureActivity.this, "Local", Toast.LENGTH_SHORT).show();;
                        return true;
                    case R.id.remote:
                        mModus.setText("Modus: Remote");
                        Toast.makeText(TakePictureActivity.this, "Remote", Toast.LENGTH_SHORT).show();;
                        return true;
                    case R.id.benchmark:
                        mModus.setText("Modus: Benchmark");
                        Toast.makeText(TakePictureActivity.this, "Benchmark", Toast.LENGTH_SHORT).show();
                        return true;
                    default:
                        return false;
                }
            }
        });

        MenuPopupHelper menuHelper = new MenuPopupHelper(this, (MenuBuilder) popupMenu.getMenu(), v);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    class CameraView extends SurfaceView implements SurfaceHolder.Callback {

        public CameraView(Context context, Camera camera) {
            super(context);

            mCamera = camera;
            mCamera.setDisplayOrientation(90);
            //get the holder and set this class as the callback, so we can get camera data here
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
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
                mCamera.setPreviewDisplay(mHolder);
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

    //Permission handling
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.CAMERA)){
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        //Action for permission granted
                        try {
                            mCamera = Camera.open();//you can use open(int) to use different cameras
                        } catch (Exception e) {
                            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
                        }

                        if (mCamera != null) {
                            TakePictureActivity.CameraView mCameraView = new TakePictureActivity.CameraView(this, mCamera);//create a SurfaceView to show camera data
                            FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
                            camera_view.addView(mCameraView);//add the SurfaceView to the layout
                        }

                    } else {
                        //default action
                        Toast.makeText(TakePictureActivity.this, "Camera is necessary for OCR processing!", Toast.LENGTH_SHORT).show();
                        Intent j = new Intent(TakePictureActivity.this, MainActivity.class);
                        startActivity(j);
                        finish();
                    }
                }

                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        //Action for permission granted

                    } else {
                        //default action
                        Toast.makeText(TakePictureActivity.this, "Permission is necessary for OCR processing!", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        }
    }
}

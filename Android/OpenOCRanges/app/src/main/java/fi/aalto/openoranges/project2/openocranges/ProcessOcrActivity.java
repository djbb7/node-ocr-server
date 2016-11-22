package fi.aalto.openoranges.project2.openocranges;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ProcessOcrActivity extends AppCompatActivity {

    private Button mRetake;
    private Button mAddPicture;
    private Button mProcessOcr;
    private ImageView mPictureView;
    private Uri mPictureUri;
    private static final String TAG = "ProcessOcrActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processocr);

        mPictureUri = Uri.parse(getIntent().getStringExtra("mPictureUri"));
        //final SelectedPictures mSelectedPictures = ((SelectedPictures) getApplicationContext());

        //View for taken picture
        mPictureView = (ImageView) findViewById(R.id.picture_view);
        mPictureView.setImageURI(mPictureUri);

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

        //Button to adding a picture to OCR
        mAddPicture = (Button) findViewById(R.id.AddPicture);
        mAddPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Adding action
                //mSelectedPictures.setSelectedPictures(mPictureUri);
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
                //OCR processing
                Toast.makeText(ProcessOcrActivity.this, "OCR performed", Toast.LENGTH_SHORT).show();
                //mSelectedPictures = new SelectedPictures();
                Intent i = new Intent(ProcessOcrActivity.this, MainActivity.class);
                startActivity(i);
                finish();

            }
        });
    }
}

package fi.aalto.openoranges.project2.openocranges;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class ProcessOcrActivity extends AppCompatActivity {

    private Button mRetake;
    private Button mAddPicture;
    private Button mProcessOcr;
    private File mPictureFile;
    private static final String TAG = "ProcessOcrActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processocr);

//        mPictureFile = getIntent().getClass("mPictureFile");
//
//        try {
//            FileOutputStream fos = new FileOutputStream(mPictureFile);
//            fos.write(data);
//            fos.close();
//        } catch (FileNotFoundException e) {
//            Log.d(TAG, "File not found: " + e.getMessage());
//        } catch (IOException e) {
//            Log.d(TAG, "Error accessing file: " + e.getMessage());
//        }


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
            }
        });

        //Button to process OCR
        mProcessOcr = (Button) findViewById(R.id.ProcessOcr);
        mProcessOcr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //OCR processing
            }
        });
    }
}

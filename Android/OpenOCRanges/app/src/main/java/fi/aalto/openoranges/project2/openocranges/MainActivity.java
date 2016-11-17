package fi.aalto.openoranges.project2.openocranges;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import com.facebook.drawee.backends.pipeline.Fresco;

public class MainActivity extends AppCompatActivity {
    private ImageButton mReadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialize fresco for downloading icon images
        Fresco.initialize(this);

        setContentView(R.layout.activity_main);

        mReadButton = (ImageButton) findViewById(R.id.read);
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, TakePictureActivity.class);
                startActivity(i);
                finish();
            }
        });

    }


}


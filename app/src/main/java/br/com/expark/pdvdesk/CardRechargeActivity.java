package br.com.expark.pdvdesk;

import android.content.Intent;
import android.graphics.PointF;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import java.io.Serializable;
import java.util.HashMap;


public class CardRechargeActivity extends ActionBarActivity implements QRCodeReaderView.OnQRCodeReadListener {
    private QRCodeReaderView mydecoderview;
    String mQrcode = null;
    Boolean mReading = false;
    HashMap<String, String> params;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_recharge);

        mydecoderview = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        mydecoderview.setOnQRCodeReadListener(this);

        android.support.v7.app.ActionBar action=getSupportActionBar();
        action.setDisplayHomeAsUpEnabled(true);
        action.setTitle("Recarga de Cartão");

        params = new HashMap<>();
    }

    @Override
    public void onResume(){
        super.onResume();
        mReading = false;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_card_recharge, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed in the action bar.
                // Create a simple intent that starts the hierarchical parent activity and
                // use NavUtils in the Support Package to ensure proper handling of Up.
                Intent upIntent = new Intent(this, MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is not part of the application's task, so create a new task
                    // with a synthesized back stack.
                    TaskStackBuilder.from(this)
                            // If there are ancestor activities, they should be added here.
                            .addNextIntent(upIntent)
                            .startActivities();
                    finish();
                } else {
                    // This activity is part of the application's task, so simply
                    // navigate up to the hierarchical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);


    }

    @Override
    public void onQRCodeRead(String qrCode, PointF[] pointFs) {
        if(!mReading){
            mQrcode = qrCode;
            Intent intent = new Intent(this, SalesActivity.class);
            params.put("card_pin", mQrcode);
            intent.putExtra("params", params);
            startActivity(intent);
            mReading = true;
        }
    }

    @Override
    public void cameraNotFound() {

    }

    @Override
    public void QRCodeNotFoundOnCamImage() {

    }


}

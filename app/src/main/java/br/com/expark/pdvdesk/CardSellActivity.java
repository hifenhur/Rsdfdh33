package br.com.expark.pdvdesk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;

import models.GlobalParameters;
import models.Sell;
import printer.PrinterActivity;


public class CardSellActivity extends ActionBarActivity implements QRCodeReaderView.OnQRCodeReadListener {
    private QRCodeReaderView mydecoderview;
    String mQrcode = null;
    Boolean mReading = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_sell);

        mydecoderview = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        mydecoderview.setOnQRCodeReadListener(this);

        android.support.v7.app.ActionBar action=getSupportActionBar();
        action.setDisplayHomeAsUpEnabled(true);
        action.setTitle("Venda de Cartão");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_card_sell, menu);
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
            CardSellRequest();
        }
    }

    @Override
    public void cameraNotFound() {

    }

    @Override
    public void QRCodeNotFoundOnCamImage() {

    }

    public void CardSellRequest(){

        String sellerUUID = GlobalParameters.getInstance().sellerUUID;
        final String url = GlobalParameters.getInstance().defaultUrl + "/pdv_sales/"+sellerUUID+"/card_sell/"+mQrcode+".json";
        final int method = Request.Method.POST;
        final Context ctx = this.getApplicationContext();
        mReading = true;

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        String title = getString(R.string.title_please_wait);
        // Progress dialog message
        String text = getString(R.string.loading);
        // Progress dialog available due job execution
        final ProgressDialog dialog = ProgressDialog.show(CardSellActivity.this, title, text);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        JsonObjectRequest loginRequest = new JsonObjectRequest(method, url, new JSONObject(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (Boolean.valueOf(response.getString("success")) == false){

                                Toast.makeText(ctx, response.getString("error").toString(), Toast.LENGTH_LONG).show();

                            }else{
//                                {
//                                        "nsu": null,
//                                        "plate": null,
//                                        "processed_at": null,
//                                        "space_number": null,
//                                        "vehicle_type": null
//                                }
                                JSONObject jsonSell = new JSONObject(response.getString("card_sell"));
                                Log.d("JSON", response.toString());
                                Sell sell = new Sell(
                                        jsonSell.getInt("sale_type"),
                                        jsonSell.getString("uuid"),
                                        Timestamp.valueOf(jsonSell.getString("created_at")),
                                        jsonSell.getString("amount"),
                                        jsonSell.getString("content"),
                                        null,
                                        jsonSell.getString("pdv"),
                                        jsonSell.getString("lote"),
                                        jsonSell.getString("nsu"),
                                        jsonSell.getString("vehicle_type"));

                                Intent intent = new Intent(ctx, SaleActivity.class);
                                intent.putExtra("sell", sell);
                                intent.putExtra("card_sell", true);
                                startActivity(intent);


                            }
                            VolleyLog.v("Response:%n %s", response.toString(4));
                        } catch (JSONException e) {
                            dialog.dismiss();
                            Toast.makeText(ctx, R.string.server_error, Toast.LENGTH_LONG).show();

                            //volta pra tela inicial
                            Intent intent = new Intent(ctx, MainActivity.class);
                            startActivity(intent);

                            e.printStackTrace();
                        }
                        dialog.dismiss();
                        mReading = false;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(ctx, R.string.server_error, Toast.LENGTH_LONG).show();
                error.printStackTrace();
                dialog.dismiss();
                mReading = false;
            }
        });
        loginRequest.setRetryPolicy(new DefaultRetryPolicy(7000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(loginRequest);


    }
}

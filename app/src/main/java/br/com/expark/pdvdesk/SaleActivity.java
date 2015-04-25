package br.com.expark.pdvdesk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import models.GlobalParameters;
import models.Sell;
import printer.PrinterActivity;


public class SaleActivity extends ActionBarActivity {
    TextView saleTypeView;
    TextView saleView;
    TextView contentTypeView;
    TextView saleAmountView;
    TextView createdAtView;
    Sell mSell;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale);

        setViews();
        setViewsVariables();

        android.support.v7.app.ActionBar action=getSupportActionBar();
        action.setDisplayHomeAsUpEnabled(true);
        action.setTitle(mSell.getContent());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sell, menu);
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

    private void setViews(){
        saleTypeView = (TextView)findViewById(R.id.sale_type_receiver);
        saleView = (TextView)findViewById(R.id.sale_content_receptor);
        contentTypeView = (TextView)findViewById(R.id.content);
        saleAmountView = (TextView)findViewById(R.id.sale_amount_receptor);
        createdAtView = (TextView)findViewById(R.id.created_at);
    }

    private void setViewsVariables(){
        mSell = (Sell)getIntent().getExtras().get("sell");
        try {
            if((Boolean)getIntent().getExtras().get("card_sell")){
                TextView title = (TextView)findViewById(R.id.sale_title);
                title.setText(R.string.card_sell);
            }
        }catch (Exception e){

        }

        try {
            if(mSell.getContentType().equals("Recarga de Cartão")){
                TextView title = (TextView)findViewById(R.id.sale_title);
                title.setText(R.string.recharge_sell);
            }
        }catch (Exception e){

        }

        saleTypeView.setText(mSell.getType());
        saleView.setText(mSell.getContent());
        contentTypeView.setText(mSell.getContentType());
        saleAmountView.setText(mSell.getValue());
        createdAtView.setText(mSell.getCreatedAt().toString());
    }

    public void confirmSell(View view){

        String sellerUUID = GlobalParameters.getInstance().sellerUUID;
        final String url = GlobalParameters.getInstance().defaultUrl + "/pdv_sales/"+sellerUUID+"/process/"+mSell.getUuid()+".json";
        final int method = Request.Method.POST;
        final Context ctx = this.getApplicationContext();

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        String title = getString(R.string.title_please_wait);
        // Progress dialog message
        String text = getString(R.string.loading);
        // Progress dialog available due job execution
        final ProgressDialog dialog = ProgressDialog.show(SaleActivity.this, title, text);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        JsonObjectRequest loginRequest = new JsonObjectRequest(method, url, new JSONObject(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (Boolean.valueOf(response.getString("success")) == false){

                                Toast.makeText(ctx, R.string.login_error, Toast.LENGTH_LONG).show();

                            }else{

                                Intent intent = new Intent(ctx, PrinterActivity.class);
                                intent.putExtra("sell", mSell);
                                startActivity(intent);


                            }
                            VolleyLog.v("Response:%n %s", response.toString(4));
                        } catch (JSONException e) {
                            dialog.dismiss();
                            Toast.makeText(ctx, R.string.server_error, Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(ctx, R.string.server_error, Toast.LENGTH_LONG).show();
                error.printStackTrace();
            }
        });
        loginRequest.setRetryPolicy(new DefaultRetryPolicy(7000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(loginRequest);


    }

    public void cancelSell(View view){

        String sellerUUID = GlobalParameters.getInstance().sellerUUID;
        final String url = GlobalParameters.getInstance().defaultUrl + "/pdv_sales/"+sellerUUID+"/cancel/"+mSell.getUuid()+".json";
        final int method = Request.Method.POST;
        final Context ctx = this.getApplicationContext();

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        String title = getString(R.string.title_please_wait);
        // Progress dialog message
        String text = getString(R.string.loading);
        // Progress dialog available due job execution
        final ProgressDialog dialog = ProgressDialog.show(SaleActivity.this, title, text);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        JsonObjectRequest loginRequest = new JsonObjectRequest(method, url, new JSONObject(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (Boolean.valueOf(response.getString("success")) == false){

                                Toast.makeText(ctx, R.string.login_error, Toast.LENGTH_LONG).show();

                            }else{
                                Toast.makeText(ctx, R.string.cancel_success, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ctx, MainActivity.class);
                                startActivity(intent);


                            }
                            VolleyLog.v("Response:%n %s", response.toString(4));
                        } catch (JSONException e) {
                            Toast.makeText(ctx, R.string.server_error, Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(ctx, R.string.server_error, Toast.LENGTH_LONG).show();
                error.printStackTrace();
            }
        });
        loginRequest.setRetryPolicy(new DefaultRetryPolicy(7000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(loginRequest);


    }


}

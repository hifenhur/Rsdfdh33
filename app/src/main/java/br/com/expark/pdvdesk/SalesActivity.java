package br.com.expark.pdvdesk;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import models.GlobalParameters;
import models.Sell;


public class SalesActivity extends ActionBarActivity {
    ArrayList<Sell> mSalesList = null;
    JSONArray mSalesListJSON = null;
    String mSellerUid = GlobalParameters.getInstance().sellerUUID;
    private SwipeRefreshLayout swipeContainer;
    private String mCardPin;
    private HashMap<String, String> params;
    private ListView mListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);
        final Context ctx = this;
        try {
            params = (HashMap<String, String>) getIntent().getSerializableExtra("params");
        }catch (Exception e){
            e.printStackTrace();
        }

        mListView = (ListView)findViewById(R.id.main_list);


        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (String.valueOf(mSellerUid).isEmpty()) {
                    Intent intent = new Intent(ctx, LoginActivity.class);
                    startActivity(intent);
                } else {
                    getSellsFromServer();
                }
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        swipeContainer.setRefreshing(true);

        getSellsFromServer();

        android.support.v7.app.ActionBar action=getSupportActionBar();
        action.setDisplayHomeAsUpEnabled(true);
        action.setTitle("Vendas no Terminal");


    }

    @Override
    protected void onResume() {
        super.onResume();
        getSellsFromServer();
        mSellerUid = GlobalParameters.getInstance().sellerUUID;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sells, menu);

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



    public void loadList() {
        final Context ctx = this.getApplicationContext();

        ArrayAdapter adapter = (new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, mSalesList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setText(mSalesList.get(position).getContent() + " - " + mSalesList.get(position).getValue());
                text2.setText(mSalesList.get(position).toString());
                return view;
            }
        });

        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ctx, SaleActivity.class);
                intent.putExtra("sell", mSalesList.get(position));
                startActivity(intent);
            }
        });

    }


    public void getSellsFromServer() {

        final Context ctx = this;
        String default_url = GlobalParameters.getInstance().defaultUrl;
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String url;
        try {
            mCardPin = params.get("card_pin").toString();
            if (!mCardPin.isEmpty()){
                getSupportActionBar().setTitle("Recarga");
                url = default_url + "/pdv_sales/" + mSellerUid + "/"+mCardPin+".json";
            }else{
                url = default_url + "/pdv_sales/" + mSellerUid + ".json";
            }

        }catch (Exception e){
            e.printStackTrace();
            url = default_url + "/pdv_sales/" + mSellerUid + ".json";
        }


        JsonArrayRequest getRequest = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // display response
                        Log.d("Response", response.toString());
                        if(response.length() < 1){
                            Toast.makeText(ctx, "Não existe nenhuma venda para ser processada", Toast.LENGTH_SHORT).show();
                        }
                        mSalesList = new ArrayList<Sell>();

                        for (int i = 0; i < response.length(); i++) {
                            try {


                                JSONObject jsonSell = response.getJSONObject(i);
                                Sell sell = new Sell(
                                        jsonSell.getInt("sale_type"),
                                        jsonSell.getString("uuid"),
                                        Timestamp.valueOf(jsonSell.getString("created_at")),
                                        jsonSell.getString("amount"),
                                        jsonSell.getString("content"),
                                        Timestamp.valueOf(jsonSell.getString("limit_at")),
                                        jsonSell.getString("pdv"),
                                        jsonSell.getString("lote"),
                                        jsonSell.getString("nsu"),
                                        jsonSell.getString("vehicle_type"));


                                mSalesList.add(sell);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                        loadList();
                        swipeContainer.setRefreshing(false);


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();

                        Toast.makeText(ctx,
                                "Erro ao receber lista de vendas, verifique sua conexao com a internet",
                                Toast.LENGTH_SHORT).show();

                        swipeContainer.setRefreshing(false);
                    }

                }
        );
        queue.add(getRequest);


    }




}

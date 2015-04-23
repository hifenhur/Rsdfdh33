package br.com.expark.pdvdesk;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
    private int backPressedCount = 0;
    private int PRESS_TIMES_TO_EXIT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void toSalesActivity(View view){
        Intent intent = new Intent(this, SalesActivity.class);
        startActivity(intent);
    }

    public void toCardSellActivity(View view){
        Intent intent = new Intent(this, CardSellActivity.class);
        startActivity(intent);
    }

    public void toCardRechargeActivity(View view){
        Intent intent = new Intent(this, CardRechargeActivity.class);
        startActivity(intent);
    }
    @Override

    public void onBackPressed() {
        // Otherwise defer to system default behavior.
        backPressedCount += 1;

        if (backPressedCount >= PRESS_TIMES_TO_EXIT) {
            super.onBackPressed();
        } else {
            Toast.makeText(this,
                    "Para sair pressione o botão mais " + (backPressedCount - PRESS_TIMES_TO_EXIT) + " vez",
                    Toast.LENGTH_SHORT).show();
        }

    }
}

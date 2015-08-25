package me.zhangyuanwei.csslayout;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewGroup layout = (ViewGroup) findViewById(R.id.layout);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (v instanceof TextView) {
                    TextView textView = (TextView) v;
                    String text = (String) textView.getText();
                    textView.setText(text + text);
                } else {
                    ((ViewGroup) v.getParent()).removeView(v);
                }
                //layout.removeView(v);
            }
        };

        walkViewTree(layout, listener);

        //findViewById(R.id.text1).setOnClickListener(listener);
        //findViewById(R.id.text2).setOnClickListener(listener);
        //findViewById(R.id.text3).setOnClickListener(listener);
    }

    private void walkViewTree(ViewGroup group, View.OnClickListener listener) {
        int count = group.getChildCount();
        int index;
        View child;
        for (index = 0; index < count; index++) {
            child = group.getChildAt(index);
            child.setOnClickListener(listener);
            if (child instanceof ViewGroup) {
                walkViewTree((ViewGroup) child, listener);
            }
        }
    }
    /*

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
    */
}

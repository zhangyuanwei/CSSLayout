package me.zhangyuanwei.csslayout;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class MainActivity extends Activity {

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
}

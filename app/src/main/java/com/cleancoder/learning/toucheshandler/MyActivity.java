package com.cleancoder.learning.toucheshandler;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.cleancoder.learning.toucheshandler.griddynamicview.GridDynamicAbsListView;
import com.cleancoder.learning.toucheshandler.griddynamicview.GridDynamicView;
import com.cleancoder.learning.toucheshandler.griddynamicview.ParallaxGridDynamicView;


public class MyActivity extends ActionBarActivity {

    private GridDynamicView gridDynamicView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        initView();
    }

    private void initView() {
        gridDynamicView = ((ParallaxGridDynamicView) findViewById(R.id.grid_dynamic_view)).getGridDynamicView();
        gridDynamicView.setAdapter(new SimpleAdapter(this));
    }

    public void onScrollToStart(View unused) {
        gridDynamicView.scrollToStart();
    }

    public void onScrollToEnd(View unused) {
        gridDynamicView.scrollToEnd();
    }

    public void checkScrolledToEdge(View unused) {
        String message = "Start: " + gridDynamicView.isScrolledToStart() + "\n" +
                         "End: " + gridDynamicView.isScrolledToEnd();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }



    private static class SimpleAdapter extends ArrayAdapter<String> {
        private static final int NUMBER_OF_ITEMS = 12;

        private static final int LAYOUT = R.layout.image_view;

        private final LayoutInflater layoutInflater;

        public SimpleAdapter(Context context) {
            super(context, LAYOUT);
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = (convertView != null) ? convertView : layoutInflater.inflate(LAYOUT, null);
            ImageView imageView = (ImageView) itemView.findViewById(R.id.image_view);
            imageView.setImageResource(R.drawable.ic_launcher);
            imageView.setLayoutParams(new GridDynamicAbsListView.LayoutParams(300, 300));
            itemView.setPadding(4, 2, 4, 2);
            return itemView;
        }

        @Override
        public int getCount() {
            return NUMBER_OF_ITEMS;
        }
    }


}

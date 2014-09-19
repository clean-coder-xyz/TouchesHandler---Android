package com.cleancoder.learning.toucheshandler;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;


public class MyActivity extends ActionBarActivity {
    private static final int NUMBER_OF_ITEMS = 15;

    private HorizontalParallaxScrollView parallaxScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        initView();
    }

    private void initView() {
        parallaxScrollView = (HorizontalParallaxScrollView) findViewById(R.id.parallaxScrollView);
        for (int i = 0; i < NUMBER_OF_ITEMS; ++i) {
            ImageView imageView = new ImageView(this);
            ViewUtils.setSize(imageView, 250, 250);
            imageView.setImageResource(R.drawable.ic_launcher);
            imageView.setPadding(6, 6, 4, 4);
            parallaxScrollView.addItemView(imageView);
        }
    }

}

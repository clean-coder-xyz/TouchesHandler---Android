package com.cleancoder.learning.toucheshandler.scrolling;

import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;

/**
 * Created by lsemenov on 11.09.2014.
 */
public class SimpleMotionEvent {

    private int action;
    private int delta;
    private int id;
    private float x;
    private float y;

    public static SimpleMotionEvent from(MotionEvent event) {
        int index = MotionEventCompat.getActionIndex(event);
        SimpleMotionEvent simpleMotionEvent = new SimpleMotionEvent();
        simpleMotionEvent.setAction(MotionEventCompat.getActionMasked(event));
        simpleMotionEvent.setX(MotionEventCompat.getX(event, index));
        simpleMotionEvent.setY(MotionEventCompat.getY(event, index));
        simpleMotionEvent.setId(MotionEventCompat.getPointerId(event, index));
        return simpleMotionEvent;
    }

    public SimpleMotionEvent() {
        // do nothing
    }

    public SimpleMotionEvent setX(float x) {
        this.x = x;
        return this;
    }

    public SimpleMotionEvent setY(float y) {
        this.y = y;
        return this;
    }

    public SimpleMotionEvent setAction(int action) {
        this.action = action;
        return this;
    }

    public SimpleMotionEvent setId(int id) {
        this.id = id;
        return this;
    }

    public SimpleMotionEvent setDelta(int delta) {
        this.delta = delta;
        return this;
    }

    public int getAction() {
        return action;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getId() {
        return id;
    }

    public int getDelta() {
        return delta;
    }

}

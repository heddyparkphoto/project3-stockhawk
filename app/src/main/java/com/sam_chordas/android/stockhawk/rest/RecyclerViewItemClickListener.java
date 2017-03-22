package com.sam_chordas.android.stockhawk.rest;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by sam_chordas on 11/9/15.
 */
/*
    updated by: hyeryungpark for Udacity project: around 11/7/16
    * Modify onInterceptTouchEvent to return 'false' to detect and handle swipe-delete event

 */
public class RecyclerViewItemClickListener implements RecyclerView.OnItemTouchListener {
    private static final String LOG_TAG = RecyclerViewItemClickListener.class.getSimpleName();

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    private GestureDetector gestureDetector;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        public void onItemClick(View v, int position);
    }

    public RecyclerViewItemClickListener(Context context, OnItemClickListener listener) {
        this.listener = listener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        View childView = view.findChildViewUnder(e.getX(), e.getY());
        if (childView != null && listener != null && gestureDetector.onTouchEvent(e)) {
            listener.onItemClick(childView, view.getChildPosition(childView));
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
    }
}

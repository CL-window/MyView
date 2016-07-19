package com.example.bigview;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v7.widget.ActionBarOverlayLayout;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * <p>Description:  </p>
 * Created by slack on 2016/7/18 13:14 .
 */
public class MySeekBar extends LinearLayout {

    private View view;
    private SeekBar mSeekBar;
    private SeekBarProgress mSeekBarProgress;
    private TextView proressInfo;
    private float progres = 0;

    public MySeekBar(Context context) {
        super(context);
    }

    public MySeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        view = LayoutInflater.from(context).inflate(R.layout.seekbar, this);

        init();
    }

    public MySeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        proressInfo = (TextView)view.findViewById(R.id.progress_info);
        mSeekBar = (SeekBar) view.findViewById(R.id.my_seekbar);
        changeInfoX();
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
//                Log.i("slack","onProgressChanged:" + progress);
                progres = progress;
                proressInfo.setText( progress + "%");

                animBig(proressInfo);
                if(mSeekBarProgress != null){
                    mSeekBarProgress.onSeekProgress(progress);
                }
                changeInfoX();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                Log.i("slack","onStartTrackingTouch...");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Log.i("slack","onStopTrackingTouch..." +mSeekBar.getX() +","+
//                        (progres / 100) + "," +
//                        mSeekBar.getWidth() + "," +mSeekBar.getMeasuredWidth() + ","+
//                        (proressInfo.getMeasuredWidth() / 2) + "," +
//                        (mSeekBar.getMeasuredWidth() * (progres / 100) + mSeekBar.getX() ));
                // proressInfo 位置：progress点的位置上
                changeInfoX();
                animCreate(proressInfo);
            }
        });
    }

    private void changeInfoX() {
        proressInfo.setX( mSeekBar.getMeasuredWidth() * (progres / 100) + mSeekBar.getX() - (proressInfo.getMeasuredWidth() / 2) );
    }

    public AnimatorSet animCreate(View view){

        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator oaX = ObjectAnimator.ofFloat(view, "scaleX",0.5f, 1.0f, 0.5f);
        ObjectAnimator oaY = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1.0f, 0.5f);
        ObjectAnimator oaA = ObjectAnimator.ofFloat(view, "alpha", 0.5f , 1.0f, 0.5f);
        animatorSet.play(oaX).with(oaY).with(oaA);
        animatorSet.setDuration(800);
        animatorSet.setInterpolator(new OvershootInterpolator());
//        animatorSet.setStartDelay(100);
        animatorSet.start();
        return animatorSet;
    }

    public AnimatorSet animBig(View view){

        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator oaX = ObjectAnimator.ofFloat(view, "scaleX",0.5f, 2.0f);
        ObjectAnimator oaY = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 2.0f);
        ObjectAnimator oaA = ObjectAnimator.ofFloat(view, "alpha", 0.5f , 1f);
        animatorSet.play(oaX).with(oaY).with(oaA);
        animatorSet.setDuration(0);
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.start();
        return animatorSet;
    }


    public void setmSeekBarProgress(SeekBarProgress mSeekBarProgress) {
        this.mSeekBarProgress = mSeekBarProgress;
    }

    interface SeekBarProgress{
        void onSeekProgress(int progress);
    }

}

package com.example.bigview;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

/**
 * <p>Description: 自定义 ProgressBar ，倒计时效果，</p>
 * Created by slack on 2016/7/12 15:16 .
 */
public class MyProgressBar extends LinearLayout {

    private ProgressBar mProgressBar;
    private Handler mHandler;
    private int maxTime = 0;
    private ProgressListener mProgressListener;

    public MyProgressBar(Context context) {
        super(context);
    }

    public MyProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mProgressBar = (ProgressBar)LayoutInflater.from(context).inflate(R.layout.progressbar, this).findViewById(R.id.progressBar);
        mHandler = new Handler();
    }

    public MyProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     *
     * @param maxTime 时间（秒）
     */
    public void setMax(int maxTime) {
        this.maxTime = maxTime;
        mProgressBar.setProgress(1);
        mProgressBar.setMax(maxTime);
        mHandler.post(timeCount);
    }

    Runnable timeCount = new Runnable() {
        @Override
        public void run() {
//            Log.i("slack","timeCount " + mProgressBar.getProgress() + "," + maxTime);
            if(mProgressBar.getProgress() < maxTime) {
                mProgressBar.incrementProgressBy(1);
                if(mProgressListener != null){
                    mProgressListener.progress(mProgressBar.getProgress());
                }
                mHandler.postDelayed(timeCount, 1000);
            }else{
                if(mProgressListener != null){
                    mProgressListener.finish();
                }
                mHandler.removeCallbacks(timeCount);
//                Log.i("slack","timeCount finish...");
            }

        }
    };

    public interface ProgressListener{
        void progress(int progress);
        void finish();// 计时完成操作，这个控件需要dismiss
    }
}

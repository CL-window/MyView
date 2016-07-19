package com.example.bigview;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * <p>Description:  中间突出的滑动 button </p>
 * Created by slack on 2016/7/11 11:00 .
 * 暗坑 ： view.getX 相当于该view距离父容器左边缘的距离，不是相对整个屏幕
 * 动画添加失败，以后再处理
 */
public class showCenter extends LinearLayout implements View.OnClickListener {

    private float downX,downY, upX;
    private double threshold = 30;// 滑动阈值
    private int[] smallIco = {R.drawable.video, R.drawable.camera, R.drawable.gifp};// 小图 录像 拍照 gif
    private int[] bigIco = {R.drawable.video_large, R.drawable.camera_large, R.drawable.gifp_large};// 录像 拍照 gif
    private String[] bigIcoInfo = {"录像", "拍照", "GIF"};
    private int centerSelect = 1;
    private View view;
    private ImageView leftImage, centerImage, rightImage;
    private LinearLayout centerView;
    private TextView centerInfo;
    private ShowCenterListener mShowCenterListener;

    private Paint paint;
    private Path path;
    private float startX = 0; // 起点坐标(中心)
    private float startY = 0;
    private float anchorX = 0;// 锚点坐标
    private float anchorY = 0;
    private float endX = 0; // 终点坐标
    private float endY = 0;
    int[] location = new int[2];
    // 默认定点圆半径
    public static final float DEFAULT_RADIUS = 20;
    // 定点圆半径
    float radius = DEFAULT_RADIUS;

    public showCenter(Context context) {
        super(context);
    }

    public showCenter(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);// 关于onDraw()方法不被执行的解决方法
        view = LayoutInflater.from(context).inflate(R.layout.bottomview, this);
        initView();
        initListener();

    }

    private void initView() {
        leftImage = (ImageView) view.findViewById(R.id.bottom_view_left);
        centerImage = (ImageView) view.findViewById(R.id.bottom_view_center_big);
        rightImage = (ImageView) view.findViewById(R.id.bottom_view_right);
        centerInfo = (TextView) view.findViewById(R.id.bottom_view_center_info);
        centerView = (LinearLayout)view.findViewById(R.id.bottom_view_center);

        path = new Path();

        paint = new Paint();
        paint.setAntiAlias(true);
//        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStyle(Paint.Style.STROKE);// 画笔画线
        paint.setStrokeWidth(2);
        paint.setColor(Color.RED);

    }

    private void initListener() {
        view.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = motionEvent.getX();
                        downY = motionEvent.getY();
//                        Log.i("slack","ACTION_DOWN..."+downX);
                        break;
                    case MotionEvent.ACTION_MOVE:
//                        Log.i("slack","ACTION_MOVE...");
                        // 滑动中开始动画
                        float tempX = 0, tempY = 0;
                        if (moveLeft(downX, motionEvent.getX())) {
                            tempX = downX - motionEvent.getX();
                            tempY = downY - motionEvent.getY();
                            rightImage.getLocationOnScreen(location);
                            startX = location[0] + (rightImage.getWidth() / 2);
                            startY = location[1] + (rightImage.getHeight() / 2);
                            Log.i("slack", "start_left:" + startX + "," + startY);
                            if (startX + tempX < centerView.getX()) {
                                endX = startX + tempX;
                                endY = startY + tempY;
                            } else {
                                endX = centerView.getChildAt(0).getX()+ (centerView.getChildAt(0).getWidth() / 2);
                                endY = centerView.getChildAt(0).getY() + (centerView.getChildAt(0).getHeight() / 2);
                            }
                            if(startY + tempY < centerView.getY()){

                            }
                        } else {
                            startX = leftImage.getX() + (leftImage.getWidth() / 2);
                            startY = leftImage.getY() + (leftImage.getHeight() / 2);
                            Log.i("slack", "start_right:" + startX + "," + startY);
                            tempX = motionEvent.getX() - startX;
                            tempY = motionEvent.getY() - startY;
                            if (startX - tempX > centerImage.getX()) {
                                endX = startX - tempX;
                                endY = startY - tempY;
                            } else {
                                endX = centerView.getChildAt(0).getX() + (centerView.getChildAt(0).getWidth() / 2);
                                endY = centerView.getChildAt(0).getY() + (centerView.getChildAt(0).getHeight() / 2);
                            }
                        }
                        Log.i("slack", "end:" + endX + "," + endY);
                        anchorX = (endX + startX) / 2;
                        anchorY = (endY + startY) / 2;
                        Log.i("slack", "anchor:" + anchorX + "," + anchorY);
                        break;
                    case MotionEvent.ACTION_UP:
                        upX = motionEvent.getX();
//                        Log.i("slack","ACTION_UP..."+upX);
                        if (downX - upX > threshold) {
                            Log.i("slack", "left...1");
                            leftSelect();
                        } else if (upX - downX > threshold) {
                            Log.i("slack", "right...1");
                            rightSelect();
                        }
//                        invalidate();//
                        changeView();
                        break;
                    default:
                        break;
                }

//                invalidate();
                return true;
            }
        });
        leftImage.setOnClickListener(this);
        centerImage.setOnClickListener(this);
        rightImage.setOnClickListener(this);
        centerInfo.setOnClickListener(this);
    }

    // 判断左滑还是右滑
    private boolean moveLeft(double downX, float x) {
        if (downX - x > threshold) {
            return true;
        }
        return false;
    }

    private void leftSelect() {
        if (centerSelect > 2) {
            centerSelect = 0;
        }
        centerSelect = (centerSelect + 1) % 3;
//        rightImage.getLocationOnScreen(location);
//        startX = location[0] + ( rightImage.getWidth() / 2 );
//        startY = location[1] + ( rightImage.getHeight() / 2 );
//        Log.i("slack","start_left:"+startX+","+startY);
//
//        startAnim();

    }

    private void startAnim() {
        centerImage.getLocationOnScreen(location);
        endX = location[0] + (centerImage.getWidth() / 2);
        endY = location[1] + (centerImage.getHeight() / 2);
        Log.i("slack", "end:" + endX + "," + endY);
        anchorX = (startX + endX) / 2;
        anchorY = (startY + endY) / 2;


//        animCreate(leftImage);
//        animCreate(centerImage);
//        animCreate(rightImage);
    }

    private void rightSelect() {
        if (centerSelect < 0) {
            centerSelect = 2;
        }
        centerSelect = (centerSelect + 2) % 3;
//        startX = leftImage.getX() + ( leftImage.getWidth() / 2 );
//        startY = leftImage.getY() + ( leftImage.getHeight() / 2 );
//        Log.i("slack","start_left:"+startX+","+startY);
//        startAnim();
    }

    public AnimatorSet animCreate(ImageView imageView) {

        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator oaX = ObjectAnimator.ofFloat(imageView, "scaleX", 0.5f, 1f);
        ObjectAnimator oaY = ObjectAnimator.ofFloat(imageView, "scaleY", 0.5f, 1f);
        ObjectAnimator oaA = ObjectAnimator.ofFloat(imageView, "alpha", 0.0f, 1f);
        animatorSet.play(oaX).with(oaY).with(oaA);
        animatorSet.setDuration(800);
        animatorSet.setInterpolator(new OvershootInterpolator());
//        animatorSet.setStartDelay(300);
        animatorSet.start();
        return animatorSet;
    }

    private void changeView() {
        leftImage.setImageResource(smallIco[(centerSelect + 2) % 3]);
        centerImage.setImageResource(bigIco[centerSelect % 3]);
        centerInfo.setText(bigIcoInfo[centerSelect]);
        rightImage.setImageResource(smallIco[(centerSelect + 1) % 3]);

        if (mShowCenterListener != null) {
            mShowCenterListener.onSliding(centerSelect);
        }

    }


    public showCenter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Log.i("slack", "onLayout...3");

        super.onLayout(changed, left, top, right, bottom);
    }

    private void calculate(){
        // 勾股定理计算两个点之间的距离
        float distance = (float) Math.sqrt(Math.pow(endY-startY, 2) + Math.pow(endX-startX, 2));
        radius = -distance/15+DEFAULT_RADIUS;

        // 根据角度算出四边形的四个点
        float offsetX = (float) (radius*Math.sin(Math.atan((endY - startY) / (endX - startX))));
        float offsetY = (float) (radius*Math.cos(Math.atan((endY - startY) / (endX - startX))));

        float x1 = startX - offsetX;
        float y1 = startY + offsetY;

        float x2 = endX - offsetX;
        float y2 = endY + offsetY;

        float x3 = endX + offsetX;
        float y3 = endY - offsetY;

        float x4 = startX + offsetX;
        float y4 = startY - offsetY;

        path.reset();
        path.moveTo(x1, y1);
        // 赛贝尔二次曲线，需要操作点坐标和结束点坐标
        path.quadTo(anchorX, anchorY, x2, y2);
        path.lineTo(x3, y3);// 直线
        path.quadTo(anchorX, anchorY, x4, y4);
        path.lineTo(x1, y1);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.i("slack", "onDraw...4");
        calculate();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.OVERLAY);
        canvas.drawCircle(startX, startY, radius, paint);
        canvas.drawCircle(endX, endY, radius, paint);
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.i("slack", "onMeasure...2");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bottom_view_left:
                rightSelect();
                changeView();
                break;
            case R.id.bottom_view_center_big:
            case R.id.bottom_view_center_info:
                if (mShowCenterListener != null) {
                    mShowCenterListener.onCenterClick();
                }
                break;
            case R.id.bottom_view_right:
                leftSelect();
                changeView();
                break;
            default:
                break;
        }
    }

    // 设置监听
    public void setListener(ShowCenterListener showCenterListener) {
        mShowCenterListener = showCenterListener;
    }

    interface ShowCenterListener {
        void onSliding(int mode);// 滑动监听 提供当前中间的按钮模式 （ 0：录像  1：拍照  2：gif）

        void onCenterClick();// 点击中间的按钮（拍照或者gif或者录像） 监听
    }
}


// 可以使用，只是不是想要的动画
//public class showCenter extends LinearLayout implements View.OnClickListener {
//
//    private double downX,upX;
//    private double threshold = 30;// 滑动阈值
//    private int[] smallIco = {R.drawable.video,R.drawable.camera,R.drawable.gifp};// 小图 录像 拍照 gif
//    private int[] bigIco = {R.drawable.video_large,R.drawable.camera_large,R.drawable.gifp_large};// 录像 拍照 gif
//    private String[] bigIcoInfo = {"录像", "拍照", "GIF"};
//    private int centerSelect = 1;
//    private View view;
//    private ImageView leftImage,centerImage,rightImage;
//    private TextView centerInfo;
//    private ShowCenterListener mShowCenterListener;
//
//    private Paint paint;
//    private Path path;
//    private float startX = 0; // 起点坐标(中心)
//    private float startY = 0;
//    private float anchorX = 0;// 锚点坐标
//    private float anchorY = 0;
//    private float endX = 0; // 终点坐标
//    private float endY = 0;
//    int[] location = new int[2];
//    // 默认定点圆半径
//    public static final float DEFAULT_RADIUS = 20;
//    // 定点圆半径
//    float radius = DEFAULT_RADIUS;
//
//    public showCenter(Context context) {
//        super(context);
//    }
//    public showCenter(Context context, AttributeSet attrs) {
//        super(context, attrs);
////        setWillNotDraw(false);// 关于onDraw()方法不被执行的解决方法
//        view = LayoutInflater.from(context).inflate(R.layout.bottomview, this);
//        initView();
//        initListener();
//
//    }
//
//    private void initView() {
//        leftImage = (ImageView) view.findViewById(R.id.bottom_view_left);
//        centerImage =(ImageView)  view.findViewById(R.id.bottom_view_center_big);
//        rightImage = (ImageView)view.findViewById(R.id.bottom_view_right);
//        centerInfo = (TextView)view.findViewById(R.id.bottom_view_center_info);
//
//
//    }
//    private void initListener() {
//        view.setOnTouchListener(new OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                switch (motionEvent.getAction()){
//                    case MotionEvent.ACTION_DOWN:
//                        downX = motionEvent.getX();
////                        Log.i("slack","ACTION_DOWN..."+downX);
//                        break;
//                    case MotionEvent.ACTION_MOVE:
////                        Log.i("slack","ACTION_MOVE...");
//                       // 滑动中开始动画
//
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        upX = motionEvent.getX();
////                        Log.i("slack","ACTION_UP..."+upX);
//                        if(downX - upX > threshold){
//                            Log.i("slack","left...");
//                            leftSelect();
//                        }else if(upX - downX > threshold){
//                            Log.i("slack","right...");
//                            rightSelect();
//                        }
//
//
//
////                        invalidate();//
//                        changeView();
//                        break;
//                    default: break;
//                }
//                return true;
//            }
//        });
//        leftImage.setOnClickListener(this);
//        centerImage.setOnClickListener(this);
//        rightImage.setOnClickListener(this);
//        centerInfo.setOnClickListener(this);
//    }
//
//    private void leftSelect() {
//        if(centerSelect > 2 ){
//            centerSelect = 0;
//        }
//        centerSelect = (centerSelect + 1) % 3;
//        rightImage.getLocationOnScreen(location);
//        startX = location[0] + ( rightImage.getWidth() / 2 );
//        startY = location[1] + ( rightImage.getHeight() / 2 );
//        Log.i("slack","start_left:"+startX+","+startY);
//
//        startAnim();
//
//    }
//
//    private void startAnim() {
//        animCreate(leftImage);
//        animCreate(centerImage);
//        animCreate(rightImage);
//    }
//
//    private void rightSelect() {
//        if(centerSelect < 0){
//            centerSelect = 2;
//        }
//        centerSelect = (centerSelect + 2) % 3;
//        startX = leftImage.getX() + ( leftImage.getWidth() / 2 );
//        startY = leftImage.getY() + ( leftImage.getHeight() / 2 );
//        Log.i("slack","start_left:"+startX+","+startY);
//        startAnim();
//    }
//
//    public AnimatorSet animCreate(ImageView imageView){
//
//        centerImage.getLocationOnScreen(location);
//        endX =location[0] + ( centerImage.getWidth() / 2 );
//        endY = location[1] + (  centerImage.getHeight() / 2 );
//        Log.i("slack","end:"+endX+","+endY);
//        anchorX = (startX + endX) / 2;
//        anchorY = (startY + endY) / 2;
//
//        AnimatorSet animatorSet = new AnimatorSet();
//        ObjectAnimator oaX = ObjectAnimator.ofFloat(imageView, "scaleX", 0.5f, 1f);
//        ObjectAnimator oaY = ObjectAnimator.ofFloat(imageView, "scaleY", 0.5f, 1f);
//        ObjectAnimator oaA = ObjectAnimator.ofFloat(imageView, "alpha", 0.0f, 1f);
//        animatorSet.play(oaX).with(oaY).with(oaA);
//        animatorSet.setDuration(800);
//        animatorSet.setInterpolator(new OvershootInterpolator());
////        animatorSet.setStartDelay(300);
//        animatorSet.start();
//        return animatorSet;
//    }
//
//    private void changeView() {
//        leftImage.setImageResource(smallIco[(centerSelect + 2) % 3]);
//        centerImage.setImageResource(bigIco[centerSelect % 3]);
//        centerInfo.setText(bigIcoInfo[centerSelect]);
//        rightImage.setImageResource(smallIco[(centerSelect + 1) % 3]);
//
//        if(mShowCenterListener != null) {
//            mShowCenterListener.onSliding(centerSelect);
//        }
//
//    }
//
//
//    public showCenter(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        Log.i("slack","onDraw...");
//        super.onDraw(canvas);
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//    }
//
//    @Override
//    public void onClick(View view) {
//        switch (view.getId()){
//            case R.id.bottom_view_left:
//                rightSelect();
//                changeView();
//                break;
//            case R.id.bottom_view_center_big:
//            case R.id.bottom_view_center_info:
//                if(mShowCenterListener != null){
//                    mShowCenterListener.onCenterClick();
//                }
//                break;
//            case R.id.bottom_view_right:
//                leftSelect();
//                changeView();
//                break;
//            default: break;
//        }
//    }
//
//    // 设置监听
//    public void setListener(ShowCenterListener showCenterListener){
//        mShowCenterListener = showCenterListener;
//    }
//
//    interface ShowCenterListener{
//        void onSliding(int mode);// 滑动监听 提供当前中间的按钮模式 （ 0：录像  1：拍照  2：gif）
//        void onCenterClick();// 点击中间的按钮（拍照或者gif或者录像） 监听
//    }
//}

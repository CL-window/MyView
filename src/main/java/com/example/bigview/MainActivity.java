package com.example.bigview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.bigview.gifencoder.ClMakerGIF;
import com.example.bigview.gifencoder.GifEncoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView mLogTv;
    List<Bitmap> bitmapList = new ArrayList<>();
    int[] arr = {R.drawable.gif1, R.drawable.gif2, R.drawable.gif3, R.drawable.gif4};
    int[] arr_png = {R.drawable.gif01, R.drawable.gif02, R.drawable.gif03, R.drawable.gif04};
    String path  = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Movies" + File.separator + "aa.gif";
    File file = new File(path);
    long start, temp;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        setContentView(R.layout.progressbar);

//        ((MyProgressBar)findViewById(R.id.progress)).setMax(20);

//        ((GifView) findViewById(R.id.gif1)).setMovieResource(R.raw.a);
//        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Movies" + File.separator + "b.gif";
//        ((GifView) findViewById(R.id.gif2)).setMovieResource(path);
//        ((GifView) findViewById(R.id.gif3)).setMovieResource(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Movies" + File.separator + "aa.gif");
        mLogTv = (TextView) findViewById(R.id.main_log_tv);
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(final Message msg) {
                if(msg.what == 100){
                    Log.i("slack","handle...");
                    addToView(path);
                }
                if(msg.getData().get("thread") != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLogTv.append("Thread:" + msg.getData().get("thread") + "   cost time:" + msg.getData().get("cost_time") + "\n" );
                        }
                    });
                }

                super.handleMessage(msg);
            }
        };
        initBitmapList();
//        addToView(file);
    }



    private void initBitmapList() {

        Bitmap bitmap = null;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 10; j++) {
                bitmap = BitmapFactory.decodeResource(getResources(), arr[i]);
//                Log.i("slack", "bitmap_1:" + bitmap.getByteCount() + "," + bitmap.getHeight() + "," + bitmap.getWidth());
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, 200, 200);
//                bitmap = zoomImage(bitmap,200,200);
                Log.i("slack", "bitmap_2:" + bitmap.getByteCount() + "," + bitmap.getHeight() + "," + bitmap.getWidth());
//                                bitmap.compress(Bitmap.CompressFormat.JPEG, 50 ,new ByteArrayOutputStream());
                bitmapList.add(bitmap);

//                int[] pix = new int[200*200];
//                bitmap.getPixels(pix,0,200,0,0,200,200);


            }
        }
//        bitmap.recycle();
        Log.i("slack", "size:" + bitmapList.size());
        mLogTv.setText("List size:" + bitmapList.size() + "\n");
    }

    private void addToView(final File outFile) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((GifView) findViewById(R.id.gif3)).setMovieResource(outFile);
            }
        });

    }
    private void addToView(final String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((GifView) findViewById(R.id.gif3)).setMovieResource(path);
            }
        });

    }

    Bitmap bitmap;

    public void encodeGif(final List<Bitmap> bitmaps, File outFile) throws IOException {

        GifEncoder gifEncoder = new GifEncoder();
        gifEncoder.setQuality(100);//\
//            gifEncoder.setSize(200,200);
        gifEncoder.start(outFile.getCanonicalPath());
        gifEncoder.setDelay(100); // 500ms between frames

        // Grab frames and encode them
        Iterator<Bitmap> iter = bitmaps.iterator();
        while (iter.hasNext()) {
            bitmap = iter.next();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    mLogTv.append("bitmap size:" + bitmap.getByteCount() + " " + bitmap.getWidth() + " * " + bitmap.getHeight()
                            + " cost: " + ((now - temp) / 1000) + "." + ((now -temp) % 1000 )  + "s" + "\n");
                    temp = now;
                }
            });
            gifEncoder.addFrame(bitmap);
        }

        // Make the gif
        gifEncoder.finish();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                mLogTv.append("file size：" + file.getTotalSpace() +" " + file.getUsableSpace() + " " +
                        file.getFreeSpace() + " cost time:" + ((now - start) / 1000)
                        + "." + ((now -start) % 1000 )  + "s" + "\n");
                Log.i("slack", "file size：" + file.getTotalSpace() + " cost time:" + ((System.currentTimeMillis() - start) / 1000));
            }
        });

        // Add to view
//            Uri picUri = Uri.fromFile(outFile);
        addToView(file);


    }


    public void start(View view) {

        start = System.currentTimeMillis();
        temp = start;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    encodeGif(bitmapList, file);
                    new ClMakerGIF().makeGifThread(bitmapList, path,handler);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("slack", "error:" + e.toString());
                }
            }
        }).start();

//        getGif(bitmapList , "slack/cl");

//        begin(path,200,200,100);
//        initBitmap();
//        end();
    }

    private void initBitmap() {

        Bitmap bitmap = null;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 10; j++) {
                bitmap = BitmapFactory.decodeResource(getResources(), arr[i]);
                Log.i("slack", "bitmap_1:" + bitmap.getByteCount() + "," + bitmap.getHeight() + "," + bitmap.getWidth());
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, 200, 200);
//                bitmap = zoomImage(bitmap,200,200);
                Log.i("slack", "bitmap_2:" + bitmap.getByteCount() + "," + bitmap.getHeight() + "," + bitmap.getWidth());
//                                bitmap.compress(Bitmap.CompressFormat.JPEG, 50 ,new ByteArrayOutputStream());

                int[] pix = new int[200*200];
                bitmap.getPixels(pix,0,200,0,0,200,200);
                writeFrame(pix,200,200,100);

            }
        }

    }

    static{
        System.loadLibrary("gif");
    }
//    public static native void getGif(List<Bitmap> bitmaps,String path);

    public static native void begin(String path,int width,int height,int delay);
    public static native void writeFrame(int[] data, int width, int height, int delay);
    public static native void end();
    public static native void release();
}

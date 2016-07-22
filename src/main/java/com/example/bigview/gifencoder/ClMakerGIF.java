package com.example.bigview.gifencoder;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>Description: 多线程处理任务，合并结果 以空间换时间 </p>
 * Created by slack on 2016/7/22 10:39 .
 */
public class ClMakerGIF {

    ByteArrayOutputStream bos = new ByteArrayOutputStream();// 线程处理输出流的结果
    List<ClOrderEncoder> lzwEncoderList = new ArrayList<>(); // 存放线程处理结果，待全部线程执行完使用
    String outputPath;// GIF 保存路径
    long start; // 开始时间
    Handler hander; // 回调回主线程使用
    int workSize = 0; // 线程队列长度（List<Bitmap> size）
    int length;
    ExecutorService executorService = Executors.newCachedThreadPool(); // 线程池

    /***
     * 多线程处理同一个任务
     * @param source  目前只做了 对 List<Bitmap>  的处理生成GIF
     * @param outputPath 文件输出路径
     * @param hander  全部执行完需要回调
     * @throws IOException
     */
    public void makeGifThread(List<Bitmap> source, String outputPath, Handler hander) throws IOException {
        start = System.currentTimeMillis();
        this.outputPath = outputPath;
        this.hander = hander;

        length = source.size();
        workSize = length;

        for (int i = 0; i < length; i++) {
            Bitmap bmp = source.get(i);
            if (bmp == null) {
                continue;
            }
            executorService.execute(new addGif(bmp, i));

        }

    }


    class addGif implements Runnable {

        Bitmap bitmap;
        ClGifEncoder encoder;
        int order;
        ByteArrayOutputStream byteArrayOutputStream =  new ByteArrayOutputStream();

        public addGif(Bitmap bitmap, int order) {
            this.encoder = new ClGifEncoder();
            this.encoder.setQuality(100);//
            this.encoder.setDelay(100); // 500ms between frames
            this.encoder.start(byteArrayOutputStream,order);
            this.encoder.setFirstFrame(order == 0 ? true : false);
            this.encoder.setRepeat(0);
            this.bitmap = bitmap;
            this.order = order;
        }

        @Override
        public void run() {
            try {
                ClOrderEncoder tempLZWEncoder = this.encoder.addFrame(this.bitmap, this.order);
                // Make the gif
                this.encoder.finishThread(this.order == (length-1) ? true : false,tempLZWEncoder.lzwEncoder);
                tempLZWEncoder.out = byteArrayOutputStream;
                lzwEncoderList.add(tempLZWEncoder);
                long temp = System.currentTimeMillis();
                Log.i("slack", "make gif success by:" + Thread.currentThread().getName() + " :" + order + " cost time:" + ((temp - start) / 1000) + "." + ((temp - start) % 1000) + "s" + "\n");
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("cost_time",( (temp - start)/ 1000f ) + "" );
                bundle.putString("thread",Thread.currentThread().getName() );
                message.setData(bundle);
                hander.sendMessage(message);
                workDone();
            } catch (Exception e) {
                Log.i("slack", "error:" + e.toString());
                e.printStackTrace();
                System.gc();
            }
        }
    }

    private synchronized void workDone() throws IOException {
        workSize --;
        Log.i("slack","workSize:"+workSize + " :" +  Thread.currentThread().getName() );
        if (workSize == 0) {
            //排序 默认从小到大
            Collections.sort(lzwEncoderList);
            for (ClOrderEncoder myLZWEncoder : lzwEncoderList) {
//                Log.i("slack","order:"+myLZWEncoder.order);
                try {
                    bos.write(myLZWEncoder.out.toByteArray());
//                    Log.i("slack","order:"+myLZWEncoder.order +"," + o.size() + " ," + bos.size());
                } catch (IOException e) {
                    e.printStackTrace();
                    System.gc();
                }
            }
//            bos.write(0x3b); // gif traile
            byte[] data = bos.toByteArray();
            File file = new File(outputPath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(data);
                fileOutputStream.flush();
                fileOutputStream.close();
                long now = System.currentTimeMillis();
                Log.i("slack", "make gif success at:" + file.getAbsolutePath() + " cost time:" + ((now - start) / 1000) + "." + ((now - start) % 1000) + "s" + "\n");
                hander.sendEmptyMessage(100); // 这里发送了一个空的消息0到MessageQueue

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

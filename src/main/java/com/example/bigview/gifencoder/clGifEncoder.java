package com.example.bigview.gifencoder;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>Description: 扩展 GifEncoder， 为多线程处理任务 </p>
 * Created by slack on 2016/7/22 09:57 .
 */
public class ClGifEncoder extends  GifEncoder{

    // slack add
    public ClOrderEncoder addFrame(Bitmap im, int order) {
        if ((im == null) || !started) {
            return null;
        }
        boolean ok = true;
        ClLzwEncoder lzwEncoder = null;
        try {
            if (!sizeSet) {
                // use first frame's size
                setSize(im.getWidth(), im.getHeight());
            }
            image = im;
            getImagePixels(); // convert to correct format if necessary
            analyzePixels(); // build color table & map pixels
            if (firstFrame) {
                writeLSD(); // logical screen descriptior
                writePalette(); // global color table
                if (repeat >= 0) {
                    // use NS app extension to indicate reps
                    writeNetscapeExt();
                }
            }
            writeGraphicCtrlExt(); // write graphic control extension
            writeImageDesc(); // image descriptor
            if (!firstFrame) {
                writePalette(); // local color table
            }
//            writePixels(); // encode and write pixel data
            lzwEncoder = waitWritePixels();
        } catch (IOException e) {
            ok = false;
        }
        if(ok && lzwEncoder != null){
            return new ClOrderEncoder(lzwEncoder,order);
        }else{
            return null;
        }
    }

    public void setSize(int w, int h) {
        width = w;
        height = h;
        if (width < 1)
            width = 200;
        if (height < 1)
            height = 200;
        sizeSet = true;
    }

    // slack add
    public void setFirstFrame(boolean firstFrame) {
        this.firstFrame = firstFrame;
    }

    // slack add
    public boolean start(OutputStream os, int num) {
        if (os == null)
            return false;
        boolean ok = true;
        closeStream = false;
        out = os;
        if(num == 0){ // 第一个
            try {
                writeString("GIF89a"); // header
            } catch (IOException e) {
                ok = false;
            }
        }
        return started = ok;
    }

    // slack add
    protected ClLzwEncoder waitWritePixels() throws IOException {
        return new ClLzwEncoder(width, height, indexedPixels, colorDepth);
    }

    // slack add
    public boolean finishThread(boolean islast,LzwEncoder lzwEncoder) {
        if (!started)
            return false;
        boolean ok = true;
        started = false;

        try {
            lzwEncoder.encode(out);
            if(islast){
                out.write(0x3b); // gif trailer
            }
            out.flush();
            if (closeStream) {
                out.close();
            }
        } catch (IOException e) {
            ok = false;
        }

        // reset for subsequent use
        transIndex = 0;
        out = null;
        image = null;
        pixels = null;
        indexedPixels = null;
        colorTab = null;
        closeStream = false;
        firstFrame = true;

        return ok;
    }


}

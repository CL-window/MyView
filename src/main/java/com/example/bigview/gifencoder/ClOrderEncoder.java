package com.example.bigview.gifencoder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * <p>Description: 自定义LZWEncoder ，可以实现排序，用于多线程任务重新按序组合  </p>
 * Created by slack on 2016/7/22 10:16 .
 */
public class ClOrderEncoder implements Comparable<ClOrderEncoder>{

    public ClLzwEncoder lzwEncoder;
    int order;
    // 存储中间变量  encoder.encode(out); 里需要的数据
    public ByteArrayOutputStream out;


    public ClOrderEncoder(ClLzwEncoder lzwEncoder, int order) {
        this.lzwEncoder = lzwEncoder;
        this.order = order;
    }

    public ClOrderEncoder(ClLzwEncoder lzwEncoder, int order, ByteArrayOutputStream out) {
        this.lzwEncoder = lzwEncoder;
        this.order = order;
        this.out = out;
    }


    // 根据 order  默认从小到大排序
    @Override
    public int compareTo(ClOrderEncoder another) {
        // 按 order 排序
        return (this.order - another.order);
    }
}

package com.example.bigview.gifencoder;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Description: 自定义LZWEncoder ，在原作者基础上现在方法，
 * 把源文件里的几个需要的参数改为public  </p>
 * Created by slack on 2016/7/22 09:55 .
 */
public class ClLzwEncoder extends LzwEncoder{


    ClLzwEncoder(int width, int height, byte[] pixels, int color_depth) {
        super(width, height, pixels, color_depth);
    }

    // slack add
    OutputStream encodeOut(OutputStream os) throws IOException {
        os.write(initCodeSize); // write "initial code size" byte

        remaining = imgW * imgH; // reset navigation variables
        curPixel = 0;

        compress(initCodeSize + 1, os); // compress and write the pixel data

        os.write(0); // write block terminator
        return os;
    }
}

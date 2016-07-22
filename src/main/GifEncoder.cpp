/**
 * @author : xiaozhuai
 * @date   : 16/7/20
 */

#include "GifEncoder.h"


//GifWriter gifWriter;
//GifBegin(&gifWriter,[self.GIFPath UTF8String],200,200,10);
//GifWriteFrame(&gifWriter, rawDataBytesRGBA_width200, 200, 200, 10); //每一帧执行write
//GifEnd(&gifWriter);

namespace WuTa {


void GifEncoder::begin(char *path, int width, int height, int delay) {
    GifEncoder::gifWriter = new GifWriter();
    GifBegin(GifEncoder::gifWriter, path, width, height, delay);
}

void GifEncoder::writeFrame(uchar *data, int width, int height, int delay) {
    //ARGB to RGBA
    int* pixels = (int*)data;
    for(int i=0; i<width*height; i++){
        *(pixels+i) = ( *(pixels+i) << 8 ) | 0x000000ff;
    }


    GifWriteFrame(GifEncoder::gifWriter, data, width, height, delay);
}

void GifEncoder::end() {
    GifEnd(GifEncoder::gifWriter);
}

void GifEncoder::release() {
    if(GifEncoder::gifWriter != NULL){
        delete GifEncoder::gifWriter;
        GifEncoder::gifWriter = NULL;
    }
}

}
/**
 * @author : xiaozhuai
 * @date   : 16/7/20
 */

#ifndef WUTANATIVE_GIFENCODER_H
#define WUTANATIVE_GIFENCODER_H

#include "codeGIF.h"

typedef unsigned char uchar;

namespace WuTa {

class GifEncoder {
public:
    static void begin(char* path, int width, int height, int delay);
    static void writeFrame(uchar* data, int width, int height, int delay);
    static void end();
    static void release();

private:
    static GifWriter* gifWriter;
};

}


#endif //WUTANATIVE_GIFENCODER_H

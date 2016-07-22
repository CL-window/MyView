#include <jni.h>
#include "android/log.h"
//#include "GifEncoder.h"
//#include <string>
//using namespace std;

//JNIEXPORT void JNICALL Java_com_example_bigview_MainActivity_getGif
//(JNIEnv * env, jobject instance, jobject object, jstring data){
//
//// 日志输出
//__android_log_write(ANDROID_LOG_INFO,"slack","This is a Test...");
//
//
//
//}

//JNIEXPORT void JNICALL Java_com_example_bigview_MainActivity_begin
//(JNIEnv * env, jobject instance,jstring path,jint width,jint height,jint delay){
//// 日志输出
//__android_log_write(ANDROID_LOG_INFO,"slack","This is a begin...");
//    string p = jstring2stdString(env,path);
//    GifEncoder.begin(p.c_str(),width,height,delay);
//
//}
//JNIEXPORT void JNICALL Java_com_example_bigview_MainActivity_writeFrame
//(JNIEnv * env, jobject instance,int[] data, int width, int height, int delay)){
//// 日志输出
//__android_log_write(ANDROID_LOG_INFO,"slack","This is a writeFrame...");
//
//uchar * d = (uchar *)env->GetIntArrayElements(data,NULL);
//GifEncoder.writeFrame(d ,width,height,delay);
//
////env->ReleaseIntArrayElements(data,d,0);//释放~~~
//
//}
JNIEXPORT void JNICALL Java_com_example_bigview_MainActivity_end
(JNIEnv * env, jclass instance){
// 日志输出
__android_log_write(ANDROID_LOG_INFO,"slack","This is a end...");

//GifEncoder.end();


}
//JNIEXPORT void JNICALL Java_com_example_bigview_MainActivity_release
//(JNIEnv * env, jobject instance){
//// 日志输出
//__android_log_write(ANDROID_LOG_INFO,"slack","This is a release...");
////GifEncoder.release();
//
//}

//string jstring2stdString(JNIEnv * env, jstring jstr){ char* rtn = NULL;    jclass clsstring = env->FindClass("java/lang/String");    jstring strencode = env->NewStringUTF("utf-8");    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");    jbyteArray barr = (jbyteArray)env->CallObjectMethod(jstr, mid, strencode);    jsize alen = env->GetArrayLength(barr);    jbyte* ba = env->GetByteArrayElements(barr,JNI_FALSE);    if(alen > 0)    {        rtn = (char*)malloc(alen+1);        memcpy(rtn,ba,alen);        rtn[alen]=0;    }    env->ReleaseByteArrayElements(barr,ba,0);    string stemp(rtn);    free(rtn);    return stemp;}

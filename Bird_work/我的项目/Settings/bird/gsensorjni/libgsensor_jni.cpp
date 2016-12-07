#define LOG_TAG "GSENSORJNI"

#include <jni.h>
#include <utils/Log.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <termios.h>
#include <pthread.h>
#include <linux/serial.h> 
#include "gsensor.h" 

#define GSENSOR_DEVICE_NAME      "/dev/gsensor"

static int g_gsensor_fd = -1;

#ifdef _cplusplus
extern "C" {
#endif

jboolean opendev(JNIEnv *env, jobject thiz){
    int ret;

    g_gsensor_fd = open(GSENSOR_DEVICE_NAME, O_RDONLY);
    if (g_gsensor_fd < 0){
	ALOGE("g_gsensor_fd=%d",g_gsensor_fd);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jboolean closedev(JNIEnv *env, jobject thiz){

    if(g_gsensor_fd != -1){   
        if(g_gsensor_fd != -1){
	        close(g_gsensor_fd);
	        g_gsensor_fd = -1;
	}
        return JNI_TRUE;    
    }
    else{
        return JNI_FALSE;
    }
}

jboolean gsensor_calibration(JNIEnv *env, jobject thiz){

	int ret;

	ALOGE("gsensor_calibration");

        g_gsensor_fd = open(GSENSOR_DEVICE_NAME, O_RDONLY);
	ALOGE("g_gsensor_fd=%d",g_gsensor_fd);
	if (g_gsensor_fd < 0){
		return JNI_FALSE;
	}
	ret = ioctl(g_gsensor_fd, GSENSOR_IOCTL_SET_CALIBRATION, 0);
	close(g_gsensor_fd);
	if (ret < 0){
		return JNI_FALSE;
	}
        return JNI_TRUE;
}

#ifdef _cplusplus
}
#endif

//JNI register
////////////////////////////////////////////////////////////////
static const char *classPathName = "com/bird/settings/sensornative/GSensorNative";

static JNINativeMethod methods[] = {
  {"opendev", "()Z", (void*)opendev },
  {"closedev", "()Z", (void*)closedev },
  {"gsensor_calibration", "()Z", (void*)gsensor_calibration},
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
         return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
 
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}


// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */
 
typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;
     
    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        goto bail;
    }
    
    result = JNI_VERSION_1_4;
    
bail:
    return result;
}


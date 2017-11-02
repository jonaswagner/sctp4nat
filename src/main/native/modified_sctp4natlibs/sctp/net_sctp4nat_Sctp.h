 /*
 * Jonas Wagner made several changes
 * 
 * 1. Renamed classes and methods to guarantee the JNI interface to work
 * 2. Added usrsctp_shutdown
 *
 */
 
#include <jni.h>
/* Header for class net_sctp4nat_Sctp */

#ifndef _Included_net_sctp4nat_Sctp
#define _Included_net_sctp4nat_Sctp
#ifdef __cplusplus
extern "C" {
#endif
#undef net_sctp4nat_Sctp_MSG_NOTIFICATION
#define net_sctp4nat_Sctp_MSG_NOTIFICATION 8192L
/*
 * Class:     net_sctp4nat_Sctp
 * Method:    on_network_in
 * Signature: (J[BII)V
 */
JNIEXPORT void JNICALL Java_net_sctp4nat_Sctp_on_1network_1in
  (JNIEnv *, jclass, jlong, jbyteArray, jint, jint);

/*
 * Class:     net_sctp4nat_Sctp
 * Method:    usrsctp_accept
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_net_sctp4nat_Sctp_usrsctp_1accept
  (JNIEnv *, jclass, jlong);

/*
 * Class:     net_sctp4nat_Sctp
 * Method:    usrsctp_close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_sctp4nat_Sctp_usrsctp_1close
  (JNIEnv *, jclass, jlong);
  
/* 
 * Class:     net_sctp4nat_Sctp
 * Method:    usrsctp_shutdown
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_sctp4nat_Sctp_usrsctp_1shutdown
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     net_sctp4nat_Sctp
 * Method:    usrsctp_connect
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_net_sctp4nat_Sctp_usrsctp_1connect
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     net_sctp4nat_Sctp
 * Method:    usrsctp_finish
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_net_sctp4nat_Sctp_usrsctp_1finish
  (JNIEnv *, jclass);

/*
 * Class:     net_sctp4nat_Sctp
 * Method:    usrsctp_init
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_net_sctp4nat_Sctp_usrsctp_1init
  (JNIEnv *, jclass, jint);

/*
 * Class:     net_sctp4nat_Sctp
 * Method:    usrsctp_listen
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_sctp4nat_Sctp_usrsctp_1listen
  (JNIEnv *, jclass, jlong);

/*
 * Class:     net_sctp4nat_Sctp
 * Method:    usrsctp_send
 * Signature: (J[BIIZII)I
 */
JNIEXPORT jint JNICALL Java_net_sctp4nat_Sctp_usrsctp_1send
  (JNIEnv *, jclass, jlong, jbyteArray, jint, jint, jboolean, jint, jint);

/*
 * Class:     net_sctp4nat_Sctp
 * Method:    usrsctp_socket
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_net_sctp4nat_Sctp_usrsctp_1socket
  (JNIEnv *, jclass, jint);

#ifdef __cplusplus
}
#endif
#endif

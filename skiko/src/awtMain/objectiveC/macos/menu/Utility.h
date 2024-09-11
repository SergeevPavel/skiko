#pragma once
#include <tuple>

#import <Cocoa/Cocoa.h>
#import <jni.h>

NSString* JStringToNSString(JNIEnv *env, jstring jstr);
NSUInteger JavaModifiersToNsKeyModifiers(jint javaModifiers, BOOL isExtMods);
unichar AWTKeyToMacShortcut(jint awtKey, BOOL doShift);

std::tuple<NSString*, NSUInteger> awtToMacAccelerator(jchar shortcutKey, jint shortcutKeyCode, jint modifiers);
NSImage* javaArrayToNSImage(JNIEnv* env, jbyteArray iconData);

jint getJNIEnvironment(JNIEnv** env);
jmethodID getJavaMethod(JNIEnv* env, jobject javaObject, const char* methodName, const char* methodSignature);

#define JNI_COCOA_ENTER() \
 @autoreleasepool { \
 @try {

#define JNI_COCOA_EXIT() \
 } \
 @catch (NSException *e) { \
     NSLog(@"%@", [e callStackSymbols]); \
 } \
 } // @autoreleasepool
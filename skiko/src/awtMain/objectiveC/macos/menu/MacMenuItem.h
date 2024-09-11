#pragma once

#import <Cocoa/Cocoa.h>
#import <jni.h>

@interface MacMenuItem : NSObject {
@public
    jobject javaObject;
}

@property (nonatomic, strong) NSMenuItem* menuItem;
@property (nonatomic) BOOL enabled;
@property (nonatomic) NSImage* image;
@property (nonatomic) NSString* title;

// - Lifecycle
- (id) initWithJavaObject:(jobject)javaObject andMenuItem:(NSMenuItem*)menuItem;
- (void) setKeyEquivalent:(NSString*)theKeyEquivalent andModifierMask:(NSUInteger)modifierMask;

@end
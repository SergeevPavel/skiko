#pragma once

#import <Cocoa/Cocoa.h>
#import <jni.h>

#import "MacMenuItem.h"

@interface MacMenu : NSObject {
@private
    jobject javaObject;
}

@property (nonatomic, strong) NSMenu* menu;
@property (nonatomic, weak) NSMenuItem* parentMenuItem;
@property (nonatomic) BOOL enabled;
@property (nonatomic) NSString* title;

// - Lifecycle
- (id) initWithJavaObject:(jobject)javaObject andMenu:(NSMenu*)menu;
- (void) addItem:(MacMenuItem*)macMenuItem;
- (void) addMenu:(MacMenu*)macMenu;
- (void) removeItem:(MacMenuItem*)macMenuItem;
- (void) removeMenu:(MacMenu*)macMenu;

@end
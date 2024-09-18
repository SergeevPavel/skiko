#include <iostream>

#import "Utility.h"
#import "MacMenuItem.h"

// TODO: generate these files
#import "java_awt_event_KeyEvent.h"
#import "java_awt_event_InputEvent.h"


extern "C" {

// MacMenuItem
JNIEXPORT jlong JNICALL
Java_org_jetbrains_skiko_menu_MacMenuItem_nativeCreate
(JNIEnv *env, jobject javaObject, jstring title)
{
    JNI_COCOA_ENTER();

    jobject javaObjectReference = env->NewGlobalRef(javaObject);

    NSMenuItem* menuItem = [[NSMenuItem alloc] init];
    [menuItem setTitle: JStringToNSString(env, title)];

    MacMenuItem* macMenuItem = [[MacMenuItem alloc] initWithJavaObject:javaObjectReference andMenuItem:menuItem];

    // Transfer ownership of macMenuItem from ARC environment
    return (jlong)(__bridge_retained void*)macMenuItem;

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenuItem_nativeDispose
(JNIEnv *env, jobject javaObject, jlong menuItemPointer)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        // Transfer ownership of macMenuItem back to ARC environment
        // that will effectively lead to releasing of macMenuItem
        MacMenuItem* macMenuItem = (__bridge_transfer MacMenuItem*)(void*)(menuItemPointer);
    });

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenuItem_nativeSetAccelerator
(JNIEnv *env, jobject javaObject, jlong menuItemPointer, jchar shortcutKey, jint shortcutKeyCode, jint modifiers)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        auto [theKeyEquivalent, modifierMask] = awtToMacAccelerator(shortcutKey, shortcutKeyCode, modifiers);

        MacMenuItem* macMenuItem = (__bridge MacMenuItem*)(void*)(menuItemPointer);
        [macMenuItem setKeyEquivalent:theKeyEquivalent andModifierMask:modifierMask];
    });

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenuItem_nativeSetIcon
(JNIEnv *env, jobject javaObject, jlong menuItemPointer, jbyteArray iconData)
{
    JNI_COCOA_ENTER();

    NSImage* image = javaArrayToNSImage(env, iconData);

    dispatch_async(dispatch_get_main_queue(), ^{
        MacMenuItem* macMenuItem = (__bridge MacMenuItem*)(void*)(menuItemPointer);
        macMenuItem.image = image;
    });

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenuItem_nativeSetTitle
(JNIEnv *env, jobject javaObject, jlong menuItemPointer, jstring javaTitle)
{
    JNI_COCOA_ENTER();

    NSString* title = JStringToNSString(env, javaTitle);

    dispatch_async(dispatch_get_main_queue(), ^{
        MacMenuItem* macMenuItem = (__bridge MacMenuItem*)(void*)(menuItemPointer);
        macMenuItem.title = title;
    });

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenuItem_nativeSetEnabled
(JNIEnv *env, jobject javaObject, jlong menuItemPointer, jboolean isEnabled)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        MacMenuItem* macMenuItem = (__bridge MacMenuItem*)(void*)(menuItemPointer);
        macMenuItem.enabled = isEnabled == JNI_TRUE;
    });

    JNI_COCOA_EXIT();
}

// MacMenuItemSeparator
JNIEXPORT jlong JNICALL
Java_org_jetbrains_skiko_menu_MacMenuItemSeparator_nativeCreate
(JNIEnv *env, jobject javaObject)
{
    JNI_COCOA_ENTER();

    jobject javaObjectReference = env->NewGlobalRef(javaObject);

    NSMenuItem* menuItem = [NSMenuItem separatorItem];
    MacMenuItem* macMenuItem = [[MacMenuItem alloc] initWithJavaObject:javaObjectReference andMenuItem:menuItem];

    // NOTE: returns retained Menu
    // Java owner must release it manually via nativeDisposeMenuItem (see MenuItem.dispose())
    return (jlong)(__bridge_retained void*)macMenuItem;

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenuItemSeparator_nativeDispose
(JNIEnv *env, jobject javaObject, jlong menuItemPointer)
{
    Java_org_jetbrains_skiko_menu_MacMenuItem_nativeDispose(env, javaObject, menuItemPointer);
}

} // extern "C"

@implementation MacMenuItem

// - Lifecycle
- (id) initWithJavaObject:(jobject)javaObject andMenuItem:(NSMenuItem*)menuItem {
    self = [super init];
    if (self) {
        self->javaObject = javaObject;
        self.menuItem = menuItem;

        [menuItem setAction:@selector(menuItemAction:)];
        [menuItem setTarget:self];
    }

    // <DEBUG>
    // NSLog(@"MacMenuItem: initWithJavaObject");
    // </DEBUG>

    return self;
}

- (void) dealloc {
    JNIEnv* env = nullptr;

    if (getJNIEnvironment(&env) == JNI_OK) {
        env->DeleteGlobalRef(javaObject);
    }

    // <DEBUG>
    // NSLog(@"MacMenuItem: dealloc");
    // </DEBUG>
}

// - Private
- (void) menuItemAction:(id)sender {
    JNIEnv* env = nullptr;

    if (getJNIEnvironment(&env) != JNI_OK) {
        std::cerr << "MacMenuItem: failed to get JNI Environment" << std::endl;
        return;
    }

    jmethodID handleActionMethod = getJavaMethod(env, javaObject, "handleAction", "()V");

    env->CallVoidMethod(javaObject, handleActionMethod);
}

- (void) setKeyEquivalent:(NSString*)theKeyEquivalent andModifierMask:(NSUInteger)modifierMask {
    self.menuItem.keyEquivalent = theKeyEquivalent;
    self.menuItem.keyEquivalentModifierMask = modifierMask;
}

- (void) setEnabled:(BOOL)value {
    self.menuItem.enabled = value;
}

- (BOOL) isEnabled {
    return self.menuItem.enabled;
}

- (void) setImage:(NSImage*)image {
    self.menuItem.image = image;
}

- (NSImage*) getImage {
    return self.menuItem.image;
}

- (void) setTitle:(NSString*)title {
    self.menuItem.title = title;
}

- (NSString*) getTitle {
    return self.menuItem.title;
}

@end
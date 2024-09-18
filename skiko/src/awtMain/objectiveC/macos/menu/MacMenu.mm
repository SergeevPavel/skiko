#import "MacMenu.h"
#import "Utility.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_org_jetbrains_skiko_menu_MacMenu_nativeCreate
(JNIEnv *env, jobject javaObject, jstring label)
{
    JNI_COCOA_ENTER();

    jobject javaObjectReference = env->NewGlobalRef(javaObject);

    NSMenu* menu = [[NSMenu alloc] init];
    [menu setTitle: JStringToNSString(env, label)];

    MacMenu* macMenu = [[MacMenu alloc] initWithJavaObject:javaObjectReference andMenu:menu];

    // Transfer ownership of macMenu from ARC environment
    return (jlong)(__bridge_retained void*)macMenu;

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenu_nativeDispose
(JNIEnv *env, jobject javaObject, jlong menuPointer)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        // Transfer ownership of macMenu back to ARC environment
        // that will effectively lead to releasing of macMenu
        MacMenu* macMenu = (__bridge_transfer MacMenu*)(void*)(menuPointer);
    });

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenu_nativeAddMenuItem
(JNIEnv *env, jobject javaObject, jlong menuPointer, jlong menuItemPointer)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        MacMenu* macMenu = (__bridge MacMenu*)(void*)(menuPointer);
        MacMenuItem* macMenuItem = (__bridge MacMenuItem*)(void*)(menuItemPointer);

        [macMenu addItem: macMenuItem];
    });

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenu_nativeAddMenu
(JNIEnv *env, jobject javaObject, jlong menuPointer, jlong otherMenuPointer)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        MacMenu* macMenu = (__bridge MacMenu*)(void*)(menuPointer);
        MacMenu* macOtherMenu = (__bridge MacMenu*)(void*)(otherMenuPointer);

        [macMenu addMenu: macOtherMenu];
    });

    JNI_COCOA_EXIT();
}


JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenu_nativeRemoveMenuItem
(JNIEnv *env, jobject javaObject, jlong menuPointer, jlong menuItemPointer)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        MacMenu* macMenu = (__bridge MacMenu*)(void*)(menuPointer);
        MacMenuItem* macMenuItem = (__bridge MacMenuItem*)(void*)(menuItemPointer);

        [macMenu removeItem: macMenuItem];
    });

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenu_nativeRemoveMenu
(JNIEnv *env, jobject javaObject, jlong menuPointer, jlong otherMenuPointer)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        MacMenu* macMenu = (__bridge MacMenu*)(void*)(menuPointer);
        MacMenu* macOtherMenu = (__bridge MacMenu*)(void*)(otherMenuPointer);

        [macMenu removeMenu: macOtherMenu];
    });

    JNI_COCOA_EXIT();
}

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenu_nativeSetEnabled
(JNIEnv *env, jobject javaObject, jlong menuPointer, jboolean isEnabled)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        MacMenu* macMenu = (__bridge MacMenu*)(void*)(menuPointer);
        macMenu.enabled = isEnabled == JNI_TRUE;
    });

    JNI_COCOA_EXIT();
}


JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenu_nativeSetTitle
(JNIEnv *env, jobject javaObject, jlong menuPointer, jstring javaTitle)
{
    JNI_COCOA_ENTER();

    NSString* title = JStringToNSString(env, javaTitle);

    dispatch_async(dispatch_get_main_queue(), ^{
        MacMenu* macMenu = (__bridge MacMenu*)(void*)(menuPointer);
        macMenu.title = title;
    });

    JNI_COCOA_EXIT();
}

} // extern "C"

@implementation MacMenu

// - Lifecycle
- (id) initWithJavaObject:(jobject)javaObject andMenu:(NSMenu*)menu {
    self = [super init];
    if (self) {
        [menu setAutoenablesItems:NO];
        self->javaObject = javaObject;
        self.menu = menu;
    }

    // <DEBUG>
    // NSLog(@"MacMenu: initWithJavaObject");
    // </DEBUG>

    return self;
}

- (void) dealloc {
    JNIEnv* env = nullptr;

    if (getJNIEnvironment(&env) == JNI_OK) {
        env->DeleteGlobalRef(javaObject);
    }

    // <DEBUG>
    // NSLog(@"MacMenu: dealloc");
    // </DEBUG>
}

- (void) addItem:(MacMenuItem*)macMenuItem
{
    [self.menu addItem: macMenuItem.menuItem];
}

- (void) addMenu:(MacMenu*)macMenu
{
    NSMenuItem* subMenuItem = [[NSMenuItem alloc] init];
    subMenuItem.title = macMenu.menu.title;

    macMenu.parentMenuItem = subMenuItem;

    [subMenuItem setSubmenu:macMenu.menu];
    [self.menu addItem: subMenuItem];
}

- (void) removeItem:(MacMenuItem*)macMenuItem
{
    [self.menu removeItem: macMenuItem.menuItem];
}

- (void) removeMenu:(MacMenu*)macMenu
{
    if (self.parentMenuItem == nil) return;

    [self.menu removeItem: macMenu.parentMenuItem];
}

- (void) setEnabled:(BOOL)value
{
    if (self.parentMenuItem == nil) return;

    self.parentMenuItem.enabled = value;
}

- (BOOL) isEnabled
{
    if (self.parentMenuItem == nil) return NO;

    return self.parentMenuItem.enabled;
}

- (void) setTitle:(NSString*)title {
    if (self.parentMenuItem != nil) {
        self.parentMenuItem.title = title;
    }

    self.menu.title = title;
}

- (NSString*) getTitle {
    return self.menu.title;
}

@end
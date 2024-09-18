#import "MacMenu.h"
#import "Utility.h"

extern "C" {

JNIEXPORT void JNICALL
Java_org_jetbrains_skiko_menu_MacMenuBar_nativeSetMainMenu
(JNIEnv *env, jobject javaObject, jlong menuPointer)
{
    JNI_COCOA_ENTER();

    dispatch_async(dispatch_get_main_queue(), ^{
        if (menuPointer == 0L) {
            [NSApp setMainMenu:[[NSMenu alloc] init]];
            return;
        }

        MacMenu* macMenu = (__bridge MacMenu*)(void*)(menuPointer);

        [NSApp setMainMenu:macMenu.menu];
    });

    JNI_COCOA_EXIT();
}

} // extern "C"
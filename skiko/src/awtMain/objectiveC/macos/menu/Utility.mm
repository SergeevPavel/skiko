#include <iostream>

#include "Utility.h"
#include "common/interop.hh"

// TODO: generate these files
#import "java_awt_event_KeyEvent.h"
#import "java_awt_event_InputEvent.h"

// Global pointer to JavaVM object. Declared in Library.cc
extern JavaVM *jvm;

static struct _nsKeyToJavaModifier
{
    NSUInteger nsMask;
    //NSUInteger cgsLeftMask;
    //NSUInteger cgsRightMask;
    unsigned short leftKeyCode;
    unsigned short rightKeyCode;
    BOOL leftKeyPressed;
    BOOL rightKeyPressed;
    jint javaExtMask;
    jint javaMask;
    jint javaKey;
}

nsKeyToJavaModifierTable[] =
{
    {
        NSAlphaShiftKeyMask,
                0,
                0,
                NO,
                NO,
                0, // no Java equivalent
                0, // no Java equivalent
                java_awt_event_KeyEvent_VK_CAPS_LOCK
    },
    {
        NSShiftKeyMask,
                //kCGSFlagsMaskAppleShiftKey,
                //kCGSFlagsMaskAppleRightShiftKey,
                56,
                60,
                NO,
                NO,
                java_awt_event_InputEvent_SHIFT_DOWN_MASK,
                java_awt_event_InputEvent_SHIFT_MASK,
                java_awt_event_KeyEvent_VK_SHIFT
    },
    {
        NSControlKeyMask,
                //kCGSFlagsMaskAppleControlKey,
                //kCGSFlagsMaskAppleRightControlKey,
                59,
                62,
                NO,
                NO,
                java_awt_event_InputEvent_CTRL_DOWN_MASK,
                java_awt_event_InputEvent_CTRL_MASK,
                java_awt_event_KeyEvent_VK_CONTROL
    },
    {
        NSCommandKeyMask,
                //kCGSFlagsMaskAppleLeftCommandKey,
                //kCGSFlagsMaskAppleRightCommandKey,
                55,
                54,
                NO,
                NO,
                java_awt_event_InputEvent_META_DOWN_MASK,
                java_awt_event_InputEvent_META_MASK,
                java_awt_event_KeyEvent_VK_META
    },
    {
        NSAlternateKeyMask,
                //kCGSFlagsMaskAppleLeftAlternateKey,
                //kCGSFlagsMaskAppleRightAlternateKey,
                58,
                61,
                NO,
                NO,
                java_awt_event_InputEvent_ALT_DOWN_MASK,
                java_awt_event_InputEvent_ALT_MASK,
                java_awt_event_KeyEvent_VK_ALT
    },
    // NSNumericPadKeyMask
    {
        NSHelpKeyMask,
                0,
                0,
                NO,
                NO,
                0, // no Java equivalent
                0, // no Java equivalent
                java_awt_event_KeyEvent_VK_HELP
    },
    // NSFunctionKeyMask
    {0, 0, 0, NO, NO, 0, 0, 0}
};

NSString* JStringToNSString(JNIEnv *env, jstring javaString) {
    if (javaString == NULL) return NULL;

    jsize len = env->GetStringLength(javaString);
    const jchar *chars = env->GetStringChars(javaString, NULL);

    if (chars == NULL) {
        return NULL;
    }

    NSString *result = [NSString stringWithCharacters:(UniChar *)chars length:len];
    env->ReleaseStringChars(javaString, chars);

    return result;
}

// returns NSEventModifierFlags
NSUInteger JavaModifiersToNsKeyModifiers(jint javaModifiers, BOOL isExtMods)
{
    NSUInteger nsFlags = 0;
    struct _nsKeyToJavaModifier* cur;

    for (cur = nsKeyToJavaModifierTable; cur->nsMask != 0; ++cur) {
        jint mask = isExtMods? cur->javaExtMask : cur->javaMask;
        if ((mask & javaModifiers) != 0) {
            nsFlags |= cur->nsMask;
        }
    }
    return nsFlags;
}

unichar AWTKeyToMacShortcut(jint awtKey, BOOL doShift)
{
    unichar macKey = 0;

    if ((awtKey >= java_awt_event_KeyEvent_VK_0 && awtKey <= java_awt_event_KeyEvent_VK_9) ||
        (awtKey >= java_awt_event_KeyEvent_VK_A && awtKey <= java_awt_event_KeyEvent_VK_Z))
    {
        // These ranges are the same in ASCII
        macKey = awtKey;
    } else if (awtKey >= java_awt_event_KeyEvent_VK_F1 && awtKey <= java_awt_event_KeyEvent_VK_F12) {
        // Support for F1 - F12 has been around since Java 1.0 and fall into a lower range.
        macKey = awtKey - java_awt_event_KeyEvent_VK_F1 + NSF1FunctionKey;
    } else if (awtKey >= java_awt_event_KeyEvent_VK_F13 && awtKey <= java_awt_event_KeyEvent_VK_F24) {
        // Support for F13-F24 came in Java 1.2 and are at a different range.
        macKey = awtKey - java_awt_event_KeyEvent_VK_F13 + NSF13FunctionKey;
    } else {
        // Special characters
        switch (awtKey) {
            case java_awt_event_KeyEvent_VK_BACK_QUOTE      : macKey = '`'; break;
            case java_awt_event_KeyEvent_VK_QUOTE           : macKey = '\''; break;

            case java_awt_event_KeyEvent_VK_ESCAPE          : macKey = 0x1B; break;
            case java_awt_event_KeyEvent_VK_SPACE           : macKey = ' '; break;
            case java_awt_event_KeyEvent_VK_PAGE_UP         : macKey = NSPageUpFunctionKey; break;
            case java_awt_event_KeyEvent_VK_PAGE_DOWN       : macKey = NSPageDownFunctionKey; break;
            case java_awt_event_KeyEvent_VK_END             : macKey = NSEndFunctionKey; break;
            case java_awt_event_KeyEvent_VK_HOME            : macKey = NSHomeFunctionKey; break;

            case java_awt_event_KeyEvent_VK_LEFT            : macKey = NSLeftArrowFunctionKey; break;
            case java_awt_event_KeyEvent_VK_UP              : macKey = NSUpArrowFunctionKey; break;
            case java_awt_event_KeyEvent_VK_RIGHT           : macKey = NSRightArrowFunctionKey; break;
            case java_awt_event_KeyEvent_VK_DOWN            : macKey = NSDownArrowFunctionKey; break;

            case java_awt_event_KeyEvent_VK_COMMA           : macKey = ','; break;

                // Mac OS doesn't distinguish between the two '-' keys...
            case java_awt_event_KeyEvent_VK_MINUS           :
            case java_awt_event_KeyEvent_VK_SUBTRACT        : macKey = '-'; break;

                // or the two '.' keys...
            case java_awt_event_KeyEvent_VK_DECIMAL         :
            case java_awt_event_KeyEvent_VK_PERIOD          : macKey = '.'; break;

                // or the two '/' keys.
            case java_awt_event_KeyEvent_VK_DIVIDE          :
            case java_awt_event_KeyEvent_VK_SLASH           : macKey = '/'; break;

            case java_awt_event_KeyEvent_VK_SEMICOLON       : macKey = ';'; break;
            case java_awt_event_KeyEvent_VK_EQUALS          : macKey = '='; break;

            case java_awt_event_KeyEvent_VK_OPEN_BRACKET    : macKey = '['; break;
            case java_awt_event_KeyEvent_VK_BACK_SLASH      : macKey = '\\'; break;
            case java_awt_event_KeyEvent_VK_CLOSE_BRACKET   : macKey = ']'; break;

            case java_awt_event_KeyEvent_VK_MULTIPLY        : macKey = '*'; break;
            case java_awt_event_KeyEvent_VK_ADD             : macKey = '+'; break;

            case java_awt_event_KeyEvent_VK_HELP            : macKey = NSHelpFunctionKey; break;
            case java_awt_event_KeyEvent_VK_TAB             : macKey = NSTabCharacter; break;
            case java_awt_event_KeyEvent_VK_ENTER           : macKey = NSNewlineCharacter; break;
            case java_awt_event_KeyEvent_VK_BACK_SPACE      : macKey = NSBackspaceCharacter; break;
            case java_awt_event_KeyEvent_VK_DELETE          : macKey = NSDeleteCharacter; break;
            case java_awt_event_KeyEvent_VK_CLEAR           : macKey = NSClearDisplayFunctionKey; break;
            case java_awt_event_KeyEvent_VK_AMPERSAND       : macKey = '&'; break;
            case java_awt_event_KeyEvent_VK_ASTERISK        : macKey = '*'; break;
            case java_awt_event_KeyEvent_VK_QUOTEDBL        : macKey = '\"'; break;
            case java_awt_event_KeyEvent_VK_LESS            : macKey = '<'; break;
            case java_awt_event_KeyEvent_VK_GREATER         : macKey = '>'; break;
            case java_awt_event_KeyEvent_VK_BRACELEFT       : macKey = '{'; break;
            case java_awt_event_KeyEvent_VK_BRACERIGHT      : macKey = '}'; break;
            case java_awt_event_KeyEvent_VK_AT              : macKey = '@'; break;
            case java_awt_event_KeyEvent_VK_COLON           : macKey = ':'; break;
            case java_awt_event_KeyEvent_VK_CIRCUMFLEX      : macKey = '^'; break;
            case java_awt_event_KeyEvent_VK_DOLLAR          : macKey = '$'; break;
            case java_awt_event_KeyEvent_VK_EXCLAMATION_MARK : macKey = '!'; break;
            case java_awt_event_KeyEvent_VK_LEFT_PARENTHESIS : macKey = '('; break;
            case java_awt_event_KeyEvent_VK_NUMBER_SIGN     : macKey = '#'; break;
            case java_awt_event_KeyEvent_VK_PLUS            : macKey = '+'; break;
            case java_awt_event_KeyEvent_VK_RIGHT_PARENTHESIS: macKey = ')'; break;
            case java_awt_event_KeyEvent_VK_UNDERSCORE      : macKey = '_'; break;
        }
    }
    return macKey;
}

std::tuple<NSString*, NSUInteger> awtToMacAccelerator(jchar shortcutKey, jint shortcutKeyCode, jint modifiers)
{
    NSString *theKeyEquivalent = nil;
    unichar macKey = shortcutKey;

    if (macKey == 0) {
        macKey = AWTKeyToMacShortcut(shortcutKeyCode, (modifiers & java_awt_event_KeyEvent_SHIFT_MASK) != 0);
    }

    if (macKey != 0) {
        unichar equivalent[1] = {macKey};
        theKeyEquivalent = [NSString stringWithCharacters:equivalent length:1];
    } else {
        theKeyEquivalent = @"";
    }

    NSUInteger modifierMask = 0;
    if (![theKeyEquivalent isEqualToString:@""]) {
        // Force the key equivalent to lower case if not using the shift key.
        // Otherwise AppKit will draw a Shift glyph in the menu.
        if ((modifiers & java_awt_event_KeyEvent_SHIFT_MASK) == 0) {
            theKeyEquivalent = [theKeyEquivalent lowercaseString];
        }

        // Hack for the question mark -- SHIFT and / means use the question mark.
        if ((modifiers & java_awt_event_KeyEvent_SHIFT_MASK) != 0 &&
            [theKeyEquivalent isEqualToString:@"/"])
        {
            theKeyEquivalent = @"?";
            modifiers &= ~java_awt_event_KeyEvent_SHIFT_MASK;
        }

        modifierMask = JavaModifiersToNsKeyModifiers(modifiers, NO);
    }

    return std::make_tuple(theKeyEquivalent, modifierMask);
}

NSImage* javaArrayToNSImage(JNIEnv* env, jbyteArray iconData)
{
    jsize length = env->GetArrayLength(iconData);

    // Allocate a buffer to hold the PNG data
    jbyte *buffer = env->GetByteArrayElements(iconData, NULL);

    // Create NSData from the byte array
    NSData *data = [NSData dataWithBytes:buffer length:length];

    // Release the Java byte array elements
    env->ReleaseByteArrayElements(iconData, buffer, 0);

    // Create an NSImage from the NSData
    return [[NSImage alloc] initWithData:data];
}

jint getJNIEnvironment(JNIEnv** env)
{
    jint result = jvm->GetEnv(reinterpret_cast<void **>(env), SKIKO_JNI_VERSION);

    if (result != JNI_OK) {
        result = jvm->AttachCurrentThread(reinterpret_cast<void **>(env), nullptr);
    }

    return result;
}

jmethodID getJavaMethod(JNIEnv* env, jobject javaObject, const char* methodName, const char* methodSignature)
{
    jclass localClass = env->GetObjectClass(javaObject);

    if (localClass == nullptr) {
        std::cerr << "getJavaMethod: failed to get object's class" << std::endl;
        return nullptr;
    }

    jclass cachedClass = static_cast<jclass>(env->NewGlobalRef(localClass));

    if (cachedClass == nullptr) {
        env->DeleteLocalRef(localClass);
        std::cerr << "getJavaMethod: failed to get the global pointer of the class object" << std::endl;
        return nullptr;
    }

    env->DeleteLocalRef(localClass);

    jmethodID methodId = env->GetMethodID(cachedClass, methodName, methodSignature);

    if (methodId == nullptr) {
        env->DeleteGlobalRef(cachedClass);
        std::cerr << "getJavaMethod: failed to get handleAction method ID" << std::endl;
        return nullptr;
    }

    return methodId;
}
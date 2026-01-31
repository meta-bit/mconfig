#include <jni.h>
#include <windows.h>
#include <string.h>
#include <stdlib.h>

/*
 * Helper to convert JString to wchar_t* (UTF-16)
 */
wchar_t* getWString(JNIEnv* env, jstring jStr) {
    if (jStr == NULL) return NULL;
    const jchar* chars = (*env)->GetStringChars(env, jStr, NULL);
    jsize len = (*env)->GetStringLength(env, jStr);
    wchar_t* wStr = (wchar_t*)malloc((len + 1) * sizeof(wchar_t));
    memcpy(wStr, chars, len * sizeof(wchar_t));
    wStr[len] = L'\0';
    (*env)->ReleaseStringChars(env, jStr, chars);
    return wStr;
}

JNIEXPORT jint JNICALL Java_org_metabit_platform_support_config_source_windowsregistry_jni_WindowsRegistryJNI_openKey
  (JNIEnv *env, jobject obj, jint hKey, jstring subKey, jint samDesired) {
    HKEY resultHKey;
    wchar_t* wSubKey = getWString(env, subKey);
    LONG status = RegOpenKeyExW((HKEY)(intptr_t)hKey, wSubKey, 0, (REGSAM)samDesired, &resultHKey);
    free(wSubKey);

    if (status == ERROR_SUCCESS) {
        return (jint)(intptr_t)resultHKey;
    }
    return 0;
}

JNIEXPORT void JNICALL Java_org_metabit_platform_support_config_source_windowsregistry_jni_WindowsRegistryJNI_closeKey
  (JNIEnv *env, jobject obj, jint hKey) {
    RegCloseKey((HKEY)(intptr_t)hKey);
}

JNIEXPORT jobject JNICALL Java_org_metabit_platform_support_config_source_windowsregistry_jni_WindowsRegistryJNI_enumValue
  (JNIEnv *env, jobject obj, jint hKey, jint index) {
    wchar_t valueName[16384];
    DWORD valueNameLen = 16384;
    DWORD type;
    BYTE data[65536];
    DWORD dataLen = 65536;

    LONG status = RegEnumValueW((HKEY)(intptr_t)hKey, (DWORD)index, valueName, &valueNameLen, NULL, &type, data, &dataLen);

    if (status != ERROR_SUCCESS) {
        return NULL;
    }

    jclass rvClass = (*env)->FindClass(env, "org/metabit/platform/support/config/source/windowsregistry/jni/WindowsRegistryJNI$RegistryValue");
    jmethodID constructor = (*env)->GetMethodID(env, rvClass, "<init>", "()V");
    jobject rvObj = (*env)->NewObject(env, rvClass, constructor);

    jfieldID typeField = (*env)->GetFieldID(env, rvClass, "type", "I");
    (*env)->SetIntField(env, rvObj, typeField, (jint)type);

    jfieldID nameField = (*env)->GetFieldID(env, rvClass, "name", "Ljava/lang/String;");
    jstring nameStr = (*env)->NewString(env, (const jchar*)valueName, (jsize)valueNameLen);
    (*env)->SetObjectField(env, rvObj, nameField, nameStr);

    jfieldID dataField = (*env)->GetFieldID(env, rvClass, "data", "[B");
    jbyteArray dataArray = (*env)->NewByteArray(env, (jsize)dataLen);
    (*env)->SetByteArrayRegion(env, dataArray, 0, (jsize)dataLen, (const jbyte*)data);
    (*env)->SetObjectField(env, rvObj, dataField, dataArray);

    return rvObj;
}

JNIEXPORT jstring JNICALL Java_org_metabit_platform_support_config_source_windowsregistry_jni_WindowsRegistryJNI_enumKey
  (JNIEnv *env, jobject obj, jint hKey, jint index) {
    wchar_t subKeyName[256];
    DWORD subKeyNameLen = 256;

    LONG status = RegEnumKeyExW((HKEY)(intptr_t)hKey, (DWORD)index, subKeyName, &subKeyNameLen, NULL, NULL, NULL, NULL);

    if (status != ERROR_SUCCESS) {
        return NULL;
    }

    return (*env)->NewString(env, (const jchar*)subKeyName, (jsize)subKeyNameLen);
}

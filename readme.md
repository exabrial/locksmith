# Locksmith 

A maven plugin to get credentials from the Mac OSX keychain. Connect to something like Nexus securely without storing plaintext passwords in `~/.m2/settings.xml`

## Development notes


so I don't forget how to do it

```
sdk install jextract
cd ~/opensource/locksmith/locksmith
```

```
SDK=$(xcrun --show-sdk-path)

mkdir -p /tmp/locksmith-headers
ln -sf $SDK/System/Library/Frameworks/Security.framework/Headers /tmp/locksmith-headers/Security
ln -sf $SDK/System/Library/Frameworks/CoreFoundation.framework/Headers /tmp/locksmith-headers/CoreFoundation

cat > /tmp/locksmith-headers/locksmith.h << 'EOF'
#include <CoreFoundation/CoreFoundation.h>
#include <Security/SecItem.h>
EOF

jextract --target-package com.github.exabrial.locksmith.macos \
    --header-class-name MacSecurity \
    --include-function SecItemCopyMatching \
    --include-function CFDictionaryCreate \
    --include-function CFRelease \
    --include-function CFDataGetLength \
    --include-function CFDataGetBytePtr \
    --include-function CFStringCreateWithCString \
    --include-var kSecClass \
    --include-var kSecClassGenericPassword \
    --include-var kSecAttrService \
    --include-var kSecAttrAccount \
    --include-var kSecReturnData \
    --include-var kSecMatchLimit \
    --include-var kSecMatchLimitOne \
    --include-var kCFAllocatorDefault \
    --include-var kCFBooleanTrue \
    --include-var kCFTypeDictionaryKeyCallBacks \
    --include-var kCFTypeDictionaryValueCallBacks \
    --include-typedef CFDictionaryKeyCallBacks \
    --include-typedef CFDictionaryValueCallBacks \
    --library Security \
    --library CoreFoundation \
    -I /tmp/locksmith-headers \
    -I $SDK/usr/include \
    --output locksmith/src/main/java \
    /tmp/locksmith-headers/locksmith.h
```
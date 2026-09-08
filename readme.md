# Locksmith

Read passwords from the macOS Keychain during a Maven build.... because storing your password in `~/.m2/settings.xml` is a bad idea in the age of user-hostile software and creepy weirdos reading your files silently.

## Modules

| Module | What it does |
|---|---|
| `locksmith` | Core library. Reads a generic password item from the macOS Keychain with Java FFM (Panama). |
| `locksmith-maven-plugin` | Maven plugin. Sets a Keychain password as a Maven project property during the `validate` phase. Use this when a plugin reads credentials from a property. |
| `locksmith-maven-extension` | Maven core extension. Decrypts `<server>` passwords in `settings.xml` at startup. Use this for `<distributionManagement>`, repository authentication, and wagon credentials. |

## Prerequisites

- macOS (Intel or Apple Silicon)
- Java 25+
- Maven 3.9+

## Store a password in the Keychain

```bash
security add-generic-password -U -s "nexus.superbiz.example.com" -a "your-nexus-username" -w
```

The `-w` flag with no value prompts for the password. The `-U` flag updates the item if it already exists.

To verify:

```bash
security find-generic-password -s "nexus.superbiz.example.com" -a "your-nexus-username" -w
```

## Install the Maven Extension

Maven core extensions load at startup, before `settings.xml` server passwords are resolved. You cannot add a core extension to a POM. Use one of the two methods below.

### Create `settings-security.xml`

Maven's `DefaultSecDispatcher` reads `settings-security.xml` before it dispatches to locksmith. The file must exist or the decryptor never runs. Create it if it does not exist:

```bash
if [ ! -f "$HOME/.m2/settings-security.xml" ]; then
  tee ~/.m2/settings-security.xml << 'EOF'
<settingsSecurity>
  <master>{locksmith-managed}</master>
</settingsSecurity>
EOF
fi
```

### Method 1: Global install (recommended)

This install applies to all Maven builds on the machine. It's only loaded if it's actually used.

```bash
set -e
LOCKSMITH_VERSION=1.0.0

gpg --keyserver hkps://keys.openpgp.org --recv-keys 871638A21A7F2C38066471420306A354336B4F0D

rm -rf /tmp/locksmith-stage
mkdir -p /tmp/locksmith-stage
cd /tmp/locksmith-stage

URL=https://repo1.maven.org/maven2/com/github/exabrial/locksmith/locksmith-maven-extension/${LOCKSMITH_VERSION}/locksmith-maven-extension-${LOCKSMITH_VERSION}
wget -q "${URL}.jar" "${URL}.jar.asc"

gpg --verify "locksmith-maven-extension-${LOCKSMITH_VERSION}.jar.asc"

EXT_DIR=$(dirname "$(which mvn)")/../lib/ext
mv -v "locksmith-maven-extension-${LOCKSMITH_VERSION}.jar" "${EXT_DIR}/"
```

#### Method 1: uninstallation

```
rm -rfv "$(dirname "$(which mvn)")/../lib/ext/"/locksmith*
```

### Method 2: Per-project `.mvn/extensions.xml`

Add this file to every project that needs it.

Create `.mvn/extensions.xml` in the project root:

```xml
<extensions>
    <extension>
        <groupId>com.github.exabrial.locksmith</groupId>
        <artifactId>locksmith-maven-extension</artifactId>
        <version>1.0.0</version>
    </extension>
</extensions>
```

## Usage: Maven Extension

Use the extension when Maven must authenticate to a repository or server.

Reference the Keychain item in `settings.xml`:

```xml
<servers>
    <server>
        <id>my-nexus</id>
        <username>your-nexus-username</username>
        <password>{[type=locksmith]nexus.superbiz.example.com/your-nexus-username}</password>
    </server>
</servers>
```

The format is `{[type=locksmith]serviceName/accountName}`. Maven's `DefaultSecDispatcher` finds the `locksmith` decryptor and reads the password from the Keychain.

## Usage: Maven Plugin

Use the plugin when you need a password as a Maven project property for another plugin's configuration exported as a maven property.

```xml
<plugin>
    <groupId>com.github.exabrial.locksmith</groupId>
    <artifactId>locksmith-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>read-password</goal>
            </goals>
            <configuration>
                <serviceName>nexus.superbiz.example.com</serviceName>
                <accountName>your-nexus-username</accountName>
                <passwordProperty>nexus.password</passwordProperty>
            </configuration>
        </execution>
    </executions>
</plugin>
```

After the `validate` phase, `${nexus.password}` is available to all subsequent plugins.

### Plugin Parameters

| Parameter | Required | Default | Description |
|---|---|---|---|
| `serviceName` | yes | | Keychain item name |
| `accountName` | yes | | Keychain account name |
| `passwordProperty` | yes | `password` | Maven project property to set |

## Development

### Notes to future self

...so I don't forget how to do this when Apple breaks backwards compatibility next year.

#### Installing jextract

```bash
sdk install jextract
```

#### Regenerate FFM bindings

```bash
cd ~/opensource/locksmith

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

#### Install during development:

```
cd ~/opensource/locksmith
rm -rfv "$(dirname "$(which mvn)")/../lib/ext/"/locksmith*
mvn clean install
cp -v locksmith-maven-extension/target/locksmith-maven-extension-*.jar "$(dirname "$(which mvn)")/../lib/ext/"
```

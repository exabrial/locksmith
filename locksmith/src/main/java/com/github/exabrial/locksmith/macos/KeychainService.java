package com.github.exabrial.locksmith.macos;

import static com.github.exabrial.locksmith.macos.MacSecurity.CFDataGetBytePtr;
import static com.github.exabrial.locksmith.macos.MacSecurity.CFDataGetLength;
import static com.github.exabrial.locksmith.macos.MacSecurity.CFDictionaryCreate;
import static com.github.exabrial.locksmith.macos.MacSecurity.CFRelease;
import static com.github.exabrial.locksmith.macos.MacSecurity.CFStringCreateWithCString;
import static com.github.exabrial.locksmith.macos.MacSecurity.C_CHAR;
import static com.github.exabrial.locksmith.macos.MacSecurity.C_POINTER;
import static com.github.exabrial.locksmith.macos.MacSecurity.SecItemCopyMatching;
import static com.github.exabrial.locksmith.macos.MacSecurity.kCFAllocatorDefault;
import static com.github.exabrial.locksmith.macos.MacSecurity.kCFBooleanTrue;
import static com.github.exabrial.locksmith.macos.MacSecurity.kCFTypeDictionaryKeyCallBacks;
import static com.github.exabrial.locksmith.macos.MacSecurity.kCFTypeDictionaryValueCallBacks;
import static com.github.exabrial.locksmith.macos.MacSecurity.kSecAttrAccount;
import static com.github.exabrial.locksmith.macos.MacSecurity.kSecAttrService;
import static com.github.exabrial.locksmith.macos.MacSecurity.kSecClass;
import static com.github.exabrial.locksmith.macos.MacSecurity.kSecClassGenericPassword;
import static com.github.exabrial.locksmith.macos.MacSecurity.kSecMatchLimit;
import static com.github.exabrial.locksmith.macos.MacSecurity.kSecMatchLimitOne;
import static com.github.exabrial.locksmith.macos.MacSecurity.kSecReturnData;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

public final class KeychainService {
	private static final int K_CF_STRING_ENCODING_UTF8 = 0x08000100;
	private static final int ERR_SEC_SUCCESS = 0;
	private static final int ERR_SEC_ITEM_NOT_FOUND = -25300;

	private KeychainService() {
	}

	public static String readPassword(final String serviceName, final String accountName) {
		try (final Arena arena = Arena.ofConfined()) {
			final MemorySegment cfServiceName = createCFString(arena, serviceName);
			final MemorySegment cfAccountName = createCFString(arena, accountName);
			try {
				final MemorySegment query = buildQuery(arena, cfServiceName, cfAccountName);
				try {
					final String result = executeQuery(arena, query);
					return result;
				} finally {
					CFRelease(query);
				}
			} finally {
				CFRelease(cfAccountName);
				CFRelease(cfServiceName);
			}
		}
	}

	private static MemorySegment createCFString(final Arena arena, final String value) {
		final byte[] utf8Bytes = value.getBytes(StandardCharsets.UTF_8);
		final MemorySegment cString = arena.allocate(utf8Bytes.length + 1);
		cString.copyFrom(MemorySegment.ofArray(utf8Bytes));
		cString.set(C_CHAR, utf8Bytes.length, (byte) 0);
		final MemorySegment cfString = CFStringCreateWithCString(kCFAllocatorDefault(), cString, K_CF_STRING_ENCODING_UTF8);
		if (cfString.equals(MemorySegment.NULL)) {
			throw new IllegalStateException("createCFString() CFStringCreateWithCString returned null for value:" + value);
		} else {
			return cfString;
		}
	}

	private static MemorySegment buildQuery(final Arena arena, final MemorySegment cfServiceName, final MemorySegment cfAccountName) {
		final int entryCount = 5;
		final MemorySegment keys = arena.allocate(C_POINTER, entryCount);
		final MemorySegment values = arena.allocate(C_POINTER, entryCount);
		keys.setAtIndex(C_POINTER, 0, kSecClass());
		values.setAtIndex(C_POINTER, 0, kSecClassGenericPassword());
		keys.setAtIndex(C_POINTER, 1, kSecAttrService());
		values.setAtIndex(C_POINTER, 1, cfServiceName);
		keys.setAtIndex(C_POINTER, 2, kSecAttrAccount());
		values.setAtIndex(C_POINTER, 2, cfAccountName);
		keys.setAtIndex(C_POINTER, 3, kSecMatchLimit());
		values.setAtIndex(C_POINTER, 3, kSecMatchLimitOne());
		keys.setAtIndex(C_POINTER, 4, kSecReturnData());
		values.setAtIndex(C_POINTER, 4, kCFBooleanTrue());
		final MemorySegment query = CFDictionaryCreate(kCFAllocatorDefault(), keys, values, entryCount, kCFTypeDictionaryKeyCallBacks(),
				kCFTypeDictionaryValueCallBacks());
		return query;
	}

	private static String executeQuery(final Arena arena, final MemorySegment query) {
		final MemorySegment resultPtr = arena.allocate(C_POINTER);
		final int osStatus = SecItemCopyMatching(query, resultPtr);
		final String result;
		if (osStatus == ERR_SEC_SUCCESS) {
			final MemorySegment cfData = resultPtr.get(C_POINTER, 0);
			try {
				final long length = CFDataGetLength(cfData);
				final MemorySegment bytePtr = CFDataGetBytePtr(cfData).reinterpret(length);
				result = new String(bytePtr.toArray(C_CHAR), StandardCharsets.UTF_8);
			} finally {
				CFRelease(cfData);
			}
		} else if (osStatus == ERR_SEC_ITEM_NOT_FOUND) {
			result = null;
		} else {
			throw new IllegalStateException("executeQuery() SecItemCopyMatching failed with osStatus:" + osStatus);
		}
		return result;
	}
}

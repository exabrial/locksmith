package com.github.exabrial.locksmith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.exabrial.locksmith.macos.KeychainService;
import com.github.exabrial.locksmith.socket.SocketCredentialReader;

public final class CredentialReader {
	private static final Logger log = LoggerFactory.getLogger(CredentialReader.class);

	private CredentialReader() {
	}

	public static String readPassword(final String serviceName, final String accountName) {
		log.info("readPassword() serviceName:{} accountName:{}", serviceName, accountName);
		final String osName = System.getProperty("os.name", "");
		final String result;
		if (osName.toLowerCase().contains("mac")) {
			final String keychainResult = KeychainService.readPassword(serviceName, accountName);
			if (keychainResult != null) {
				result = keychainResult;
			} else {
				log.debug("readPassword() keychain returned null, trying socket fallback");
				result = SocketCredentialReader.readPassword(serviceName, accountName);
			}
		} else {
			log.debug("readPassword() not macOS, trying socket osName:{}", osName);
			result = SocketCredentialReader.readPassword(serviceName, accountName);
		}
		if (result == null) {
			throw new IllegalStateException(
					"readPassword() no credential source available. On macOS, store the password in the Keychain."
							+ " On Linux, forward the locksmith agent socket over SSH."
							+ " See https://github.com/exabrial/locksmith for setup.");
		}
		if (!log.isTraceEnabled()) {
			log.debug("readPassword() result is present:{}", result != null);
		} else {
			log.trace("readPassword() result:{}", result);
		}
		return result;
	}
}

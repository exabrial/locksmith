package com.github.exabrial.locksmith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.exabrial.locksmith.macos.KeychainService;

public final class CredentialReader {
	private static final Logger log = LoggerFactory.getLogger(CredentialReader.class);

	private CredentialReader() {
	}

	public static String readPassword(final String serviceName, final String accountName) {
		log.info("readPassword() serviceName:{} accountName:{}", serviceName, accountName);
		final String osName = System.getProperty("os.name", "");
		final String result;
		if (osName.toLowerCase().contains("mac")) {
			result = KeychainService.readPassword(serviceName, accountName);
		} else {
			throw new UnsupportedOperationException("readPassword() unsupported operating system osName:" + osName);
		}
		if (!log.isTraceEnabled()) {
			log.debug("readPassword() result is present:{}", result != null);
		} else {
			log.trace("readPassword() result:{}", result);
		}
		return result;
	}
}

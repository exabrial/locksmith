package com.github.exabrial.locksmith.macos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.exabrial.locksmith.CredentialReader;

public class MacOsKeychainCredentialReader implements CredentialReader {
	private static final Logger log = LoggerFactory.getLogger(MacOsKeychainCredentialReader.class);

	@Override
	public String readPassword(final String serviceName, final String accountName) {
		log.debug("readPassword() serviceName:{} accountName:{}", serviceName, accountName);
		final String osName = System.getProperty("os.name", "");
		final String result;
		if (osName.toLowerCase().contains("mac")) {
			result = KeychainService.readPassword(serviceName, accountName);
		} else {
			log.trace("readPassword() skipping, not macOS osName:{}", osName);
			result = null;
		}
		log.debug("readPassword() result is present:{}", result != null);
		return result;
	}
}

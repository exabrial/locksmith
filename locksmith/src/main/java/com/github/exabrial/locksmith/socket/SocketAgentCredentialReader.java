package com.github.exabrial.locksmith.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.exabrial.locksmith.CredentialReader;

public class SocketAgentCredentialReader implements CredentialReader {
	private static final Logger log = LoggerFactory.getLogger(SocketAgentCredentialReader.class);

	@Override
	public String readPassword(final String serviceName, final String accountName) {
		log.debug("readPassword() serviceName:{} accountName:{}", serviceName, accountName);
		final String result = SocketCredentialReader.readPassword(serviceName, accountName);
		log.debug("readPassword() result is present:{}", result != null);
		return result;
	}
}

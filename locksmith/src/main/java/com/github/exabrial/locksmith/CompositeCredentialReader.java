package com.github.exabrial.locksmith;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.exabrial.locksmith.macos.MacOsKeychainCredentialReader;
import com.github.exabrial.locksmith.socket.SocketAgentCredentialReader;

public class CompositeCredentialReader implements CredentialReader {
	private static final Logger log = LoggerFactory.getLogger(CompositeCredentialReader.class);
	private static final CompositeCredentialReader INSTANCE = new CompositeCredentialReader();

	private final List<CredentialReader> readers = getReaders();

	private CompositeCredentialReader() {
	}

	protected List<CredentialReader> getReaders() {
		return List.of(new MacOsKeychainCredentialReader(), new SocketAgentCredentialReader());
	}

	public static CompositeCredentialReader getInstance() {
		return INSTANCE;
	}

	@Override
	public String readPassword(final String serviceName, final String accountName) {
		log.info("readPassword() serviceName:{} accountName:{}", serviceName, accountName);
		String result = null;
		for (final CredentialReader reader : readers) {
			result = reader.readPassword(serviceName, accountName);
			if (result != null) {
				log.debug("readPassword() resolved by reader:{}", reader.getClass().getSimpleName());
				break;
			}
		}
		if (result == null) {
			throw new IllegalStateException("readPassword() no credential source available. On macOS, store the password in the Keychain."
					+ " On Linux, forward the locksmith agent socket over SSH." + " See https://github.com/exabrial/locksmith for setup.");
		} else {
			if (!log.isTraceEnabled()) {
				log.debug("readPassword() result is present:{}", result != null);
			} else {
				log.trace("readPassword() result:{}", result);
			}
			return result;
		}
	}
}

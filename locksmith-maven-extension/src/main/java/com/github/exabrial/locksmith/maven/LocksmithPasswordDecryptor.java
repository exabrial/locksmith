package com.github.exabrial.locksmith.maven;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import com.github.exabrial.locksmith.CredentialReader;

@Singleton
@Named("locksmith")
public class LocksmithPasswordDecryptor implements PasswordDecryptor {

	@Override
	@SuppressWarnings("rawtypes")
	public String decrypt(final String str, final Map attributes, final Map config) throws SecDispatcherException {
		final int separatorIndex = str.indexOf('/');
		if (separatorIndex == -1 || separatorIndex == 0 || separatorIndex == str.length() - 1) {
			throw new SecDispatcherException("invalid locksmith password format, expected serviceName/accountName but got:" + str);
		} else {
			final String serviceName = str.substring(0, separatorIndex);
			final String accountName = str.substring(separatorIndex + 1);
			try {
				final String password = CredentialReader.readPassword(serviceName, accountName);
				if (password == null) {
					throw new SecDispatcherException("keychain item not found for serviceName:" + serviceName + " accountName:" + accountName);
				} else {
					return password;
				}
			} catch (final SecDispatcherException secDispatcherException) {
				throw secDispatcherException;
			} catch (final Exception exception) {
				throw new SecDispatcherException("failed to read keychain for serviceName:" + serviceName + " accountName:" + accountName,
						exception);
			}
		}
	}
}

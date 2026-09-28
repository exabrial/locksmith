package com.github.exabrial.locksmith.maven;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.components.secdispatcher.MasterSourceMeta;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.SecDispatcherException;
import org.codehaus.plexus.components.secdispatcher.internal.sources.PrefixMasterSourceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.exabrial.locksmith.CompositeCredentialReader;

@Singleton
@Named(LocksmithMasterSource.NAME)
public class LocksmithMasterSource extends PrefixMasterSourceSupport implements MasterSourceMeta {
	private static final Logger log = LoggerFactory.getLogger(LocksmithMasterSource.class);
	public static final String NAME = "locksmith";

	public LocksmithMasterSource() {
		super(NAME + ":");
		log.info("locksmith master source loaded");
	}

	@Override
	public String description() {
		return "macOS Keychain (serviceName/accountName)";
	}

	@Override
	public Optional<String> configTemplate() {
		return Optional.of(NAME + ":serviceName/accountName");
	}

	@Override
	protected String doHandle(final String transformed) throws SecDispatcherException {
		log.debug("doHandle() transformed:{}", transformed);
		final int separatorIndex = transformed.indexOf('/');
		if (separatorIndex == -1 || separatorIndex == 0 || separatorIndex == transformed.length() - 1) {
			throw new SecDispatcherException("invalid locksmith format, expected serviceName/accountName but got:" + transformed);
		} else {
			final String serviceName = transformed.substring(0, separatorIndex);
			final String accountName = transformed.substring(separatorIndex + 1);
			try {
				final String password = CompositeCredentialReader.getInstance().readPassword(serviceName, accountName);
				if (password == null) {
					throw new SecDispatcherException("keychain item not found for serviceName:" + serviceName + " accountName:" + accountName);
				} else {
					log.debug("doHandle() password retrieved for serviceName:{} accountName:{}", serviceName, accountName);
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

	@Override
	protected SecDispatcher.ValidationResponse doValidateConfiguration(final String transformed) {
		log.debug("doValidateConfiguration() transformed:{}", transformed);
		final int separatorIndex = transformed.indexOf('/');
		if (separatorIndex == -1 || separatorIndex == 0 || separatorIndex == transformed.length() - 1) {
			return new SecDispatcher.ValidationResponse(getClass().getSimpleName(), false,
					Map.of(SecDispatcher.ValidationResponse.Level.ERROR,
							List.of("invalid format, expected serviceName/accountName but got:" + transformed)),
					List.of());
		} else {
			final String serviceName = transformed.substring(0, separatorIndex);
			final String accountName = transformed.substring(separatorIndex + 1);
			try {
				final String password = CompositeCredentialReader.getInstance().readPassword(serviceName, accountName);
				if (password == null) {
					return new SecDispatcher.ValidationResponse(getClass().getSimpleName(), false,
							Map.of(SecDispatcher.ValidationResponse.Level.ERROR,
									List.of("keychain item not found for serviceName:" + serviceName + " accountName:" + accountName)),
							List.of());
				} else {
					return new SecDispatcher.ValidationResponse(getClass().getSimpleName(), true,
							Map.of(SecDispatcher.ValidationResponse.Level.INFO,
									List.of("keychain item found for serviceName:" + serviceName + " accountName:" + accountName)),
							List.of());
				}
			} catch (final Exception exception) {
				return new SecDispatcher.ValidationResponse(getClass().getSimpleName(), false,
						Map.of(SecDispatcher.ValidationResponse.Level.ERROR, List.of("failed to read keychain:" + exception.getMessage())),
						List.of());
			}
		}
	}
}

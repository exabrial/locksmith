package com.github.exabrial.locksmith.socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SocketCredentialReader {
	private static final Logger log = LoggerFactory.getLogger(SocketCredentialReader.class);

	private SocketCredentialReader() {
	}

	public static String readPassword(final String serviceName, final String accountName) {
		final Path socketPath = resolveSocketPath();
		if (socketPath == null) {
			return null;
		} else {
			log.debug("readPassword() using socket socketPath:{}", socketPath);
			try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
				channel.connect(UnixDomainSocketAddress.of(socketPath));
				try (
						PrintWriter writer = new PrintWriter(new OutputStreamWriter(Channels.newOutputStream(channel), StandardCharsets.UTF_8),
								true);
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(Channels.newInputStream(channel), StandardCharsets.UTF_8))) {
					writer.println(serviceName + "/" + accountName);
					final String password = reader.readLine();
					return password;
				}
			} catch (final Exception exception) {
				log.warn("readPassword() socket read failed socketPath:{}", socketPath, exception);
				return null;
			}
		}
	}

	static Path resolveSocketPath() {
		final String locksmithSock = System.getenv("LOCKSMITH_SOCK");
		final Path result;
		if (locksmithSock != null && !locksmithSock.isBlank()) {
			final Path envPath = Path.of(locksmithSock);
			if (Files.exists(envPath)) {
				result = envPath;
			} else {
				log.debug("resolveSocketPath() LOCKSMITH_SOCK set but does not exist locksmithSock:{}", locksmithSock);
				result = null;
			}
		} else {
			final String xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");
			if (xdgRuntimeDir != null && !xdgRuntimeDir.isBlank()) {
				final Path xdgPath = Path.of(xdgRuntimeDir, "locksmith.sock");
				if (Files.exists(xdgPath)) {
					result = xdgPath;
				} else {
					result = tryHomePath();
				}
			} else {
				result = tryHomePath();
			}
		}
		return result;
	}

	private static Path tryHomePath() {
		final Path homePath = Path.of(System.getProperty("user.home"), ".locksmith", "locksmith.sock");
		final Path result;
		if (Files.exists(homePath)) {
			result = homePath;
		} else {
			result = null;
		}
		return result;
	}
}

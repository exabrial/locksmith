package com.github.exabrial.locksmith.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.github.exabrial.locksmith.CredentialReader;

@Mojo(name = "read-password", defaultPhase = LifecyclePhase.VALIDATE)
public class KeychainPasswordMojo extends AbstractMojo {

	@Parameter(required = true)
	private String serviceName;

	@Parameter(required = true)
	private String accountName;

	@Parameter(required = true, defaultValue = "password")
	private String passwordProperty;

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("reading keychain item serviceName:" + serviceName + " accountName:" + accountName);
		try {
			final String password = CredentialReader.readPassword(serviceName, accountName);
			if (password == null) {
				throw new MojoExecutionException("keychain item not found for serviceName:" + serviceName + " accountName:" + accountName);
			} else {
				project.getProperties().setProperty(passwordProperty, password);
				getLog().info("set project property passwordProperty:" + passwordProperty);
			}
		} catch (final MojoExecutionException mojoExecutionException) {
			throw mojoExecutionException;
		} catch (final Exception exception) {
			throw new MojoExecutionException("failed to read keychain item serviceName:" + serviceName + " accountName:" + accountName,
					exception);
		}
	}
}

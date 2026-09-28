package com.github.exabrial.locksmith;

public interface CredentialReader {
	String readPassword(String serviceName, String accountName);
}

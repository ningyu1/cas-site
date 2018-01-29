package org.apereo.cas.adaptors.jdbc;

public interface ForceLogoutManager {

	public void doLogout(final String username);
}

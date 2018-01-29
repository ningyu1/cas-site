package org.apereo.cas.adaptors.jdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("cas")
public class ForceLogoutProerties {

	@NestedConfigurationProperty
	private boolean forceLogout;

	public boolean isForceLogout() {
		return forceLogout;
	}

	public void setForceLogout(boolean forceLogout) {
		this.forceLogout = forceLogout;
	}
	
}

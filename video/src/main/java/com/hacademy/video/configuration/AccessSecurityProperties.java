package com.hacademy.video.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "custom.security")
public class AccessSecurityProperties {
	private String adminPassword;
	private long viewerSessionHours = 12;
	private long adminSessionHours = 2;
	private boolean cookieSecure = true;

	public String getAdminPassword() {
		return adminPassword;
	}
	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}
	public long getViewerSessionHours() {
		return viewerSessionHours;
	}
	public void setViewerSessionHours(long viewerSessionHours) {
		this.viewerSessionHours = viewerSessionHours;
	}
	public long getAdminSessionHours() {
		return adminSessionHours;
	}
	public void setAdminSessionHours(long adminSessionHours) {
		this.adminSessionHours = adminSessionHours;
	}
	public boolean isCookieSecure() {
		return cookieSecure;
	}
	public void setCookieSecure(boolean cookieSecure) {
		this.cookieSecure = cookieSecure;
	}
}

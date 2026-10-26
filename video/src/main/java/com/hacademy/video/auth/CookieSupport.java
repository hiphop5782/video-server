package com.hacademy.video.auth;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.http.ResponseCookie;

import com.hacademy.video.configuration.AccessSecurityProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public final class CookieSupport {
	private CookieSupport() {
	}

	public static String value(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if(cookies == null) {
			return null;
		}
		return Arrays.stream(cookies)
				.filter(cookie -> name.equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElse(null);
	}

	public static ResponseCookie sessionCookie(
			String name,
			String value,
			Duration maxAge,
			AccessSecurityProperties properties) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(properties.isCookieSecure())
				.sameSite("Lax")
				.path("/")
				.maxAge(maxAge)
				.build();
	}

	public static ResponseCookie expiredCookie(String name, AccessSecurityProperties properties) {
		return sessionCookie(name, "", Duration.ZERO, properties);
	}
}

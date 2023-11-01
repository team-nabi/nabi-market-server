package org.prgrms.nabimarketbe.oauth2.google.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
	GUEST("ROLE_GUEST"),
	USER("ROLE_USER");

	private final String key;
}

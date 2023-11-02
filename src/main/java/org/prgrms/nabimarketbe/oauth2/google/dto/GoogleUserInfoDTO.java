package org.prgrms.nabimarketbe.oauth2.google.dto;

import org.prgrms.nabimarketbe.domain.user.Role;
import org.prgrms.nabimarketbe.domain.user.entity.User;

public record GoogleUserInfoDTO(
	String id,
	String email,
	String picture
) {
	private static final String PROVIDER = "GOOGLE";

	public User toEntity(String nickName) {
		return User.builder()
			.nickname(nickName)
			.email(email)
			.imageUrl(picture)
			.provider(PROVIDER)
			.accountId(id)
			.role(Role.USER)
			.build();
	}
}
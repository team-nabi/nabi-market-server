package org.prgrms.nabimarketbe.domain.user.dto;

import org.prgrms.nabimarketbe.domain.user.entity.User;
import org.prgrms.nabimarketbe.global.security.jwt.dto.TokenResponseDTO;

public record UserLoginResponseDTO(
	UserResponseDto user,
	TokenResponseDTO token
) {

	public static UserLoginResponseDTO from(User user, TokenResponseDTO tokenResponseDTO) {
		UserResponseDto userResponseDto = UserResponseDto.from(user);

		UserLoginResponseDTO response = new UserLoginResponseDTO(userResponseDto, tokenResponseDTO);

		return response;
	}
}
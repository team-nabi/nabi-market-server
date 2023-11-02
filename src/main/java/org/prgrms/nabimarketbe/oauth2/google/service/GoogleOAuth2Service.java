package org.prgrms.nabimarketbe.oauth2.google.service;

import org.prgrms.nabimarketbe.domain.user.repository.UserRepository;
import org.prgrms.nabimarketbe.domain.user.service.SignService;
import org.prgrms.nabimarketbe.global.security.jwt.provider.JwtProvider;
import org.prgrms.nabimarketbe.oauth2.google.domain.OAuth2;
import org.prgrms.nabimarketbe.oauth2.google.dto.GoogleOAuth2TokenDTO;
import org.prgrms.nabimarketbe.oauth2.google.dto.GoogleUserInfoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2Service {
	private final OAuth2 oAuth2;

	private final JwtProvider jwtProvider;

	private final UserRepository userRepository;

	private final SignService signService;

	public String requestRedirectUrl() {
		String redirectUrl = oAuth2.getOAuth2RedirectUrl();

		return redirectUrl;
	}

	public GoogleUserInfoDTO OAuth2Login(String code) throws JsonProcessingException {
		GoogleUserInfoDTO googleUserInfoDTO = getUserInfoFromPlatform(code);

		return googleUserInfoDTO;
	}

	private GoogleUserInfoDTO getUserInfoFromPlatform(String code) throws JsonProcessingException {
		ResponseEntity<String> accessToken = oAuth2.requestAccessToken(code);
		GoogleOAuth2TokenDTO googleOAuth2TokenDTO = oAuth2.getAccessToken(accessToken);

		ResponseEntity<String> userInfo = oAuth2.requestUserInfo(googleOAuth2TokenDTO);

		return oAuth2.parseUserInfo(userInfo);
	}
}
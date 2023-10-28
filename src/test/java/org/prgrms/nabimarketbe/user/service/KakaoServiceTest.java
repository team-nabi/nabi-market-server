package org.prgrms.nabimarketbe.user.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.prgrms.nabimarketbe.domain.user.service.KakaoService;
import org.prgrms.nabimarketbe.domain.security.oauth.dto.social.KakaoProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@WithMockUser(username = "mockUser")
public class KakaoServiceTest {

    @Autowired
    private KakaoService kakaoService;

    @Autowired
    Environment env;

    private static String accessToken;

    @Before
    public void setToken() {
        accessToken = env.getProperty("social.kakao.accessToken");
    }

    @Test
    public void 액세스토큰으로_사용자정보_요청() throws Exception
    {
        //given
        //when
        KakaoProfile kakaoProfile = kakaoService.getKakaoProfile(accessToken);

        //then
        assertThat(kakaoProfile).isNotNull();
        assertThat(kakaoProfile.getProperties().getNickname()).isEqualTo("신예진");
    }
}
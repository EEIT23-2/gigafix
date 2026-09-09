package com.gigafix.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gigafix.member.dto.RegisterAndLoginResult;
import com.gigafix.member.entity.Member;
import com.gigafix.member.entity.Member.Gender;
import com.gigafix.member.exception.InvalidCredentialsException;
import com.gigafix.member.repository.MemberRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional
@Service
public class GoogleAuthService {
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberUserDetailsService memberUserDetailsService;

    public RegisterAndLoginResult googleLogin(String idTokenString) throws Exception {
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(idTokenString);
        } catch (IllegalArgumentException e) {// 如果傳進的字串為一個不是JWT的假字串，會拋出這個例外，然後回傳給前端錯的格式，所以要catch
            throw new InvalidCredentialsException();// 回傳自訂格式
        }

        // 認證失敗的情況→簽章不對/過期/audience不符
        // 認證失敗的話.verify()的回傳值會是null，那麼就拋出例外給前端
        if (idToken == null) {
            throw new InvalidCredentialsException();
        }

        // 拿到payload裡的email，以及檢查有沒有註冊過這個mail
        Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        // getEmailVerified() 回傳的是 Boolean 物件，怕回傳是null，所以用Wrapper class的Boolean.TRUE做比較
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new InvalidCredentialsException();
        }
        // Google帳號的名稱，用來填realName/nickName
        String name = (String) payload.get("name");

        Member member = memberRepository.findByEmail(email)// 查不到就幫該使用者建立資料庫
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email(email)
                        .realName(name)
                        .nickName(name)
                        .phone("0900000000")
                        .address("尚未填寫")
                        .gender(Gender.MALE)
                        .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))// 如果使用第三方登入的話，替使用者隨機生成一個假密碼佔位，但這個密碼誰也不知道是啥(除非使用者用忘記密碼)
                        .createTime(java.time.LocalDateTime.now())
                        .build()));

        // 第5步：不管是查到的還是剛建立的member，都用同一套發JWT+包cookie的邏輯收尾
        return memberUserDetailsService.login(member);
    }

}

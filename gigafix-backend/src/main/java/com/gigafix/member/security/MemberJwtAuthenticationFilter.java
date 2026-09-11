package com.gigafix.member.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gigafix.common.util.JwtUtils;
import com.gigafix.member.service.MemberUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// 因為之後要把這個filter手動註冊到Srcurity Configration，宣告其為Component會重複註冊
// 如果ApplicationContext裡出現任何Filter型別的bean會被Spring Boot註冊成Bean。重複註冊會造成每次請求都默默跑兩次
@RequiredArgsConstructor
public class MemberJwtAuthenticationFilter extends OncePerRequestFilter {
    // 因為這個類別不是Bean，沒辦法享有spring 的Autowired，要在呼叫時塞這兩個Bean給這個物件
    private final JwtUtils jwtUtils;
    private final MemberUserDetailsService memberUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 這個filter是用來取代在spring boot預設filter chain的BasicAuthenticationFilter

        // 先抓header的jwt
        Cookie[] cookies = request.getCookies();
        String token = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break; // 加這個，找到就跳出，不用整個陣列跑完
                }
            }
        }

        // 如果有JWT就要做jwt解析判斷是否為合格的簽章
        if (token != null && jwtUtils.validateToken(token)) {
            // security config中的Filter Chain中.authenticated()只看SecurityContextHolder 裡的
            // Authentication有沒有東西，如果jwt驗證成功才會在SecurityContextHolder放入認證(也就是才能對需要認證的path做請求)
            Long memberId = jwtUtils.extractMemberId(token);
            // 把 memberId 轉成完整的 MemberUserDetails
            // 如果是打在 permitAll() 的公開端點、沒帶 token，authentication.getPrincipal() 會是 Spring
            // Security 自動塞的匿名字串 "anonymousUser"，硬轉型會直接丟
            // ClassCastException；authorizeHttpRequests 已經先把沒登入的 request篩選掉所以才可以這樣用
            MemberUserDetails memberDetails = (MemberUserDetails) memberUserDetailsService.loadUserById(memberId);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(memberDetails,
                    null, memberDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // authentication放到SecurityContextHolder，後面的filter就可以藉此判斷這是一個已經認證合格的請求

            // 因為JWT要設計成滑動過期，所以每次有API請求，且是有使用者是有登入的話，就塞JWT給他
            ResponseCookie newJwtCookie = jwtUtils.createTokenCookie(memberId);
            response.addHeader(HttpHeaders.SET_COOKIE, newJwtCookie.toString());// 用setHeader會把整個header覆蓋掉，用addHeader會保留之前的Header內容並加上現在的這個，比較保險
        }
        // 如果沒有JWT可能是還沒登入或是註冊，所以就算jwt解析沒過也不會拋出例外

        filterChain.doFilter(request, response);
    }

}

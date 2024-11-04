package com.poppy.common.auth.filter;

import com.poppy.common.auth.JwtTokenizer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenizer jwtTokenizer;

    // 검증 필터
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            Claims claims = verifyToken(request);
            getAuthentication(claims);

            filterChain.doFilter(request, response);
        }
        catch (ExpiredJwtException e) { // 토큰이 만료된 경우
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"토큰이 만료되었습니다.\"}");
        }
        catch (MalformedJwtException e) {   // 토큰이 유효하지 않은 경우
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"유효하지 않은 토큰입니다.\"}");
        }
        catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"내부 서버 오류가 발생했습니다.\"}");
        }
    }

    // 검증하지 않아도 되는 경우
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Authorization 헤더 체크
        String authorization = request.getHeader("Authorization");
        return authorization == null || !authorization.startsWith("Bearer");
    }

    // 토큰 검증
    private Claims verifyToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null; // 토큰이 없는 경우 검증하지 않음
        }

        String token = authorizationHeader.replace("Bearer ", "");
        return jwtTokenizer.parseAccessToken(token);
    }

    // 권한 부여
    private void getAuthentication(Claims claims) {
        long userId = claims.get("userId", Long.class); // claims에서 userId 찾기

        // JwtTokenizer로부터 authorities 가져오기
        List<GrantedAuthority> authorities = jwtTokenizer.getAuthoritiesFromClaims(claims);

        // Authentication 객체 생성 및 SecurityContext에 권한, 유저 객체 정보 저장
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);   // SecurityContextHolder에 인증 설정
    }
}

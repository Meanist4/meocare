package com.meocare.meocare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình Security cho giai đoạn phát triển/test API:
 * - Stateless (không tạo session), tắt CSRF, tắt form login và HTTP Basic.
 * - Cho phép toàn bộ request (permitAll) để không bị 401/403 khi test.
 *
 * Khi triển khai JWT: thêm JwtAuthenticationFilter bằng http.addFilterBefore(...),
 * mở /api/auth/** và đổi anyRequest().permitAll() thành anyRequest().authenticated(),
 * sau đó phân quyền theo role (hasRole("ADMIN"), hasRole("VETERINARIAN"), ...).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

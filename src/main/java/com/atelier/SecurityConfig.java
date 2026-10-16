package com.atelier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Locale;

@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
 @Bean UserDetailsService users(JdbcTemplate jdbc) {
  return supplied -> jdbc.query("SELECT email,password_hash FROM app_user WHERE email=?",
   (rs,n) -> User.withUsername(rs.getString(1)).password(rs.getString(2)).roles("USER").build(),
   supplied.trim().toLowerCase(Locale.ROOT)).stream().findFirst()
   .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  http.authorizeHttpRequests(auth -> auth
   .requestMatchers("/", "/index.html", "/style.css", "/app.js", "/assets/**", "/api/auth/csrf", "/api/auth/register", "/health").permitAll()
   .anyRequest().authenticated());
  http.formLogin(login -> login.loginProcessingUrl("/api/auth/login").usernameParameter("email")
   .successHandler((req,res,auth) -> res.setStatus(204))
   .failureHandler((req,res,e) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Email or password is incorrect.\"}"); }).permitAll());
  http.logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
   .logoutSuccessHandler((req,res,auth) -> res.setStatus(204)));
  http.exceptionHandling(errors -> errors
   .authenticationEntryPoint((req,res,e) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Please sign in to open your wardrobe.\"}"); })
   .accessDeniedHandler((req,res,e) -> { res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Your security token expired. Please try again.\"}"); }));
  return http.build();
 }
}

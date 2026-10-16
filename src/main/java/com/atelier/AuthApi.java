package com.atelier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;

@RestController
public class AuthApi {
 private final JdbcTemplate jdbc;
 private final PasswordEncoder encoder;
 private final ObjectMapper mapper;
 public AuthApi(JdbcTemplate jdbc, PasswordEncoder encoder, ObjectMapper mapper) {
  this.jdbc=jdbc; this.encoder=encoder; this.mapper=mapper;
 }
 public record Credentials(String email, String password) {}
 @GetMapping("/api/auth/csrf") public Map<String,String> csrf(CsrfToken token) {
  return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
 }
 @GetMapping("/api/auth/me") public Map<String,String> me(Principal user) { return Map.of("email",user.getName()); }
 @GetMapping("/health") public Map<String,String> health() { jdbc.queryForObject("SELECT 1",Integer.class); return Map.of("status","ok"); }
 @PostMapping("/api/auth/register") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public Map<String,String> register(@RequestBody Credentials credentials) throws Exception {
  String email=credentials.email()==null?"":credentials.email().trim().toLowerCase(Locale.ROOT);
  String password=credentials.password();
  if(email.length()>254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid email address.");
  if(password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a password of at least 12 characters and at most 72 UTF-8 bytes.");
  try { jdbc.update("INSERT INTO app_user(email,password_hash) VALUES (?,?)",email,encoder.encode(password)); }
  catch(DuplicateKeyException e) { throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists."); }
  jdbc.update("INSERT INTO user_closet(owner_email,snapshot) VALUES (?,?)",email,mapper.writeValueAsString(Closet.seed()));
  return Map.of("email",email);
 }
 @ExceptionHandler(ResponseStatusException.class)
 public org.springframework.http.ResponseEntity<Map<String,String>> invalid(ResponseStatusException e) {
  return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"Request failed.")));
 }
}

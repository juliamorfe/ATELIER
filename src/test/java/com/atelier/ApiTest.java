package com.atelier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ApiTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 @Autowired JdbcTemplate jdbc;
 String password="a-long-test-password";
 String email(){return UUID.randomUUID()+"@example.com";}
 void register(String email)throws Exception{
  mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
   .content(mapper.writeValueAsString(Map.of("email",email,"password",password))))
   .andExpect(status().isCreated());
 }
 MockHttpSession login(String email)throws Exception{
  return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).param("email",email).param("password",password))
   .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
 }
 @Test void publicPageAndProtectedData()throws Exception{
  mvc.perform(get("/")).andExpect(status().isOk());
  mvc.perform(get("/assets/shirt.svg")).andExpect(status().isOk());
  mvc.perform(get("/health")).andExpect(status().isOk());
  mvc.perform(get("/api/state")).andExpect(status().isUnauthorized());
  mvc.perform(get("/uploads/anything.png")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{}"))
   .andExpect(status().isForbidden());
 }
 @Test void accountValidationHashingLoginLogoutAndCsrf()throws Exception{
  String email=email();register(email);
  String hash=jdbc.queryForObject("SELECT password_hash FROM app_user WHERE email=?",String.class,email);
  assertNotEquals(password,hash);assertTrue(hash.startsWith("$2a$"));
  mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
   .content(mapper.writeValueAsString(Map.of("email",email.toUpperCase(),"password",password))))
   .andExpect(status().isConflict());
  mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
   .content("{\"email\":\"invalid\",\"password\":\"short\"}"))
   .andExpect(status().isBadRequest());
  mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
   .content(mapper.writeValueAsString(Map.of("email",email(),"password","short"))))
   .andExpect(status().isBadRequest());
  mvc.perform(post("/api/auth/login").with(csrf()).param("email",email).param("password","wrong"))
   .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").exists());
  var session=login(email.toUpperCase());
  mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.email").value(email));
  mvc.perform(delete("/api/items/sample-0").session(session)).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
  assertTrue(session.isInvalid());
  mvc.perform(get("/api/state")).andExpect(status().isUnauthorized());
 }
 @Test void usersCannotReadChangeOrReferenceEachOthersData()throws Exception{
  String alice=email(),bob=email();register(alice);register(bob);
  var a=login(alice);var b=login(bob);
  var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB),"png",output);
  String itemJson=mvc.perform(multipart("/api/items").file(new MockMultipartFile("image","photo.png","image/png",output.toByteArray()))
   .session(a).with(csrf()).param("name","Alice shirt").param("category","tops").param("season","Resort").param("occasion","Travel"))
   .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  var item=mapper.readTree(itemJson);String id=item.get("id").asText(),image=item.get("image").asText();
  mvc.perform(get(image).session(a)).andExpect(status().isOk()).andExpect(content().bytes(output.toByteArray()));
  mvc.perform(get(image).session(b)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/items/"+id).session(b).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(post("/api/looks").session(b).with(csrf()).contentType("application/json")
   .content(mapper.writeValueAsString(Map.of("name","Stolen","season","Resort","occasion","Travel","items",List.of(id)))))
   .andExpect(status().isBadRequest());
  String lookJson=mvc.perform(post("/api/looks").session(a).with(csrf()).contentType("application/json")
   .content(mapper.writeValueAsString(Map.of("name","Private","season","Resort","occasion","Travel","items",List.of(id)))))
   .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  String lookId=mapper.readTree(lookJson).get("id").asText();
  mvc.perform(delete("/api/looks/"+lookId).session(b).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(delete("/api/items/sample-0").session(a).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/state").session(b)).andExpect(jsonPath("$.items.length()").value(12)).andExpect(jsonPath("$.looks.length()").value(1));
  mvc.perform(get("/api/state").session(login(alice))).andExpect(jsonPath("$.items.length()").value(12)).andExpect(jsonPath("$.looks.length()").value(2));
  mvc.perform(delete("/api/items/"+id).session(a).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(image).session(a)).andExpect(status().isNotFound());
  mvc.perform(get("/api/state").session(a)).andExpect(jsonPath("$.looks.length()").value(1));
 }
 @Test void csrfEndpointWorksWithActualSessionToken()throws Exception{
  String email=email();register(email);
  var tokenResult=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
  var token=mapper.readTree(tokenResult.getResponse().getContentAsString());
  var session=(MockHttpSession)tokenResult.getRequest().getSession();String oldId=session.getId();
  mvc.perform(post("/api/auth/login").session(session).header(token.get("headerName").asText(),token.get("token").asText())
   .param("email",email).param("password",password)).andExpect(status().isNoContent());
  assertNotEquals(oldId,session.getId());
  mvc.perform(delete("/api/items/sample-0").session(session).header(token.get("headerName").asText(),token.get("token").asText()))
   .andExpect(status().isForbidden());
  var fresh=mapper.readTree(mvc.perform(get("/api/auth/csrf").session(session)).andReturn().getResponse().getContentAsString());
  mvc.perform(delete("/api/items/sample-0").session(session).header(fresh.get("headerName").asText(),fresh.get("token").asText()))
   .andExpect(status().isNoContent());
 }
}

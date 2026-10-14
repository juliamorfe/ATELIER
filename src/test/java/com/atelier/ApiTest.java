package com.atelier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Files;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class ApiTest {
 @DynamicPropertySource static void config(DynamicPropertyRegistry registry)throws Exception{String path=Files.createTempDirectory("atelier-api-test-").toString();registry.add("atelier.data",()->path);}
 @Autowired MockMvc mvc;
 @Test void servesInterfaceAndJavaData()throws Exception{mvc.perform(get("/")).andExpect(status().isOk());mvc.perform(get("/style.css")).andExpect(status().isOk());mvc.perform(get("/assets/shirt.svg")).andExpect(status().isOk());mvc.perform(get("/api/state")).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(12));mvc.perform(get("/api/items").param("category","tops")).andExpect(jsonPath("$.length()").value(2));mvc.perform(get("/api/shuffle").param("occasion","Formal")).andExpect(jsonPath("$.length()").value(0));}
 @Test void rejectsInvalidRequestsWithReadableErrors()throws Exception{mvc.perform(post("/api/looks").contentType("application/json").content("{\"name\":\"x\",\"season\":\"All season\",\"occasion\":\"Everyday\",\"items\":[]}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());mvc.perform(multipart("/api/items").file(new MockMultipartFile("image","fake.png","image/png","fake".getBytes())).param("name","Fake").param("category","tops").param("season","All season").param("occasion","Everyday")).andExpect(status().isBadRequest());mvc.perform(get("/uploads/not-a-file.png")).andExpect(status().isNotFound());mvc.perform(delete("/api/items/missing")).andExpect(status().isNotFound());}
}

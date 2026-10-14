package com.atelier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class ClosetTest {
 @TempDir Path directory;
 Closet closet;
 @BeforeEach void setup()throws Exception{closet=new Closet(directory.toString(),new ObjectMapper());}
 @Test void seedAndDeletionPersistAcrossRestart()throws Exception{
  assertEquals(12,closet.state().items().size());closet.deleteItem("sample-0");Closet restarted=new Closet(directory.toString(),new ObjectMapper());assertEquals(11,restarted.state().items().size());assertFalse(restarted.state().looks().get(0).items().contains("sample-0"));
 }
 @Test void filterAndShuffleRespectCategoriesAndDressRules(){
  assertEquals(2,closet.filter("tops","all","all").size());assertEquals(2,closet.filter("all","all","Workwear").size());assertTrue(closet.shuffle("all","Formal").isEmpty());
  for(int n=0;n<200;n++){var look=closet.shuffle("all","all");Set<String> slots=new HashSet<>();for(var i:look)assertTrue(slots.add(Closet.slot(i.category())));assertFalse(look.stream().anyMatch(i->i.category().equals("dresses"))&&look.stream().anyMatch(i->i.category().equals("bottoms")));}
 }
 @Test void saveValidatesAndDeletionCleansEmptyLooks()throws Exception{
  assertThrows(ResponseStatusException.class,()->closet.save(new Closet.SaveLook("","All season","Everyday",List.of("sample-0"))));
  assertThrows(ResponseStatusException.class,()->closet.save(new Closet.SaveLook("Bad","All season","Everyday",List.of("sample-9","sample-2"))));
  assertThrows(ResponseStatusException.class,()->closet.save(new Closet.SaveLook("Bad","All season","Everyday",List.of("sample-0","sample-1"))));
  assertThrows(ResponseStatusException.class,()->closet.save(new Closet.SaveLook("Bad","All season","Everyday",List.of("missing"))));
  var look=closet.save(new Closet.SaveLook("Only knit","All season","Everyday",List.of("sample-1")));closet.deleteItem("sample-1");assertFalse(closet.state().looks().stream().anyMatch(l->l.id().equals(look.id())));
 }
 @Test void realImageUploadPersistsAndInvalidImagesAreRejected()throws Exception{
  var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB),"png",output);
  var item=closet.upload("My shirt","tops","Resort","Travel",new MockMultipartFile("image","../../photo.png","image/png",output.toByteArray()));
  assertTrue(java.nio.file.Files.exists(closet.uploads().resolve(Path.of(item.image()).getFileName())));assertEquals(13,new Closet(directory.toString(),new ObjectMapper()).state().items().size());
  closet.deleteItem(item.id());assertFalse(java.nio.file.Files.exists(closet.uploads().resolve(Path.of(item.image()).getFileName())));
  assertThrows(ResponseStatusException.class,()->closet.upload("Fake","tops","All season","Everyday",new MockMultipartFile("image","fake.png","image/png","not an image".getBytes())));
  assertThrows(ResponseStatusException.class,()->closet.upload("Fake","unknown","All season","Everyday",new MockMultipartFile("image",output.toByteArray())));
 }
 @Test void looksCanBeSavedDeletedAndReloaded()throws Exception{var look=closet.save(new Closet.SaveLook("Office","Autumn/Winter","Workwear",List.of("sample-0","sample-3","sample-5")));assertEquals(2,new Closet(directory.toString(),new ObjectMapper()).state().looks().size());closet.deleteLook(look.id());assertEquals(1,closet.state().looks().size());}
}

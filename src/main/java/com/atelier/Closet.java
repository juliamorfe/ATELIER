package com.atelier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.io.*;
import java.time.Instant;
import java.util.*;

@Service
public class Closet {
 public record Item(String id,String name,String category,String season,String occasion,String image) {}
 public record Look(String id,String name,String season,String occasion,List<String> items,String created) {}
 public record State(List<Item> items,List<Look> looks) {}
 public record SaveLook(String name,String season,String occasion,List<String> items) {}
 public static final List<String> CATEGORIES=List.of("tops","bottoms","dresses","outerwear","shoes","bags","accessories","hats");
 public static final List<String> SEASONS=List.of("All season","Spring/Summer","Autumn/Winter","Resort");
 public static final List<String> OCCASIONS=List.of("Everyday","Workwear","Evening","Formal","Travel");
 private final Path root;
 private final ObjectMapper mapper;
 private State state;
 public Closet(@Value("${atelier.data}") String directory,ObjectMapper mapper) throws IOException {
  this.root=Path.of(directory).toAbsolutePath().normalize(); this.mapper=mapper;
  Files.createDirectories(root.resolve("uploads"));
  if(Files.exists(root.resolve("closet.json"))) state=mapper.readValue(root.resolve("closet.json").toFile(),State.class);
  else {state=seed(); persist(state);}
 }
 private State seed() {
  List<Item> items=new ArrayList<>();
  String[][] samples={{"Cloud cotton shirt","tops","shirt"},{"Ink ribbed knit","tops","knit"},{"Sand wide-leg trousers","bottoms","trousers"},{"Midnight tailored trousers","bottoms","pants"},{"Stone workwear jacket","outerwear","jacket"},{"Espresso leather loafers","shoes","loafers"},{"Ivory court sneakers","shoes","sneakers"},{"Cognac everyday tote","bags","tote"},{"Gold sculptural hoops","accessories","hoops"},{"Oat linen dress","dresses","dress"},{"Sage wool beret","hats","beret"},{"Cream pleated skirt","bottoms","skirt"}};
  for(int i=0;i<samples.length;i++) {String[] s=samples[i];items.add(new Item("sample-"+i,s[0],s[1],"All season",i==3||i==5?"Workwear":"Everyday","/assets/"+s[2]+".svg"));}
  return new State(items,new ArrayList<>(List.of(new Look("first-look","A quiet morning","All season","Everyday",List.of("sample-0","sample-2","sample-6","sample-7"),Instant.now().toString()))));
 }
 public static String slot(String category) {
  // SWITCH: maps wardrobe categories to meaningful outfit positions.
  return switch(category) {case "tops","dresses" -> "base"; case "bottoms" -> "bottom"; case "outerwear" -> "layer";case "shoes" -> "footwear";case "bags" -> "bag";case "hats" -> "headwear";case "accessories" -> "detail";default -> throw bad("Choose a valid category.");};
 }
 private static ResponseStatusException bad(String message) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 private static String name(String value) {if(value==null||value.trim().isEmpty()||value.trim().length()>80) throw bad("Name must contain 1–80 characters.");return value.trim();}
 private static void metadata(String season,String occasion) {if(!SEASONS.contains(season)||!OCCASIONS.contains(occasion)) throw bad("Choose a valid season and occasion.");}
 private void persist(State next) throws IOException {
  Path temp=root.resolve("closet.tmp");mapper.writeValue(temp.toFile(),next);
  try {Files.move(temp,root.resolve("closet.json"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,root.resolve("closet.json"),StandardCopyOption.REPLACE_EXISTING);}
  state=next;
 }
 public synchronized State state(){return new State(List.copyOf(state.items()),List.copyOf(state.looks()));}
 public synchronized List<Item> filter(String category,String season,String occasion) {
  List<Item> result=new ArrayList<>();
  // LOOP + CONDITIONAL: apply all selected wardrobe filters.
  for(Item item:state.items()) if((category.equals("all")||item.category().equals(category))&&matches(item,season,occasion)) result.add(item);
  return result;
 }
 private boolean matches(Item item,String season,String occasion){return (season.equals("all")||item.season().equals("All season")||item.season().equals(season))&&(occasion.equals("all")||item.occasion().equals(occasion));}
 public synchronized List<Item> shuffle(String season,String occasion) {
  Map<String,List<Item>> pools=new LinkedHashMap<>();
  for(Item item:filter("all",season,occasion)) pools.computeIfAbsent(slot(item.category()),k->new ArrayList<>()).add(item);
  List<Item> result=new ArrayList<>();boolean dress=false;
  for(String position:List.of("base","bottom","layer","footwear","bag","detail","headwear")) {
   // CONDITIONAL: a dress replaces top + bottom; absent categories are skipped.
   if(position.equals("bottom")&&dress) continue;
   List<Item> pool=pools.get(position);if(pool==null||pool.isEmpty()) continue;
   Item picked=pool.get(new Random().nextInt(pool.size()));result.add(picked);if(picked.category().equals("dresses")) dress=true;
  }
  return result;
 }
 public synchronized Item upload(String title,String category,String season,String occasion,MultipartFile file) throws IOException {
  title=name(title);slot(category);metadata(season,occasion);
  if(file.isEmpty()||file.getSize()>8*1024*1024) throw bad("Choose a PNG or JPEG image up to 8 MB.");
  byte[] bytes=file.getBytes();boolean png=bytes.length>8&&bytes[0]==(byte)137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71;boolean jpg=bytes.length>3&&bytes[0]==(byte)255&&bytes[1]==(byte)216;
  if(!png&&!jpg) throw bad("Only real PNG and JPEG images are accepted.");
  try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext()) throw bad("This image could not be read.");var reader=readers.next();
   try {reader.setInput(input);long pixels=(long)reader.getWidth(0)*reader.getHeight(0);if(pixels>20000000) throw bad("Image is too large. Use at most 20 megapixels.");if(reader.read(0)==null) throw bad("This image could not be read.");}finally{reader.dispose();}
  }
  String id=UUID.randomUUID().toString(),filename=id+(png?".png":".jpg");Path target=root.resolve("uploads").resolve(filename);Files.write(target,bytes);
  Item item=new Item(id,title,category,season,occasion,"/uploads/"+filename);List<Item> next=new ArrayList<>(state.items());next.add(item);
  try{persist(new State(next,state.looks()));}catch(IOException e){Files.deleteIfExists(target);throw e;}return item;
 }
 public synchronized void deleteItem(String id) throws IOException {
  Item item=state.items().stream().filter(i->i.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Piece not found."));
  List<Item> items=new ArrayList<>(state.items());items.remove(item);List<Look> looks=new ArrayList<>();
  for(Look look:state.looks()){List<String> ids=new ArrayList<>(look.items());ids.remove(id);if(!ids.isEmpty())looks.add(new Look(look.id(),look.name(),look.season(),look.occasion(),ids,look.created()));}
  persist(new State(items,looks));if(item.image().startsWith("/uploads/")) Files.deleteIfExists(root.resolve("uploads").resolve(Path.of(item.image()).getFileName()));
 }
 public synchronized Look save(SaveLook request) throws IOException {
  String title=name(request.name());metadata(request.season(),request.occasion());if(request.items()==null||request.items().isEmpty()||request.items().size()>7) throw bad("Choose between 1 and 7 pieces.");
  Set<String> slots=new HashSet<>();boolean dress=false,bottom=false;
  for(String id:request.items()){Item item=state.items().stream().filter(i->i.id().equals(id)).findFirst().orElseThrow(()->bad("A selected piece no longer exists."));if(!slots.add(slot(item.category())))throw bad("Choose one piece per outfit position.");dress|=item.category().equals("dresses");bottom|=item.category().equals("bottoms");}
  if(dress&&bottom)throw bad("A dress replaces the top and bottom.");
  Look look=new Look(UUID.randomUUID().toString(),title,request.season(),request.occasion(),List.copyOf(request.items()),Instant.now().toString());List<Look> next=new ArrayList<>(state.looks());next.add(0,look);persist(new State(state.items(),next));return look;
 }
 public synchronized void deleteLook(String id) throws IOException {List<Look> next=new ArrayList<>(state.looks());if(!next.removeIf(l->l.id().equals(id)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Look not found.");persist(new State(state.items(),next));}
 public Path uploads(){return root.resolve("uploads");}
}

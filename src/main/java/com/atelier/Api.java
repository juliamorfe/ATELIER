package com.atelier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.core.io.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.io.IOException;
import java.util.*;
@RestController
public class Api {
 private final Closet closet; public Api(Closet closet){this.closet=closet;}
 @GetMapping("/api/state") public Closet.State state(){return closet.state();}
 @GetMapping("/api/items") public List<Closet.Item> items(@RequestParam(defaultValue="all") String category,@RequestParam(defaultValue="all") String season,@RequestParam(defaultValue="all") String occasion){return closet.filter(category,season,occasion);}
 @GetMapping("/api/shuffle") public List<Closet.Item> shuffle(@RequestParam(defaultValue="all") String season,@RequestParam(defaultValue="all") String occasion){return closet.shuffle(season,occasion);}
 @PostMapping("/api/items") @ResponseStatus(HttpStatus.CREATED) public Closet.Item add(@RequestParam String name,@RequestParam String category,@RequestParam String season,@RequestParam String occasion,@RequestParam MultipartFile image)throws IOException{return closet.upload(name,category,season,occasion,image);}
 @DeleteMapping("/api/items/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable String id)throws IOException{closet.deleteItem(id);}
 @PostMapping("/api/looks") @ResponseStatus(HttpStatus.CREATED) public Closet.Look save(@RequestBody Closet.SaveLook request)throws IOException{return closet.save(request);}
 @DeleteMapping("/api/looks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id)throws IOException{closet.deleteLook(id);}
 @GetMapping("/uploads/{filename}") public ResponseEntity<Resource> image(@PathVariable String filename){if(!filename.matches("[a-f0-9-]{36}\\.(png|jpg)"))throw new ResponseStatusException(HttpStatus.NOT_FOUND);Resource resource=new FileSystemResource(closet.uploads().resolve(filename));if(!resource.exists())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").contentType(filename.endsWith("png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG).body(resource);}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> invalid(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Not found.":e.getReason()));}
 @ExceptionHandler(MaxUploadSizeExceededException.class) public ResponseEntity<Map<String,String>> size(){return ResponseEntity.status(413).body(Map.of("message","Image must be under 8 MB."));}
 @ExceptionHandler(IOException.class) public ResponseEntity<Map<String,String>> disk(){return ResponseEntity.internalServerError().body(Map.of("message","Could not read or save data. Check available disk space and image validity."));}
}

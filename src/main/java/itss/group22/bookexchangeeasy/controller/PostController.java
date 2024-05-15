package itss.group22.bookexchangeeasy.controller;

import itss.group22.bookexchangeeasy.dto.PostDTO;
import itss.group22.bookexchangeeasy.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    @PostMapping("/posts")
    public ResponseEntity<PostDTO> postPost(@RequestBody PostDTO postDTO) {
       return ResponseEntity.ok(postService.postPost(postDTO))     ;
    }
}
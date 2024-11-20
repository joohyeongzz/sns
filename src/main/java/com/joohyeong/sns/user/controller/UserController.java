package com.joohyeong.sns.user.controller;

import com.joohyeong.sns.post.util.FileUploadUtil;
import com.joohyeong.sns.user.domain.User;
import com.joohyeong.sns.user.dto.request.UserRegisterRequest;
import com.joohyeong.sns.user.dto.response.UserProFileDetailResponse;
import com.joohyeong.sns.user.dto.response.UserSearchResponse;
import com.joohyeong.sns.user.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileUploadUtil fileUploadUtil;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegisterRequest dto) {
        try {
            User registeredUser = userService.registerUser(dto);
            return ResponseEntity.ok("User registered successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/profile-upload")
    public ResponseEntity<?> profilePictureUpload(MultipartFile file) {


        String mimeType = file.getContentType();

        if (!mimeType.startsWith("image/") && !mimeType.startsWith("video/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }

        byte[] mediaBytes = fileUploadUtil.convertToBytes(file);


        return ResponseEntity.ok("이미지가 업로드되었습니다.");
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProFileDetailResponse> getUserProfile(@RequestParam long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUser(@RequestParam String keyword) {
        return ResponseEntity.ok(userService.searchUser(keyword,200L));
    }


}
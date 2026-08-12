package dev.totos.rag_hub.controllers;

import dev.totos.rag_hub.entity.User;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.records.SavedUser;
import dev.totos.rag_hub.records.UserPasswordRecord;
import dev.totos.rag_hub.records.UserUpdateRecord;
import dev.totos.rag_hub.repository.UserRepository;
import dev.totos.rag_hub.service.UserService;
import dev.totos.rag_hub.service.authService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final authService authService;
    UserController(UserService userService,authService authService){
        this.userService=userService;
        this.authService=authService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String,Object>> FindMe( Principal principal){
        UUID userId = UUID.fromString(principal.getName());
        User user =  userService.findMe(userId);
        SavedUser userDTO = new SavedUser(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt().toString());
        Map<String,Object> response = Map.of("message","user retured successfully","user",userDTO);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/me")
    public  ResponseEntity<?> DeleteMe ( Principal principal, HttpServletResponse res){
        //TODO  cascading vecoredb
        UUID userId = UUID.fromString(principal.getName());

        userService.deleteMe(userId);
        ResponseCookie accessCookie = ResponseCookie.from("accessToken",null).httpOnly(true).secure(false).maxAge(0).path("/").build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", null)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE,accessCookie.toString());
        res.addHeader(HttpHeaders.SET_COOKIE,refreshCookie.toString());
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateUser(
            @AuthenticationPrincipal Principal principal,
            @Valid @RequestBody UserUpdateRecord record
    ) {
        if (record.email() == null && record.username() == null) {
            throw new ApiException("Please include fields to update", HttpStatus.BAD_REQUEST);
        }

        UUID userId = UUID.fromString(principal.getName());

        User savedUser = userService.updateUser(userId, record.email(), record.username());
        SavedUser userDTO = new SavedUser(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getCreatedAt().toString()
        );

        return ResponseEntity.ok(Map.of(
                "message", "User updated successfully!",
                "user", userDTO
        ));
    }
    @PutMapping("/me/password")
    public ResponseEntity<?> updatePassword(
             Principal principal,
            @Valid @RequestBody UserPasswordRecord record
    ) {
        UUID userId = UUID.fromString(principal.getName());

        userService.updateUserPassword(userId,record.oldPassword(),record.newPassword(),record.confirmNewPassword());


        return ResponseEntity.ok(Map.of("message", "Password successfully updated"));
    }

}

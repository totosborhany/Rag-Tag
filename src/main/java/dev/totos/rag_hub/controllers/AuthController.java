package dev.totos.rag_hub.controllers;

import dev.totos.rag_hub.entity.User;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.records.RegisterRequest;
import dev.totos.rag_hub.records.LoginRequest;
import dev.totos.rag_hub.records.ResetPasswordRecord;
import dev.totos.rag_hub.records.SavedUser;
import dev.totos.rag_hub.service.JwtService;
import dev.totos.rag_hub.service.authService; // Note: Should ideally be AuthService (capital A)
import dev.totos.rag_hub.utils.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final authService authService;
    private final JwtService jwtService;
    private final CookieUtils cookieUtils; // Inject your new utility

    public AuthController(authService authService, JwtService jwtService, CookieUtils cookieUtils) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieUtils = cookieUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse res) {
        if (authService.checkUserExists(request.email(), request.username())) {
            throw new ApiException("Sorry, a user with this email or username already exists", HttpStatus.CONFLICT);
        }
        if (!request.password().equalsIgnoreCase(request.confirmPassword())) {
            throw new ApiException("Passwords have to match", HttpStatus.BAD_REQUEST);
        }

        User newUser = authService.createUser(request.username(), request.email(), request.password());
        setAuthCookiesAndRedis(newUser.getId(), res); // Moved to private helper below!

        SavedUser responseDto = new SavedUser(newUser.getId(), newUser.getUsername(), newUser.getEmail(), newUser.getCreatedAt().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered successfully!",
                "user", responseDto
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse res) {
        User myUser = authService.Login(request.email(), request.password());

        setAuthCookiesAndRedis(myUser.getId(), res); // Reusing the same helper!

        SavedUser userDTO = new SavedUser(myUser.getId(), myUser.getUsername(), myUser.getEmail(), myUser.getCreatedAt().toString());
        return ResponseEntity.ok(Map.of(
                "message", "Logged in successfully",
                "user", userDTO
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse res) {
        if (refreshToken == null || !jwtService.validateRefreshToken(refreshToken)) {
            throw new ApiException("Token expired, please log in", HttpStatus.UNAUTHORIZED);
        }
        UUID userId = jwtService.extractUserIdFromRefresh(refreshToken);

        Boolean isValidRedis =  jwtService.validateRefreshTokenWithRedis(refreshToken,userId);
        if(!isValidRedis){
            throw new ApiException("Sorry invalid token please login",HttpStatus.UNAUTHORIZED);
        }
        setAuthCookiesAndRedis(userId,res);
        return ResponseEntity.ok(Map.of("message", "Token successfully renewed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse res,  Principal principal) {
        UUID uuid = UUID.fromString(principal.getName());

        jwtService.deleteRefreshTokenFromRedis(uuid);

        res.addHeader(HttpHeaders.SET_COOKIE, cookieUtils.clearAccessCookie().toString());
        res.addHeader(HttpHeaders.SET_COOKIE, cookieUtils.clearRefreshCookie().toString());

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            throw new ApiException("Email is required", HttpStatus.BAD_REQUEST);
        }

        authService.processForgotPassword(email);

        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestParam("token") String token,
            @RequestBody ResetPasswordRecord body
    ) {
        String newPassword = body.newPassword();
        String confirmNewPassword = body.confirmNewPassword();
        if (newPassword == null || newPassword.isBlank()) {
            throw new ApiException("New password is required", HttpStatus.BAD_REQUEST);
        }

        authService.processResetPassword(token, newPassword,confirmNewPassword);

        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. You can now log in."));
    }


    private void setAuthCookiesAndRedis(UUID userId, HttpServletResponse res) {
        String accessToken = jwtService.generateAccessToken(userId);
        String refreshToken = jwtService.generateRefreshToken(userId);

        jwtService.saveRefreshTokenToRedis(userId, refreshToken);


        res.addHeader(HttpHeaders.SET_COOKIE, cookieUtils.createAccessCookie(accessToken).toString());
        res.addHeader(HttpHeaders.SET_COOKIE, cookieUtils.createRefreshCookie(refreshToken).toString());
    }
}
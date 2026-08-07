package dev.totos.rag_hub.service;

import dev.totos.rag_hub.entity.User;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.UUID;

@Service
public class authService {
    private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final EmailService emailService;
        private final RedisTemplate<String,String> redisTemplate;
    authService(UserRepository userRepository,PasswordEncoder passwordEncoder,EmailService emailService,RedisTemplate<String,String> redisTemplate ){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.emailService=emailService;
        this.redisTemplate=redisTemplate;
    }
   public Boolean checkUserExists(String email,String userName){

        return userRepository.existsByEmailOrUsername(email,userName) ;
   }
   @Transactional
   public User createUser (String username, String email, String password){

        String hashedPassword = passwordEncoder.encode(password);
        User savedUser = new User(username,email,hashedPassword);
       User thesaved= userRepository.save(savedUser);
        return thesaved;
   }

   public User Login(String email, String password){

        User user = userRepository.findByEmail(email) .orElseThrow(() -> new ApiException("error loging you in" , HttpStatus.BAD_REQUEST));
       Boolean match = passwordEncoder.matches(password,user.getPasswordHash());
        if(!match){
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }


       return user;
   }
   @Transactional
    public void processForgotPassword(String email){

        User user = userRepository.findByEmail(email).orElseThrow(()->new ApiException("Sorry a problem occured ",HttpStatus.BAD_REQUEST));


       String resetToken = UUID.randomUUID().toString();
       redisTemplate.opsForValue().set("resetToken:" + resetToken, user.getId().toString(), Duration.ofMinutes(15));
       String resetUrl = "http://localhost:8080/api/v1/auth/reset-password?token=" + resetToken;

       emailService.sendSimpleEmail(user.getEmail(), "Password reset token", "your password reset token is this url"+resetUrl);


    }
    @Transactional
    public void processResetPassword(String token ,String newPassword,String confirmNewPassword){

        String rawUserId = redisTemplate.opsForValue().get("resetToken:" + token);

        // 2. Check null BEFORE parsing UUID
        if (rawUserId == null) {
            throw new ApiException("Token expired or invalid", HttpStatus.BAD_REQUEST);
        }

        UUID userId = UUID.fromString(rawUserId);

        User user = userRepository.findById(userId).orElseThrow(()->new ApiException("Token expired or invalid", HttpStatus.BAD_REQUEST));

        if(!(newPassword.equals(confirmNewPassword))){
            throw new ApiException("Sorry passwords should match", HttpStatus.BAD_REQUEST);

        }
        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);
    }


}

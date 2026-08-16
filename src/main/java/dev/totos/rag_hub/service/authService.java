package dev.totos.rag_hub.service;

import dev.totos.rag_hub.entity.User;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.repository.UserRepository;
import dev.totos.rag_hub.utils.RedisUtil;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
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
        private final  JwtService jwtService;
        private final RedisUtil redisUtil;
    authService(UserRepository userRepository,PasswordEncoder passwordEncoder,EmailService emailService,JwtService jwtService,RedisUtil redisUtil ){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.emailService=emailService;
        this.jwtService=jwtService;
        this.redisUtil=redisUtil;
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

        User user = userRepository.findByEmail(email) .orElseThrow(() -> new ApiException("Invalid email or password" , HttpStatus.UNAUTHORIZED));
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

       redisUtil.saveResetTokenToredis( user.getId(),resetToken);

       String resetUrl = "http://localhost:8080/api/v1/auth/reset-password?token=" + resetToken;
       System.out.println(resetUrl);
       emailService.sendSimpleEmail(user.getEmail(), "Password reset token", "your password reset token is this url :   \n "+resetUrl);


    }
    @Transactional
    public void processResetPassword(String token ,String newPassword,String confirmNewPassword){

        String rawUserId = redisUtil.getResetTokenFromredis(token);

        // 2. Check null BEFORE parsing UUID
        if (rawUserId == null) {
            throw new BadCredentialsException("Token expired or invalid");
        }

        UUID userId = UUID.fromString(rawUserId);

        User user = userRepository.findById(userId).orElseThrow(()->new BadCredentialsException("Token expired or invalid"));


        if(!(newPassword.equals(confirmNewPassword))){
            throw new ApiException("Sorry passwords should match", HttpStatus.BAD_REQUEST);

        }
        redisUtil.deleteFromRedis("resetToken",null,token);
        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);
    }


}

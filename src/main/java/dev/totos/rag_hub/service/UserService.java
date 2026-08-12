package dev.totos.rag_hub.service;

import dev.totos.rag_hub.entity.User;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.repository.DocumentRepository;
import dev.totos.rag_hub.repository.UserRepository;
import dev.totos.rag_hub.utils.RedisUtil;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private  final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;
    private final VectorStore vectorStore;
    private final RedisUtil redisUtil;
    UserService(UserRepository userRepository,DocumentRepository documentRepository,PasswordEncoder passwordEncoder,VectorStore vectorStore,RedisUtil redisUtil){
        this.userRepository=userRepository;
        this.documentRepository = documentRepository;
        this.passwordEncoder = passwordEncoder;
        this.vectorStore=vectorStore;
        this.redisUtil=redisUtil;

    }

    public User findMe(UUID uuid){
        User myUser = userRepository.findById(uuid).orElseThrow(()->new ApiException("Sorry user doesnt exist", HttpStatus.NOT_FOUND));
        return myUser;
    }
    @Transactional
    public  void deleteMe(UUID uuid){
        if (!userRepository.existsById(uuid)) {
        throw new ApiException("User not found or already deleted", HttpStatus.NOT_FOUND);
            }
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            userRepository.deleteById(uuid);
            redisUtil.deleteFromRedis("refreshToken",uuid,null);
            vectorStore.delete( b.eq("userId", uuid.toString()).build());
            documentRepository.deleteByUserId(uuid);
        }

    @Transactional
    public User updateUser(UUID id,String email,String username){

        User currentUser = userRepository.findById(id).orElseThrow(()-> new ApiException("user not found",HttpStatus.BAD_REQUEST));


        boolean isEmailChanging = email != null && !email.equals(currentUser.getEmail());
        boolean isUsernameChanging = username != null && !username.equals(currentUser.getUsername());

        if (isEmailChanging && userRepository.existsByEmailOrUsername(email, null)) {
            throw new ApiException("Sorry, this email is already taken", HttpStatus.CONFLICT);
        }
        if (isUsernameChanging && userRepository.existsByEmailOrUsername(null, username)) {
            throw new ApiException("Sorry, this username is already taken", HttpStatus.CONFLICT);
        }

        if (email != null) {
            currentUser.setEmail(email);
        }
        if (username != null) {
            currentUser.setUsername(username);
        }
        return userRepository.save(currentUser);
    }
    @Transactional
    public void updateUserPassword(UUID userId, String oldPassword, String newPassword, String confirmNewPassword){
        User user = userRepository.findById(userId) .orElseThrow(() -> new ApiException("error cant finid user" , HttpStatus.BAD_REQUEST));

        Boolean match = passwordEncoder.matches(oldPassword,user.getPasswordHash());
        if(!match){
            throw new ApiException("password is invalid", HttpStatus.BAD_REQUEST);
        }
        if(!(newPassword.equals(confirmNewPassword))){
            throw new ApiException("sorry new password and confimr new password dont match", HttpStatus.BAD_REQUEST);

        }
        String newPAsswordHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newPAsswordHash);
        userRepository.save(user);

    }
}

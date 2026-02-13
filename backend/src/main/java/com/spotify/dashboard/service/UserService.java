package com.spotify.dashboard.service;

import com.spotify.dashboard.model.User;
import com.spotify.dashboard.repository.UserRepository;
import com.spotify.dashboard.util.EncryptionUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    public UserService(UserRepository userRepository, EncryptionUtil encryptionUtil) {
        this.userRepository = userRepository;
        this.encryptionUtil = encryptionUtil;
    }

    @SuppressWarnings("unchecked")
    public User createOrUpdateUser(Map<String, Object> spotifyUserData, String refreshToken) {
        String spotifyId = (String) spotifyUserData.get("id");
        
        User user = userRepository.findBySpotifyId(spotifyId)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setSpotifyId(spotifyId);
                return newUser;
            });
        
        user.setDisplayName((String) spotifyUserData.get("display_name"));
        user.setEmail((String) spotifyUserData.get("email"));
        user.setAccountType((String) spotifyUserData.get("product"));
        user.setCountry((String) spotifyUserData.get("country"));
        
        var images = (List<Map<String, Object>>) spotifyUserData.get("images");
        if (images != null && !images.isEmpty()) {
            user.setImageUrl((String) images.get(0).get("url"));
        }
        
        if (refreshToken != null && !refreshToken.isEmpty()) {
            String encrypted = encryptionUtil.encrypt(refreshToken);
            user.setEncryptedRefreshToken(encrypted);
        }
        
        user.setLastLogin(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    public String getRefreshToken(String spotifyId) {
        return userRepository.findBySpotifyId(spotifyId)
            .map(User::getEncryptedRefreshToken)
            .filter(token -> token != null && !token.isEmpty())
            .map(encryptionUtil::decrypt)
            .orElse(null);
    }

    public void updateRefreshToken(String spotifyId, String newRefreshToken) {
        userRepository.findBySpotifyId(spotifyId).ifPresent(user -> {
            user.setEncryptedRefreshToken(encryptionUtil.encrypt(newRefreshToken));
            userRepository.save(user);
        });
    }
}
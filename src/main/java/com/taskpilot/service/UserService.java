package com.taskpilot.service;

import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public UserService(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void saveNotionIntegrationDetails(
            Long userId, String accessToken, String workspaceId,
            String workspaceName, String workspaceIcon, String botId
    ) {
        // Encrypt the access token before saving
        String encryptedAccessToken = encryptionService.encrypt(accessToken);

        // Find the user and update their details
        userRepository.findById(userId).ifPresent(user -> {
            user.setNotionAccessToken(encryptedAccessToken);
            user.setNotionWorkspaceId(workspaceId);
            user.setNotionWorkspaceName(workspaceName);
            user.setNotionWorkspaceIcon(workspaceIcon);
            user.setNotionBotId(botId);
            userRepository.save(user);
        });
    }

    public void setNotionTargetDatabase(Long userId, String databaseId, String databaseName) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setNotionTargetDatabaseId(databaseId);
            user.setNotionTargetDatabaseName(databaseName);
            userRepository.save(user);
        });
    }
}

package com.examradar.backend.service;

import com.examradar.backend.domain.DeviceToken;
import com.examradar.backend.domain.UserProfile;
import com.examradar.backend.dto.DeviceRegistrationRequest;
import com.examradar.backend.repository.DeviceTokenRepository;
import com.examradar.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional
public class DeviceService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserService userService;

    public DeviceService(DeviceTokenRepository deviceTokenRepository, UserService userService) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userService = userService;
    }

    public String registerDevice(AuthenticatedUser authUser, DeviceRegistrationRequest request) {
        UserProfile user = userService.getOrCreateUser(authUser);

        Optional<DeviceToken> existingOpt = deviceTokenRepository.findByToken(request.token());
        if (existingOpt.isPresent()) {
            DeviceToken token = existingOpt.get();
            token.setUser(user);
            token.setDeviceType(request.deviceType() != null ? request.deviceType() : "ANDROID");
            token.setAppVersion(request.appVersion());
            token.setIsActive(true);
            token.setLastUsedAt(OffsetDateTime.now());
            deviceTokenRepository.save(token);
            return request.token();
        }

        DeviceToken newToken = new DeviceToken(user, request.token(), request.deviceType(), request.appVersion());
        deviceTokenRepository.save(newToken);
        return request.token();
    }

    public void unregisterDevice(AuthenticatedUser authUser, String token) {
        deviceTokenRepository.findByToken(token).ifPresent(dt -> {
            if (dt.getUser().getId().equals(authUser.getId())) {
                dt.setIsActive(false);
                deviceTokenRepository.save(dt);
            }
        });
    }
}

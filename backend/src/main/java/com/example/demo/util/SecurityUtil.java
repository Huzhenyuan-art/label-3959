package com.example.demo.util;

import com.example.demo.enums.RoleEnum;
import com.example.demo.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public class SecurityUtil {

    private SecurityUtil() {
    }

    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static Optional<UserDetails> getUserDetails() {
        return getAuthentication()
                .filter(auth -> auth.getPrincipal() instanceof UserDetails)
                .map(auth -> (UserDetails) auth.getPrincipal());
    }

    public static Optional<CustomUserDetails> getCustomUserDetails() {
        return getUserDetails()
                .filter(ud -> ud instanceof CustomUserDetails)
                .map(ud -> (CustomUserDetails) ud);
    }

    public static Long getCurrentUserId() {
        return getCustomUserDetails()
                .map(CustomUserDetails::getUserId)
                .orElse(null);
    }

    public static String getCurrentUsername() {
        return getUserDetails()
                .map(UserDetails::getUsername)
                .orElse(null);
    }

    public static String getCurrentRole() {
        return getCustomUserDetails()
                .map(CustomUserDetails::getRole)
                .orElse(null);
    }

    public static boolean isAdmin() {
        return RoleEnum.ADMIN.getCode().equals(getCurrentRole());
    }

    public static boolean isUser() {
        return RoleEnum.USER.getCode().equals(getCurrentRole());
    }

    public static void checkAdminOrThrow() {
        if (!isAdmin()) {
            throw new SecurityException("只有管理员可以执行此操作");
        }
    }

    public static void checkResourceOwnerOrThrow(Long ownerId) {
        if (!isAdmin() && !ownerId.equals(getCurrentUserId())) {
            throw new SecurityException("无权访问他人资源");
        }
    }
}

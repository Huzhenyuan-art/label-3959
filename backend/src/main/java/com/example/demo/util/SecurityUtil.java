package com.example.demo.util;

import com.example.demo.enums.RoleEnum;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            if (userDetails instanceof com.example.demo.security.CustomUserDetails customUserDetails) {
                return customUserDetails.getUserId();
            }
        }
        return null;
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }

    public static String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            if (userDetails instanceof com.example.demo.security.CustomUserDetails customUserDetails) {
                return customUserDetails.getRole();
            }
        }
        return null;
    }

    public static boolean isAdmin() {
        return RoleEnum.ADMIN.getCode().equals(getCurrentRole());
    }

    public static boolean isUser() {
        return RoleEnum.USER.getCode().equals(getCurrentRole());
    }
}

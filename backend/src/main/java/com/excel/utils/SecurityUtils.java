package com.excel.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 * 提供当前登录用户身份与角色信息，用于后端数据范围控制
 */
public class SecurityUtils {

    /** 医保办：可见全院任务 */
    public static final String ROLE_ADMIN = "ADMIN";
    /** 科室人员：仅可见本人上传的批次 */
    public static final String ROLE_DEPT = "DEPT";

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    /**
     * 获取当前登录用户角色（ADMIN/DEPT）
     */
    public static String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if (role != null && role.startsWith("ROLE_")) {
                    return role.substring(5);
                }
            }
        }
        return null;
    }

    /**
     * 当前用户是否为医保办（可见全院数据）
     */
    public static boolean isAdmin() {
        return ROLE_ADMIN.equals(getCurrentRole());
    }
}

package com.excel.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类：获取当前登录用户及其数据范围
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /**
     * 获取当前登录用户，未登录时抛出异常
     */
    public static LoginUser requireLoginUser() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new IllegalStateException("未登录或登录已过期");
        }
        return loginUser;
    }
}

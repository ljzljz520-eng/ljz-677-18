package com.excel.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户信息，作为 SecurityContext 中的 Principal。
 * 数据范围判断仅以后端解析出的角色与科室为准，不信任前端传值。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    /** 医保办：可查看全院任务 */
    public static final String ROLE_ADMIN = "ADMIN";
    /** 科室人员：仅可查看本科室上传的批次 */
    public static final String ROLE_DEPT = "DEPT";

    private Long userId;

    private String username;

    /**
     * 角色：ADMIN-医保办 DEPT-科室人员
     */
    private String role;

    /**
     * 所属科室代码（医保办可为空）
     */
    private String deptCode;

    /**
     * 是否医保办（全院数据范围）
     */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}

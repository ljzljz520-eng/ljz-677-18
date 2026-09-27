package com.excel.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;

    private Long userId;

    private String username;

    private String realName;

    /**
     * 角色：ADMIN-医保办 DEPT-科室人员
     */
    private String role;

    /**
     * 所属科室代码
     */
    private String deptCode;

    /**
     * 所属科室名称
     */
    private String deptName;
}

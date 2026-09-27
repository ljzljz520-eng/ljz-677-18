package com.excel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    /**
     * 角色：ADMIN-医保办（全院范围） DEPT-科室人员（仅本科室）
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

    private String email;

    private String phone;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

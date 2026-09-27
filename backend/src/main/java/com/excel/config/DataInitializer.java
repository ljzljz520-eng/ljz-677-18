package com.excel.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.excel.entity.User;
import com.excel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 兼容已有数据库：为 sys_user 补充角色与科室列
        ensureUserTableColumns();
        initAdminUser();
        initDeptUsers();
    }

    /**
     * 兼容历史数据库：检查 sys_user 是否缺少 role / dept_name 列，缺失则自动补齐
     */
    private void ensureUserTableColumns() {
        addColumnIfMissing("role",
                "VARCHAR(20) NOT NULL DEFAULT 'DEPT' COMMENT '角色：ADMIN-医保办 DEPT-科室人员'");
        addColumnIfMissing("dept_name",
                "VARCHAR(50) COMMENT '科室名称'");
    }

    private void addColumnIfMissing(String column, String definition) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = ?",
                    Integer.class, column);
            if (count == null || count == 0) {
                jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN " + column + " " + definition);
                logger.info("sys_user 表新增列: {}", column);
            }
        } catch (Exception e) {
            logger.warn("检查/新增 sys_user.{} 列失败: {}", column, e.getMessage());
        }
    }

    private void initAdminUser() {
        // 检查admin用户是否存在
        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")
        );

        if (existingUser == null) {
            // 创建医保办管理员用户
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("医保办管理员");
            admin.setRole("ADMIN");
            admin.setDeptName("医保办");
            admin.setEmail("admin@example.com");
            admin.setPhone("13800000000");
            admin.setStatus(1);
            admin.setDeleted(0);
            userMapper.insert(admin);
            logger.info("初始化医保办管理员用户成功: admin / admin123");
        } else {
            // 更新密码与角色，确保一致
            existingUser.setPassword(passwordEncoder.encode("admin123"));
            existingUser.setRole("ADMIN");
            if (existingUser.getDeptName() == null) {
                existingUser.setDeptName("医保办");
            }
            userMapper.updateById(existingUser);
            logger.info("更新医保办管理员账号成功");
        }
    }

    /**
     * 初始化科室演示账号（仅可见本人上传的批次）
     */
    private void initDeptUsers() {
        createDeptUserIfAbsent("neike", "内科");
        createDeptUserIfAbsent("waike", "外科");
    }

    private void createDeptUserIfAbsent(String username, String deptName) {
        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (existingUser == null) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRealName(deptName + "操作员");
            user.setRole("DEPT");
            user.setDeptName(deptName);
            user.setStatus(1);
            user.setDeleted(0);
            userMapper.insert(user);
            logger.info("初始化科室用户成功: {} / 123456（{}）", username, deptName);
        } else if (existingUser.getRole() == null) {
            existingUser.setRole("DEPT");
            existingUser.setDeptName(deptName);
            userMapper.updateById(existingUser);
        }
    }
}

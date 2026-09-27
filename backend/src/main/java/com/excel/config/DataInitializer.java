package com.excel.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.excel.entity.User;
import com.excel.mapper.UserMapper;
import com.excel.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
        initDeptUser("neike01", "neike123", "李医生", "D001", "内科");
        initDeptUser("waike01", "waike123", "王医生", "D002", "外科");
    }

    /**
     * 医保办账号：全院数据范围
     */
    private void initAdminUser() {
        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")
        );

        if (existingUser == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("医保办管理员");
            admin.setRole(LoginUser.ROLE_ADMIN);
            admin.setEmail("admin@example.com");
            admin.setPhone("13800000000");
            admin.setStatus(1);
            admin.setDeleted(0);
            userMapper.insert(admin);
            logger.info("初始化医保办管理员成功: admin / admin123");
        } else {
            // 更新密码与角色，确保一致
            existingUser.setPassword(passwordEncoder.encode("admin123"));
            existingUser.setRole(LoginUser.ROLE_ADMIN);
            userMapper.updateById(existingUser);
            logger.info("更新医保办管理员账号成功");
        }
    }

    /**
     * 科室账号：仅本科室数据范围，已存在则不重复创建
     */
    private void initDeptUser(String username, String password, String realName,
                              String deptCode, String deptName) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (count != null && count > 0) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(LoginUser.ROLE_DEPT);
        user.setDeptCode(deptCode);
        user.setDeptName(deptName);
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
        logger.info("初始化科室用户成功: {} / {}（{}）", username, password, deptName);
    }
}

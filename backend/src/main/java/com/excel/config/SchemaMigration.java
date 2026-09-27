package com.excel.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 轻量 schema 迁移：为已有数据库补充数据隔离所需的列与索引。
 * 必须在 DataInitializer 之前执行（Order 值更小）。
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class SchemaMigration implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 用户表：角色与所属科室
        addColumnIfMissing("sys_user", "role",
                "VARCHAR(20) DEFAULT 'DEPT' COMMENT '角色：ADMIN-医保办 DEPT-科室人员'");
        addColumnIfMissing("sys_user", "dept_code",
                "VARCHAR(50) COMMENT '所属科室代码'");
        addColumnIfMissing("sys_user", "dept_name",
                "VARCHAR(100) COMMENT '所属科室名称'");

        // 导入记录表：上传科室（数据隔离维度）
        addColumnIfMissing("import_record", "dept_code",
                "VARCHAR(50) COMMENT '上传科室代码'");
        addColumnIfMissing("import_record", "dept_name",
                "VARCHAR(100) COMMENT '上传科室名称'");
        addIndexIfMissing("import_record", "idx_dept_code", "dept_code");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
            logger.info("schema迁移: {}.{} 列已添加", table, column);
        }
    }

    private void addIndexIfMissing(String table, String indexName, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class, table, indexName);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD INDEX `" + indexName + "` (`" + column + "`)");
            logger.info("schema迁移: {}.{} 索引已添加", table, indexName);
        }
    }
}

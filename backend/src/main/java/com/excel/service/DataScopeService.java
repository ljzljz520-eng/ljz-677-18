package com.excel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.excel.entity.ImportRecord;
import com.excel.exception.AccessDeniedException;
import com.excel.mapper.ImportRecordMapper;
import com.excel.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 数据权限范围服务
 * 隔离规则：
 * - 医保办（ADMIN）：可见全院上传任务
 * - 科室人员（DEPT）：仅可见本人上传的批次及其异常结果
 * 所有任务列表、批次详情、上报、导出都必须经过此服务限定范围，
 * 不允许由前端自行过滤。
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private static final Logger logger = LoggerFactory.getLogger(DataScopeService.class);

    private final ImportRecordMapper importRecordMapper;

    /**
     * 为导入记录查询追加数据范围条件。
     * 医保办不追加条件（可见全院）；科室人员强制限定 operator_id 为本人。
     */
    public void applyScope(LambdaQueryWrapper<ImportRecord> wrapper) {
        if (!SecurityUtils.isAdmin()) {
            Long userId = SecurityUtils.getCurrentUserId();
            // 用户ID为空时给一个不可能命中的值，保证查不到任何数据
            wrapper.eq(ImportRecord::getOperatorId, userId != null ? userId : -1L);
        }
    }

    /**
     * 校验当前用户是否有权访问指定批次，无权访问时抛出 AccessDeniedException。
     *
     * @param batchNo 批次号
     * @return 批次对应的导入记录
     */
    public ImportRecord checkBatchAccess(String batchNo) {
        ImportRecord record = importRecordMapper.selectOne(
                new LambdaQueryWrapper<ImportRecord>()
                        .eq(ImportRecord::getBatchNo, batchNo)
        );
        if (record == null) {
            throw new AccessDeniedException("批次不存在或无权访问");
        }
        if (!SecurityUtils.isAdmin()) {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null || !userId.equals(record.getOperatorId())) {
                logger.warn("用户 {} 越权访问批次 {}", userId, batchNo);
                throw new AccessDeniedException("无权访问其他科室上传的批次数据");
            }
        }
        return record;
    }
}

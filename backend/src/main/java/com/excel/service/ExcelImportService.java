package com.excel.service;

import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.excel.dto.ExcelDataDTO;
import com.excel.dto.ImportResultDTO;
import com.excel.entity.ExcelData;
import com.excel.entity.ImportRecord;
import com.excel.entity.User;
import com.excel.listener.ExcelDataListener;
import com.excel.mapper.ExcelDataMapper;
import com.excel.mapper.ImportRecordMapper;
import com.excel.mapper.UserMapper;
import com.excel.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelImportService.class);

    private final ExcelDataMapper excelDataMapper;
    private final ImportRecordMapper importRecordMapper;
    private final UserMapper userMapper;

    /**
     * 导入Excel文件
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportResultDTO importExcel(MultipartFile file, Long operatorId) throws IOException {
        String batchNo = IdUtil.fastSimpleUUID();
        String fileName = file.getOriginalFilename();
        long fileSize = file.getSize();

        logger.info("开始导入Excel文件: {}, 大小: {} bytes, 批次号: {}", fileName, fileSize, batchNo);

        // 获取操作人信息
        User operator = userMapper.selectById(operatorId);
        String operatorName = operator != null ? operator.getRealName() : "系统";

        // 创建导入记录（冗余上传人所属科室，作为数据隔离维度）
        ImportRecord record = new ImportRecord();
        record.setBatchNo(batchNo);
        record.setFileName(fileName);
        record.setFileSize(fileSize);
        record.setStatus(0);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        if (operator != null) {
            record.setDeptCode(operator.getDeptCode());
            record.setDeptName(operator.getDeptName());
        }
        importRecordMapper.insert(record);

        // 使用EasyExcel SAX模式解析，避免OOM
        ExcelDataListener listener = new ExcelDataListener(excelDataMapper, batchNo);

        try {
            // 根据文件后缀判断Excel类型
            ExcelTypeEnum excelType = fileName != null && fileName.endsWith(".xlsx")
                    ? ExcelTypeEnum.XLSX : ExcelTypeEnum.XLS;

            EasyExcel.read(file.getInputStream(), ExcelDataDTO.class, listener)
                    .excelType(excelType)
                    .charset(StandardCharsets.UTF_8)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();

            // 更新导入记录
            record.setTotalCount(listener.getTotalCount());
            record.setSuccessCount(listener.getSuccessCount());
            record.setFailCount(listener.getFailCount());
            record.setStatus(listener.getFailCount() > 0 ? 2 : 1);

            if (!listener.getErrorList().isEmpty()) {
                StringBuilder errorDetails = new StringBuilder();
                for (ExcelDataDTO error : listener.getErrorList()) {
                    errorDetails.append("第").append(error.getRowIndex()).append("行: ")
                            .append(error.getErrorMsg()).append("\n");
                }
                record.setErrorDetails(errorDetails.toString());
            }

            importRecordMapper.updateById(record);

            logger.info("Excel导入完成: 总计{}条，成功{}条，失败{}条",
                    listener.getTotalCount(), listener.getSuccessCount(), listener.getFailCount());

            return ImportResultDTO.builder()
                    .batchNo(batchNo)
                    .totalCount(listener.getTotalCount())
                    .successCount(listener.getSuccessCount())
                    .failCount(listener.getFailCount())
                    .errorList(listener.getErrorList())
                    .status(listener.getFailCount() > 0 ? "completed_with_errors" : "completed")
                    .message(String.format("导入完成，总计%d条，成功%d条，失败%d条",
                            listener.getTotalCount(), listener.getSuccessCount(), listener.getFailCount()))
                    .build();

        } catch (Exception e) {
            logger.error("Excel导入失败", e);
            record.setStatus(2);
            record.setErrorDetails("导入失败: " + e.getMessage());
            importRecordMapper.updateById(record);
            throw new RuntimeException("Excel导入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取导入记录列表（按登录用户数据范围过滤，后端强制隔离）
     * 医保办（ADMIN）：全院任务；科室人员（DEPT）：仅本科室上传的批次
     */
    public Page<ImportRecord> getImportRecords(Integer pageNum, Integer pageSize, LoginUser loginUser) {
        Page<ImportRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ImportRecord> wrapper = new LambdaQueryWrapper<>();

        if (!loginUser.isAdmin()) {
            // 科室用户：仅本科室数据；未分配科室时查不到任何数据（fail closed）
            if (!StringUtils.hasText(loginUser.getDeptCode())) {
                return page;
            }
            wrapper.eq(ImportRecord::getDeptCode, loginUser.getDeptCode());
        }

        wrapper.orderByDesc(ImportRecord::getCreateTime);
        return importRecordMapper.selectPage(page, wrapper);
    }

    /**
     * 校验当前用户是否有权访问指定批次，并返回批次记录。
     * 科室用户越权访问其他科室批次时抛出 AccessDeniedException。
     */
    public ImportRecord checkBatchAccess(String batchNo, LoginUser loginUser) {
        ImportRecord record = importRecordMapper.selectOne(
                new LambdaQueryWrapper<ImportRecord>()
                        .eq(ImportRecord::getBatchNo, batchNo)
        );
        if (record == null) {
            throw new RuntimeException("批次不存在: " + batchNo);
        }
        if (!loginUser.isAdmin()
                && !java.util.Objects.equals(record.getDeptCode(), loginUser.getDeptCode())) {
            logger.warn("用户 {} 越权访问批次 {}（所属科室: {}）",
                    loginUser.getUsername(), batchNo, record.getDeptCode());
            throw new AccessDeniedException("无权访问其他科室的批次数据");
        }
        return record;
    }

    /**
     * 根据批次号获取数据
     */
    public Page<ExcelData> getDataByBatch(String batchNo, Integer pageNum, Integer pageSize) {
        Page<ExcelData> page = new Page<>(pageNum, pageSize);
        return excelDataMapper.selectPage(page,
                new LambdaQueryWrapper<ExcelData>()
                        .eq(ExcelData::getBatchNo, batchNo)
                        .orderByAsc(ExcelData::getId));
    }

    /**
     * 获取待上报数据
     */
    public List<ExcelData> getPendingReportData(String batchNo) {
        return excelDataMapper.selectByBatchAndStatus(batchNo, 0);
    }

    /**
     * 下载导入模板
     */
    public byte[] downloadTemplate() {
        // 返回模板的字节数组
        return null; // Controller中处理
    }
}

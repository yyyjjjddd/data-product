package com.example.metrics.util;

import com.example.metrics.exception.BusinessException;
import com.example.metrics.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FieldValidator单元测试
 *
 * <p>测试字段校验逻辑：
 * <ul>
 *   <li>正常情况：合法配置通过校验</li>
 *   <li>异常情况：非法表名、字段名、聚合方式</li>
 *   <li>边界条件：空值、大小写敏感、SQL注入防护</li>
 * </ul>
 */
class FieldValidatorTest {

    @Test
    void validateAll_withValidConfig_shouldPass() {
        // given - 合法的完整配置
        String tableName = "asset";
        String field = "asset_id";
        String aggregation = "COUNT";
        String filterCondition = "status='approved'";

        // when & then - 不应抛出异常
        assertDoesNotThrow(() -> FieldValidator.validateAll(tableName, field, aggregation, filterCondition));
    }

    @Test
    void validateAll_withNullFilterCondition_shouldPass() {
        // given - filterCondition为null
        String tableName = "asset";
        String field = "file_size_bytes";
        String aggregation = "SUM";
        String filterCondition = null;

        // when & then - 不应抛出异常
        assertDoesNotThrow(() ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
    }

    @Test
    void validateAll_withEmptyFilterCondition_shouldPass() {
        // given - filterCondition为空字符串
        String tableName = "asset";
        String field = "status";
        String aggregation = "COUNT";
        String filterCondition = "";

        // when & then - 不应抛出异常
        assertDoesNotThrow(() ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
    }

    // ========== 表名校验测试 ==========
    @Test
    void validateAll_withInvalidTableName_shouldThrowException() {
        // given - 不支持的表名
        String tableName = "users";
        String field = "asset_id";
        String aggregation = "COUNT";
        String filterCondition = null;

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.METRIC_CONFIG_INVALID, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("不支持的数据表"));
    }

    @Test
    void validateAll_withNullTableName_shouldThrowException() {
        // given - tableName为null
        String tableName = null;
        String field = "asset_id";
        String aggregation = "COUNT";
        String filterCondition = null;

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("表名不能为空"));
    }

    @Test
    void validateAll_withEmptyTableName_shouldThrowException() {
        // given - tableName为空
        String tableName = "";
        String field = "asset_id";
        String aggregation = "COUNT";
        String filterCondition = null;

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
    }

    // ========== 字段名校验测试 ==========

    @Test
    void validateAll_withInvalidFieldName_shouldThrowException() {
        // given - 不支持的字段名
        String tableName = "asset";
        String field = "password";
        String aggregation = "COUNT";
        String filterCondition = null;

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.METRIC_CONFIG_INVALID, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("不支持的字段"));
    }

    @Test
    void validateAll_withNullFieldName_shouldThrowException() {
        // given - field为null
        String tableName = "asset";
        String field = null;
        String aggregation = "COUNT";
        String filterCondition = null;

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("字段名不能为空"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASSET_ID", "File_Size_Bytes", "STATUS"})
    void validateAll_withCaseInsensitiveFieldNames_shouldPass(String fieldName) {
        // given - 字段名大小写不敏感
        String tableName = "asset";
        String aggregation = "COUNT";
        String filterCondition = null;

        // when & then
        assertDoesNotThrow(() ->
                FieldValidator.validateAll(tableName, fieldName, aggregation, filterCondition)
        );
    }

    // ========== 聚合方式校验测试 ==========

    @Test
    void validateAll_withInvalidAggregation_shouldThrowException() {
        // given - 不支持的聚合方式
        String tableName = "asset";
        String field = "asset_id";
        String aggregation = "INVALID";
        String filterCondition = null;

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("不支持的聚合方式"));
    }

    @Test
    void validateAll_withNullAggregation_shouldThrowException() {
        // given - aggregation为null
        String tableName = "asset";
        String field = "asset_id";
        String aggregation = null;
        String filterCondition = null;

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("聚合方式不能为空"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"count", "Count", "COUNT"})
    void validateAll_withCaseInsensitiveAggregation_shouldPass(String aggregation) {
        // given - 聚合方式大小写不敏感
        String tableName = "asset";
        String field = "asset_id";
        String filterCondition = null;

        // when & then
        assertDoesNotThrow(() ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
    }

    // ========== 聚合与字段匹配校验测试 ==========

    @Test
    void validateAll_withSumOnNumericField_shouldPass() {
        // given - SUM在数值字段上
        String tableName = "asset";
        String field = "file_size_bytes";
        String aggregation = "SUM";
        String filterCondition = null;

        // when & then
        assertDoesNotThrow(() ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
    }

    @Test
    void validateAll_withAvgOnNumericField_shouldPass() {
        // given - AVG在数值字段上
        String tableName = "asset";
        String field = "duration_seconds";
        String aggregation = "AVG";
        String filterCondition = null;

        // when & then
        assertDoesNotThrow(() ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
    }

    @Test
    void validateAll_withSumOnNonNumericField_shouldThrowException() {
        // given - SUM在非数值字段上
        String tableName = "asset";
        String field = "status";
        String aggregation = "SUM";
        String filterCondition = null;

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.AGGREGATION_FIELD_MISMATCH, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("聚合方式 SUM 仅适用于数值类型字段"));
    }

    @Test
    void validateAll_withAvgOnNonNumericField_shouldThrowException() {
        // given - AVG在非数值字段上
        String tableName = "asset";
        String field = "uploader";
        String aggregation = "AVG";
        String filterCondition = null;

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.AGGREGATION_FIELD_MISMATCH, exception.getErrorCode());
    }

    @Test
    void validateAll_withCountOnNonNumericField_shouldPass() {
        // given - COUNT在任意字段上都可以
        String tableName = "asset";
        String field = "uploader";
        String aggregation = "COUNT";
        String filterCondition = null;

        // when & then
        assertDoesNotThrow(() ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
    }

    // ========== 筛选条件SQL注入防护测试 ==========
    @Test
    void validateAll_withSqlInjectionDrop_shouldThrowException() {
        // given - DROP注入
        String tableName = "asset";
        String field = "asset_id";
        String aggregation = "COUNT";
        String filterCondition = "status='approved'; DROP TABLE asset--";

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("筛选条件包含非法关键字"));
    }

    @Test
    void validateAll_withSqlInjectionUnion_shouldThrowException() {
        // given - UNION注入
        String tableName = "asset";
        String field = "asset_id";
        String aggregation = "COUNT";
        String filterCondition = "status='approved' UNION SELECT * FROM users--";

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
    }

    @Test
    void validateAll_withSqlInjectionDelete_shouldThrowException() {
        // given - DELETE注入
        String tableName = "asset";
        String field = "asset_id";
        String aggregation = "COUNT";
        String filterCondition = "1=1; DELETE FROM asset WHERE 1=1";

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
    }

    @Test
    void validateAll_withDoubleSingleQuote_shouldThrowException() {
        // given - 双单引号注入
        String tableName = "asset";
        String field = "asset_id";
        String aggregation = "COUNT";
        String filterCondition = "uploader='' OR 1=1";

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("筛选条件包含非法字符"));
    }

    @Test
    void validateAll_withValidFilterCondition_shouldPass() {
        // given - 合法的筛选条件
        String tableName = "asset";
        String field = "asset_id";
        String aggregation = "COUNT";
        String filterCondition = "status='approved' AND city='上海'";

        // when & then
        assertDoesNotThrow(() ->
                FieldValidator.validateAll(tableName, field, aggregation, filterCondition)
        );
    }

    // ========== validateForUpdate 部分校验测试 ==========

    @Test
    void validateForUpdate_withOnlyFieldChanged_shouldValidateField() {
        // given - 只更新field
        String newField = "status";
        String newAggregation = null; // 不更新
        FieldValidator.ValidationContext currentConfig =
                new FieldValidator.ValidationContext("asset", "file_size_bytes", "SUM", null);

        // when & then - SUM on status应该失败
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateForUpdate(null, newField, newAggregation, null, currentConfig)
        );
        assertEquals(ErrorCode.AGGREGATION_FIELD_MISMATCH, exception.getErrorCode());
    }

    @Test
    void validateForUpdate_withOnlyAggregationChanged_shouldValidateAggregation() {
        // given - 只更新aggregation为SUM
        String newField = null; // 不更新
        String newAggregation = "SUM";
        FieldValidator.ValidationContext currentConfig =
                new FieldValidator.ValidationContext("asset", "status", "COUNT", null);

        // when & then - SUM on status应该失败
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateForUpdate(null, newField, newAggregation, null, currentConfig)
        );
        assertEquals(ErrorCode.AGGREGATION_FIELD_MISMATCH, exception.getErrorCode());
    }

    @Test
    void validateForUpdate_withNewValidConfig_shouldPass() {
        // given - 更新为合法的配置
        String newField = "file_size_bytes";
        String newAggregation = "SUM";
        FieldValidator.ValidationContext currentConfig =
                new FieldValidator.ValidationContext("asset", "status", "COUNT", null);

        // when & then
        assertDoesNotThrow(() ->
                FieldValidator.validateForUpdate(null, newField, newAggregation, null, currentConfig)
        );
    }

    @Test
    void validateForUpdate_withOnlyTableChanged_shouldValidateTable() {
        // given - 只更新tableName为非法值
        String newTableName = "invalid_table";
        String newField = null;
        String newAggregation = null;
        FieldValidator.ValidationContext currentConfig =
                new FieldValidator.ValidationContext("asset", "asset_id", "COUNT", null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateForUpdate(newTableName, newField, newAggregation, null, currentConfig)
        );
        assertEquals(ErrorCode.METRIC_CONFIG_INVALID, exception.getErrorCode());
    }

    @Test
    void validateForUpdate_withOnlyFilterConditionChanged_shouldValidateFilter() {
        // given - 更新filterCondition包含非法关键字
        String newFilterCondition = "status='approved'; DROP TABLE asset";
        FieldValidator.ValidationContext currentConfig =
                new FieldValidator.ValidationContext("asset", "asset_id", "COUNT", null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                FieldValidator.validateForUpdate(null, null, null, newFilterCondition, currentConfig)
        );
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED, exception.getErrorCode());
    }

    @Test
    void validateForUpdate_withAllNull_shouldPass() {
        // given - 所有更新字段都为null
        FieldValidator.ValidationContext currentConfig =
                new FieldValidator.ValidationContext("asset", "asset_id", "COUNT", null);

        // when & then - 不应该抛异常
        assertDoesNotThrow(() ->
                FieldValidator.validateForUpdate(null, null, null, null, currentConfig)
        );
    }
}


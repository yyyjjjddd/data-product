package com.example.metrics.util;

import com.example.metrics.exception.BusinessException;
import com.example.metrics.exception.ErrorCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 字段校验工具（责任链模式）
 *
 * <p>将校验逻辑拆分为独立的处理器，形成校验链：
 * <ul>
 *   <li>TableNameValidator - 表名校验</li>
 *   <li>FieldValidator.Handler - 字段名校验</li>
 *   <li>AggregationValidator - 聚合方式校验</li>
 *   <li>AggregationFieldMatchValidator - 聚合与字段匹配校验</li>
 * </ul>
 */
public class FieldValidator {

    /** 允许的数据表 */
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(Arrays.asList("asset"));

    /** 允许的字段 */
    private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList(
            "asset_id", "title", "uploader", "uploaded_at", "file_size_bytes",
            "status", "tags", "city", "platform", "duration_seconds"
    ));

    /** 允许的聚合方式 */
    private static final Set<String> ALLOWED_AGGREGATIONS = new HashSet<>(Arrays.asList(
            "COUNT", "SUM", "AVG", "MAX", "MIN"
    ));

    /** 数值类型字段 */
    private static final Set<String> NUMERIC_FIELDS = new HashSet<>(Arrays.asList(
            "file_size_bytes", "duration_seconds"
    ));

    /**
     * 校验上下文，携带校验所需数据
     */
    @Getter
    public static class ValidationContext {
        private String tableName;
        private String field;
        private String aggregation;
        private String filterCondition;

        public ValidationContext(String tableName, String field, String aggregation,
                                 String filterCondition) {
            this.tableName = tableName;
            this.field = field;
            this.aggregation = aggregation;
            this.filterCondition = filterCondition;
        }
    }

    /**
     * 校验处理器接口
     */
    public interface Handler {
        void validate(ValidationContext context);
    }

    /**
     * 校验链
     */
    public static class Chain {
        private final List<Handler> handlers = new ArrayList<>();

        void addHandler(Handler handler) {
            handlers.add(handler);
        }

        public void validate(ValidationContext context) {
            for (Handler handler : handlers) {
                handler.validate(context);
            }
        }
    }

    /**
     * 校验链构建器
     */
    public static class ChainBuilder {
        private final Chain chain = new Chain();

        public ChainBuilder addTableValidator() {
            chain.addHandler(new TableNameHandler());
            return this;
        }

        public ChainBuilder addFieldValidator() {
            chain.addHandler(new FieldHandler());
            return this;
        }

        public ChainBuilder addAggregationValidator() {
            chain.addHandler(new AggregationHandler());
            return this;
        }

        public ChainBuilder addAggregationFieldMatchValidator() {
            chain.addHandler(new AggregationFieldMatchHandler());
            return this;
        }

        public ChainBuilder addFilterConditionValidator() {
            chain.addHandler(new FilterConditionHandler());
            return this;
        }

        public Chain build() {
            return chain;
        }
    }

    /**
     * 表名校验处理器
     */
    private static class TableNameHandler implements Handler {
        @Override
        public void validate(ValidationContext context) {
            String tableName = context.getTableName();
            if (tableName == null || tableName.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "表名不能为空");
            }
            if (!ALLOWED_TABLES.contains(tableName.toLowerCase())) {
                throw new BusinessException(ErrorCode.METRIC_CONFIG_INVALID,
                        "不支持的数据表: " + tableName);
            }
        }
    }

    /**
     * 字段名校验处理器
     */
    private static class FieldHandler implements Handler {
        @Override
        public void validate(ValidationContext context) {
            String field = context.getField();
            if (field == null || field.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "字段名不能为空");
            }
            if (!ALLOWED_FIELDS.contains(field.toLowerCase())) {
                throw new BusinessException(ErrorCode.METRIC_CONFIG_INVALID,
                        "不支持的字段: " + field);
            }
        }
    }

    /**
     * 聚合方式校验处理器
     */
    private static class AggregationHandler implements Handler {
        @Override
        public void validate(ValidationContext context) {
            String aggregation = context.getAggregation();
            if (aggregation == null || aggregation.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "聚合方式不能为空");
            }
            if (!ALLOWED_AGGREGATIONS.contains(aggregation.toUpperCase())) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED,
                        "不支持的聚合方式: " + aggregation);
            }
        }
    }

    /**
     * 聚合与字段匹配校验处理器
     */
    private static class AggregationFieldMatchHandler implements Handler {
        @Override
        public void validate(ValidationContext context) {
            String aggUpper = context.getAggregation().toUpperCase();
            String fieldLower = context.getField().toLowerCase();

            if (("SUM".equals(aggUpper) || "AVG".equals(aggUpper)) &&
                    !NUMERIC_FIELDS.contains(fieldLower)) {
                throw new BusinessException(ErrorCode.AGGREGATION_FIELD_MISMATCH,
                        "聚合方式 " + context.getAggregation() + " 仅适用于数值类型字段（file_size_bytes, duration_seconds）");
            }
        }
    }

    /**
     * 筛选条件校验处理器
     */
    private static class FilterConditionHandler implements Handler {
        @Override
        public void validate(ValidationContext context) {
            String filterCondition = context.getFilterCondition();
            if (filterCondition == null || filterCondition.isEmpty()) {
                return;
            }

            String upperFilter = filterCondition.toUpperCase();

            // 禁止危险关键字
            if (upperFilter.contains(";") || upperFilter.contains("--") ||
                    upperFilter.contains("DROP") || upperFilter.contains("DELETE") ||
                    upperFilter.contains("UPDATE") || upperFilter.contains("INSERT") ||
                    upperFilter.contains("TRUNCATE") || upperFilter.contains("ALTER") ||
                    upperFilter.contains("UNION") || upperFilter.contains("EXEC")) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED,
                        "筛选条件包含非法关键字");
            }

            // 校验值格式：不能有双单引号
            if (filterCondition.contains("''")) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED,
                        "筛选条件包含非法字符");
            }
        }
    }

    /**
     * 一键校验完整的指标配置（使用责任链）
     */
    public static void validateAll(String tableName, String field, String aggregation,
                                   String filterCondition) {
        new ChainBuilder()
                .addTableValidator()
                .addFieldValidator()
                .addAggregationValidator()
                .addAggregationFieldMatchValidator()
                .addFilterConditionValidator()
                .build()
                .validate(new ValidationContext(tableName, field, aggregation, filterCondition));
    }

    /**
     * 部分校验（用于 update 场景）
     *
     * <p>只校验传入的非 null 字段，并校验聚合与字段的匹配性。
     *
     * @param tableName    表名（可为 null，表示不更新）
     * @param field        字段名（可为 null，表示不更新）
     * @param aggregation  聚合方式（可为 null，表示不更新）
     * @param filterCondition 筛选条件（可为 null，表示不更新）
     * @param currentConfig 当前配置（用于获取未更新的字段值）
     */
    public static void validateForUpdate(String tableName, String field, String aggregation,
                                         String filterCondition, ValidationContext currentConfig) {
        Chain chain = new Chain();

        if (tableName != null) {
            chain.addHandler(new TableNameHandler());
        }
        if (field != null) {
            chain.addHandler(new FieldHandler());
        }
        if (aggregation != null) {
            chain.addHandler(new AggregationHandler());
        }
        if (filterCondition != null) {
            chain.addHandler(new FilterConditionHandler());
        }
        // 聚合与字段匹配性：只要有一个更新就需要校验
        if (field != null || aggregation != null) {
            chain.addHandler(new AggregationFieldMatchHandler());
        }

        // 构建完整的上下文（用新值覆盖旧值）
        String fullTable = tableName != null ? tableName : currentConfig.getTableName();
        String fullField = field != null ? field : currentConfig.getField();
        String fullAgg = aggregation != null ? aggregation : currentConfig.getAggregation();
        String fullFilter = filterCondition != null ? filterCondition : currentConfig.getFilterCondition();

        chain.validate(new ValidationContext(fullTable, fullField, fullAgg, fullFilter));
    }
}

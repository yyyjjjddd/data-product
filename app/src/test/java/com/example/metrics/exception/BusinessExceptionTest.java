package com.example.metrics.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BusinessException单元测试
 *
 * <p>测试业务异常：
 * <ul>
 *   <li>正常情况：构造和获取属性</li>
 *   <li>边界条件：错误码、详情为空</li>
 * </ul>
 */
class BusinessExceptionTest {

    @Test
    void constructor_withErrorCodeOnly_shouldCreateException() {
        // given
        ErrorCode errorCode = ErrorCode.METRIC_NOT_FOUND;

        // when
        BusinessException exception = new BusinessException(errorCode);

        // then
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(ErrorCode.METRIC_NOT_FOUND.getCode(), exception.getCode());
        assertEquals(ErrorCode.METRIC_NOT_FOUND.getMessage(), exception.getMessage());
        assertNull(exception.getDetails());
    }

    @Test
    void constructor_withErrorCodeAndDetails_shouldCreateException() {
        // given
        ErrorCode errorCode = ErrorCode.METRIC_NOT_FOUND;
        String details = "id: 123";

        // when
        BusinessException exception = new BusinessException(errorCode, details);

        // then
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(ErrorCode.METRIC_NOT_FOUND.getCode(), exception.getCode());
        assertEquals(ErrorCode.METRIC_NOT_FOUND.getMessage(), exception.getMessage());
        assertEquals(details, exception.getDetails());
    }

    @Test
    void getMessage_shouldReturnErrorCodeMessage() {
        // given
        ErrorCode errorCode = ErrorCode.PARAM_VALIDATION_FAILED;
        BusinessException exception = new BusinessException(errorCode, "field is null");

        // when & then
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED.getMessage(), exception.getMessage());
    }

    @Test
    void allErrorCodes_shouldHaveUniqueCodes() {
        // given
        ErrorCode[] errorCodes = ErrorCode.values();

        // when
        long uniqueCount = java.util.Arrays.stream(errorCodes)
                .map(ErrorCode::getCode)
                .distinct()
                .count();

        // then
        assertEquals(errorCodes.length, uniqueCount, "All error codes should be unique");
    }

    @Test
    void allErrorCodes_shouldHaveValidCodeRanges() {
        // given
        ErrorCode[] errorCodes = ErrorCode.values();

        // when & then
        for (ErrorCode code : errorCodes) {
            int codeValue = code.getCode();
            assertTrue(codeValue >= 40001 && codeValue <= 59999,
                    "Error code " + code + " should be in 4xxxx or 5xxxx range");
        }
    }

    @Test
    void errorCode4xxRange_shouldBeClientErrors() {
        // given
        int[] clientErrorCodes = {
                ErrorCode.PARAM_VALIDATION_FAILED.getCode(),
                ErrorCode.METRIC_NOT_FOUND.getCode(),
                ErrorCode.METRIC_ALREADY_EXISTS.getCode(),
                ErrorCode.METRIC_CONFIG_INVALID.getCode(),
                ErrorCode.AGGREGATION_FIELD_MISMATCH.getCode(),
                ErrorCode.TASK_NOT_FOUND.getCode(),
                ErrorCode.TASK_STILL_RUNNING.getCode(),
                ErrorCode.DUPLICATE_TASK_SUBMISSION.getCode()
        };

        // when & then
        for (int code : clientErrorCodes) {
            assertTrue(code >= 40001 && code < 50000,
                    "Client error code should be in 4xxxx range: " + code);
        }
    }

    @Test
    void errorCode5xxRange_shouldBeServerErrors() {
        // given
        int[] serverErrorCodes = {
                ErrorCode.DATABASE_ERROR.getCode(),
                ErrorCode.MESSAGE_QUEUE_ERROR.getCode(),
                ErrorCode.SYSTEM_ERROR.getCode()
        };

        // when & then
        for (int code : serverErrorCodes) {
            assertTrue(code >= 50001 && code < 60000,
                    "Server error code should be in 5xxxx range: " + code);
        }
    }

    @Test
    void getCode_shouldReturnErrorCodeValue() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.DATABASE_ERROR);

        // when & then
        assertEquals(50001, exception.getCode());
    }

    @Test
    void exceptionWithNullDetails_shouldWork() {
        // given
        ErrorCode errorCode = ErrorCode.TASK_NOT_FOUND;
        String details = null;

        // when
        BusinessException exception = new BusinessException(errorCode, details);

        // then
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getDetails());
    }
}

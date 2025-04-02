package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class CommonUtils {
    private CommonUtils() {
        //util class
    }
    public static BigDecimal calculateVolume(BigDecimal length, BigDecimal width, BigDecimal height){
        return length.multiply(width).multiply(height);
    }

    public static String getDateFromTimeStampString(String date) {
        LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(CommonConstants.BIG_QUERY_DATETIME_FORMAT));
        return localDateTime.format(DateTimeFormatter.ofPattern(CommonConstants.BIG_QUERY_DATE_FORMAT));
    }

    public static String getTimeStampToBqFormat(String date){
        LocalDateTime localDateTime = LocalDateTime.parse(date);
        return localDateTime.format(DateTimeFormatter.ofPattern(CommonConstants.BIG_QUERY_DATETIME_FORMAT));
    }

    public static String getCurrentTimeStampToBqFormat(){
        return LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(CommonConstants.BIG_QUERY_DATETIME_FORMAT));
    }

    public static String getRandomUuid(){
        return UUID.randomUUID().toString();
    }

    public static String getValueOrNull(String value) {
        return StringUtils.isBlank(value)?null:value;
    }
    public static String getPunchTypeHdwFormat(String punchType) {
        return punchType.contains(CommonConstants.CLOCK_IN)?CommonConstants.CLOCK_IN:CommonConstants.CLOCK_OUT;
    }
}

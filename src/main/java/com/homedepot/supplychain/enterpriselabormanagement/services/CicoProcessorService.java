package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;
import com.homedepot.supplychain.enterpriselabormanagement.utils.CommonUtils;
import com.homedepot.supplychain.enterpriselabormanagement.utils.RowMapperUtils;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TransactionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.*;

@Service
@Slf4j
public class CicoProcessorService {

    @Value("${elm.cico-punches-threshold-hours}")
    private Long cicoThresholdHours;
    private final ElmTransactionBigQueryService elmTransactionBigQueryService;

    @Autowired
    public CicoProcessorService(ElmTransactionBigQueryService elmTransactionBigQueryService){
        this.elmTransactionBigQueryService = elmTransactionBigQueryService;
    }

    public void processCicoEvents(HdwTransaction hdwTransaction, BasicAcknowledgeablePubsubMessage originalMessage, String dcNumber) {
        // Update Transaction Timestamp into BQ format
        if (!ObjectUtils.isEmpty(hdwTransaction.getLaborEventDetail().getTransactionTimestamp())) {
            hdwTransaction.getLaborEventDetail().setTransactionTimestamp(
                    CommonUtils.getTimeStampToBqFormat(hdwTransaction.getLaborEventDetail().getTransactionTimestamp()));
        }
        // Update Transaction Local Timestamp into BQ format
        if (!ObjectUtils.isEmpty(hdwTransaction.getLaborEventDetail().getTransactionLocalTimestamp())) {
            hdwTransaction.getLaborEventDetail().setTransactionLocalTimestamp(
                    CommonUtils.getTimeStampToBqFormat(hdwTransaction.getLaborEventDetail().getTransactionLocalTimestamp()));
        }
        // Update PunchType into EM format i.e. CLOCK_IN -> IN/ CLOCK_OUT -> OUT
        hdwTransaction.getLaborEvent().setPunchType(CommonUtils.getPunchTypeHdwFormat(hdwTransaction.getLaborEvent().getActivity()));

        // Extract punchDate from Transaction Local Timestamp and set into HdwTransaction.
        hdwTransaction.getLaborEventDetail().setPunchDate(CommonUtils.getDateFromTimeStampString(hdwTransaction.getLaborEventDetail().getTransactionLocalTimestamp()));
        checkLatestExistingPunch(hdwTransaction.getLaborEventDetail().getPunchDate(), hdwTransaction, dcNumber, originalMessage);
    }

    private void checkLatestExistingPunch(String punchDate, HdwTransaction hdwTransaction, String dcNumber, BasicAcknowledgeablePubsubMessage originalMessage) {
        String punchType = hdwTransaction.getLaborEvent().getPunchType();
        String transactionLocalTimestamp = hdwTransaction.getLaborEventDetail().getTransactionLocalTimestamp();
        TableResult tableResult = elmTransactionBigQueryService.selectQueryJob(hdwTransaction.getLaborEventDetail().getUserId(), punchDate, CICO_PUNCHES_TABLE);
        Iterator<FieldValueList> iterator = tableResult.getValues().iterator();
        String lastPunchType = "";
        String lastAdjustedPunchDate = "";
        String lastPunchLocalTimestamp = "";
        if (iterator.hasNext()) {
            FieldValueList filedValueList = iterator.next();
            lastPunchType = filedValueList.get(ElmTransactionBqHeaders.PUNCH_TYPE).getStringValue();
            lastAdjustedPunchDate = filedValueList.get(ElmTransactionBqHeaders.ADJUSTED_PUNCH_DATE).getStringValue();
            lastPunchLocalTimestamp = filedValueList.get(ElmTransactionBqHeaders.PUNCH_LOCAL_TIME).getStringValue();
            if (punchType.equals(lastPunchType)) {
                log.warn("Unexpected punch type '{}' received for userId: {}, punchDate: {}", punchType, hdwTransaction.getLaborEventDetail().getUserId(), punchDate);
            } else {
                // If received punch is OUT punch and the last IN punch is on the previous day and if the difference b/w IN and OUT is with in threshold, we will consider this punch as an overlapping punch, and will need to adjust the punch date for OUT to the IN punch date
                if (punchType.contains(CLOCK_OUT) && dayDifference(lastAdjustedPunchDate, punchDate) == DAY_1 && isTimeDifferenceLessThanThreshold(lastPunchLocalTimestamp, transactionLocalTimestamp)) {
                    hdwTransaction.getLaborEventDetail().setAdjustPunchDate(lastAdjustedPunchDate);
                }
            }
        }
        List<Map<String, Object>> rows = RowMapperUtils.populatePunchesRowsFromCicoTransactions(hdwTransaction);
        elmTransactionBigQueryService.insertBqStreaming(hdwTransaction, originalMessage, dcNumber, rows);
        if (CLOCK_OUT.equals(punchType) && !CLOCK_OUT.equals(lastPunchType) && !lastPunchLocalTimestamp.isEmpty()) {
            updateCicoSummary(lastPunchLocalTimestamp, lastAdjustedPunchDate, hdwTransaction, dcNumber);
        } else {
            log.info("No need to update CICO summary for userId: {}, punchDate: {}", hdwTransaction.getLaborEventDetail().getUserId(), punchDate);
        }
    }

    private long dayDifference(String lastAdjustedPunchDate, String punchDate) {
        return Duration.between(LocalDate.parse(lastAdjustedPunchDate).atStartOfDay(),LocalDate.parse(punchDate).atStartOfDay()).toDays();
    }

    private void updateCicoSummary(String lastPunchLocalTimestamp, String lastAdjustedPunchDate,HdwTransaction hdwTransaction, String dcNumber) {
        // Calculate total worked hours based on last IN and recent out.
        double workedHours = TransactionUtils.calculateTotalWorkedHours(lastPunchLocalTimestamp, hdwTransaction.getLaborEventDetail().getTransactionLocalTimestamp());
        // For a given adjusted_punch_date checking record already exist or not.
        TableResult tableResult = elmTransactionBigQueryService.selectQueryJob(hdwTransaction.getLaborEventDetail().getUserId(), lastAdjustedPunchDate, CICO_SUMMARY_TABLE);
        Iterator<FieldValueList> iterator = tableResult.getValues().iterator();
        if (iterator.hasNext()) {
            FieldValueList next = iterator.next();
            double totalHoursWorked = next.get(ElmTransactionBqHeaders.TOTAL_HOURS_WORKED).getDoubleValue();
            // Update total hours worked in existing row in summary table.
            elmTransactionBigQueryService.updateCicoSummaryQueryJob(hdwTransaction.getLaborEventDetail().getUserId(), totalHoursWorked + workedHours, lastAdjustedPunchDate);
        } else {
            // Insert new row
            elmTransactionBigQueryService.insertCicoSummaryQueryJob(hdwTransaction, dcNumber, workedHours, lastAdjustedPunchDate);
        }
    }

    private boolean isTimeDifferenceLessThanThreshold(String lastPunchLocalTime, String transactionLocalTimestamp) {
        if(StringUtils.isBlank(lastPunchLocalTime)){
            return false;
        }
        return Duration.between(LocalDateTime.parse(transactionLocalTimestamp), LocalDateTime.parse(lastPunchLocalTime)).toHours() < cicoThresholdHours;
    }
}

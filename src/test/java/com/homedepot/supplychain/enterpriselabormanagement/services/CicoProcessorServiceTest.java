package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.Data;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.LaborEvent;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DateTimeException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
class CicoProcessorServiceTest {

    @Mock
    private ElmTransactionBigQueryService elmTransactionBigQueryService;
    @Mock
    private BasicAcknowledgeablePubsubMessage message;
    @Mock
    private TableResult tableResult;
    @Mock
    FieldValue fieldValue;
    @Mock
    FieldValue fieldValue1;
    @Mock
    FieldValue punchLocalTimeFieldValue;
    @Mock
    FieldValueList fieldValueList;
    @InjectMocks
    CicoProcessorService cicoProcessorService;
    @Value(TestData.TEST_THRESHOLD_HOURS)
    private Long cicoThresholdHours;

    @BeforeEach()
    public void setUp() {
        ReflectionTestUtils.setField(cicoProcessorService, "cicoThresholdHours", cicoThresholdHours);
    }

    @Test()
    @DisplayName("test- successful PubSub cico events same punches consecutively")
    void testCicoProcessWhenSamePunchesTwoTime(){
        List<FieldValueList> listOfFieldValueList = List.of(fieldValueList);
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(CommonConstants.CLOCK_OUT);
        when(elmTransactionBigQueryService.selectQueryJob(anyString(),anyString(), anyString())).thenReturn(tableResult);
        doNothing().when(elmTransactionBigQueryService).insertBqStreaming(any(), any(),anyString(), any());
        when(tableResult.getValues()).thenReturn(listOfFieldValueList);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_TYPE)).thenReturn(fieldValue);
        when(fieldValue.getStringValue()).thenReturn(TestData.TEST_PUNCH_TYPE_OUT);
        when(fieldValueList.get(anyString())).thenReturn(fieldValue);
        Assertions.assertDoesNotThrow(() -> cicoProcessorService.processCicoEvents(hdwTransaction, message, TestData.TEST_DC_NUMBER));
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(0)).insertAll(any(), anyString());
    }

    @Test()
    @DisplayName("test- successful PubSub cico events for first IN punch")
    void testCicoProcessFirstInPunchOfDay(){
        List<FieldValueList> listOfFieldValueList = List.of();
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(CommonConstants.CLOCK_IN);
        when(elmTransactionBigQueryService.selectQueryJob(anyString(),anyString(), anyString())).thenReturn(tableResult);
        when(tableResult.getValues()).thenReturn(listOfFieldValueList);
        Assertions.assertDoesNotThrow(() -> cicoProcessorService.processCicoEvents(hdwTransaction, message, TestData.TEST_DC_NUMBER));
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(0)).insertAll(any(), anyString());
    }

    @Test()
    @DisplayName("test- successful PubSub cico events for Multiple punch out")
    void testCicoProcessMultiplePunchOfDay(){
        List<FieldValueList> listOfFieldValueList = List.of(fieldValueList);
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(CommonConstants.CLOCK_OUT);
        when(elmTransactionBigQueryService.selectQueryJob(anyString(),anyString(), anyString())).thenReturn(tableResult);
        when(tableResult.getValues()).thenReturn(listOfFieldValueList);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_LOCAL_TIME)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_TYPE)).thenReturn(fieldValue1);
        when(fieldValueList.get(ElmTransactionBqHeaders.ADJUSTED_PUNCH_DATE)).thenReturn(fieldValue1);
        when(fieldValueList.get(ElmTransactionBqHeaders.TOTAL_HOURS_WORKED)).thenReturn(fieldValue);
        when(fieldValue.getStringValue()).thenReturn(TestData.TEST_PUNCH_LOCAL_TIMESTAMP);
        when(fieldValue1.getStringValue()).thenReturn(TestData.TEST_PUNCH_DATE);

        Assertions.assertDoesNotThrow(() -> cicoProcessorService.processCicoEvents(hdwTransaction, message, TestData.TEST_DC_NUMBER));
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(0)).insertAll(any(), anyString());
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(0)).insertAll(any(), anyString());
    }

    @Test()
    @DisplayName("test- successful PubSub cico events for out punch")
    void testCicoProcessOUTPunchOfDay() {
        List<FieldValueList> listOfFieldValueList = List.of(fieldValueList);
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(CommonConstants.CLOCK_OUT);
        when(elmTransactionBigQueryService.selectQueryJob(anyString(),anyString(), anyString())).thenReturn(tableResult);
        when(tableResult.getValues()).thenReturn(listOfFieldValueList);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_LOCAL_TIME)).thenReturn(fieldValue1);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_TYPE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.ADJUSTED_PUNCH_DATE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.TOTAL_HOURS_WORKED)).thenReturn(fieldValue);
        when(fieldValue1.getStringValue()).thenReturn(TestData.TEST_LAST_PUNCH_LOCAL_TIMESTAMP);
        when(fieldValue.getStringValue()).thenReturn(TestData.TEST_LAST_EVENT_PUNCH_DATE);
        Assertions.assertDoesNotThrow(() -> cicoProcessorService.processCicoEvents(hdwTransaction, message, TestData.TEST_DC_NUMBER));
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(2)).selectQueryJob(anyString(), anyString(), anyString());
    }

    @Test()
    @DisplayName("test- successful PubSub cico events for first out punch")
    void testCicoProcessOUTFirstPunchOfDay() {
        List<FieldValueList> listOfFieldValueList = List.of(fieldValueList);
        List<FieldValueList> listOfFieldValueListEmptyResult = List.of();
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(CommonConstants.CLOCK_OUT);
        when(elmTransactionBigQueryService.selectQueryJob(anyString(), anyString(),anyString())).thenReturn(tableResult);
        //noinspection unchecked
        when(tableResult.getValues()).thenReturn(listOfFieldValueList, listOfFieldValueListEmptyResult);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_LOCAL_TIME)).thenReturn(fieldValue1);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_TYPE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.ADJUSTED_PUNCH_DATE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.TOTAL_HOURS_WORKED)).thenReturn(fieldValue);
        when(fieldValue1.getStringValue()).thenReturn(TestData.TEST_PUNCH_LOCAL_TIMESTAMP);
        when(fieldValue.getStringValue()).thenReturn(TestData.TEST_LAST_EVENT_PUNCH_DATE);
        Assertions.assertDoesNotThrow(() -> cicoProcessorService.processCicoEvents(hdwTransaction, message, TestData.TEST_DC_NUMBER));
        //Verify that if selectQueryJob method invoked times
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(2)).selectQueryJob(anyString(), anyString(), anyString());
    }

    @Test()
    @DisplayName("test- Process cico events for first punch out when difference is greater than threshold value")
    void testCicoProcessOUTFirstPunchOutDifferenceGreaterThanThresholdValue() {
        List<FieldValueList> listOfFieldValueList = List.of(fieldValueList);
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(CommonConstants.CLOCK_OUT);
        when(elmTransactionBigQueryService.selectQueryJob(anyString(), anyString(),anyString())).thenReturn(tableResult);
        when(tableResult.getValues()).thenReturn(listOfFieldValueList);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_LOCAL_TIME)).thenReturn(fieldValue1);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_TYPE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.ADJUSTED_PUNCH_DATE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.TOTAL_HOURS_WORKED)).thenReturn(fieldValue);
        when(fieldValue1.getStringValue()).thenReturn(TestData.TEST_PUNCH_LOCAL_TIMESTAMP);
        when(fieldValue.getStringValue()).thenReturn(TestData.TEST_LAST_EVENT_PUNCH_DATE);
        Assertions.assertDoesNotThrow(() -> cicoProcessorService.processCicoEvents(hdwTransaction, message, TestData.TEST_DC_NUMBER));
        //Verify that if selectQueryJob method invoked two times
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(2)).selectQueryJob(anyString(), anyString(), anyString());
    }

    @Test()
    @DisplayName("test- Process cico events for multiple In punch for day")
    void testCicoProcessMultipleInPunchOfDay() {
        List<FieldValueList> listOfFieldValueList = List.of(fieldValueList);
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(CommonConstants.CLOCK_IN);
        when(elmTransactionBigQueryService.selectQueryJob(anyString(), anyString(),anyString())).thenReturn(tableResult);
        when(tableResult.getValues()).thenReturn(listOfFieldValueList);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_LOCAL_TIME)).thenReturn(fieldValue1);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_TYPE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.ADJUSTED_PUNCH_DATE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.TOTAL_HOURS_WORKED)).thenReturn(fieldValue);
        when(fieldValue1.getStringValue()).thenReturn(TestData.TEST_PUNCH_LOCAL_TIMESTAMP);
        when(fieldValue.getStringValue()).thenReturn(TestData.TEST_LAST_EVENT_PUNCH_DATE);
        Assertions.assertDoesNotThrow(() -> cicoProcessorService.processCicoEvents(hdwTransaction, message, TestData.TEST_DC_NUMBER));
        //Verify that if selectQueryJob method invoked once
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(1)).selectQueryJob(anyString(), anyString(), anyString());
    }

    @Test()
    @DisplayName("test- PubSub cico events when transaction timestamp is Invalid then throw DateTimeException")
    void testCicoProcessTransactionTimestampEmptyThrowDateTimeException() {
        HdwTransaction hdwTransaction = mock(HdwTransaction.class);
        Data laborEventDetail = mock(Data.class);
        LaborEvent laborEvent = mock(LaborEvent.class);
        when(hdwTransaction.getLaborEventDetail()).thenReturn(laborEventDetail);
        when(hdwTransaction.getLaborEvent()).thenReturn(laborEvent);
        when(laborEventDetail.getTransactionLocalTimestamp()).thenReturn("");
        when(laborEventDetail.getTransactionTimestamp()).thenReturn("");
        when(laborEventDetail.getPunchDate()).thenReturn(TestData.TEST_PUNCH_DATE);
        when(laborEvent.getActivity()).thenReturn(TestData.TEST_PUNCH_DATE);
        //DateTimeException throw when invalid timestamp
        Assertions.assertThrows(DateTimeException.class,() -> cicoProcessorService.processCicoEvents(hdwTransaction, message, TestData.TEST_DC_NUMBER));
    }

    @Test()
    @DisplayName("test- Process cico events for multiple In punch for day")
    void testCicoProcessWhenLastPunchTimeEmpty() {
        List<FieldValueList> listOfFieldValueList = List.of(fieldValueList);
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(CommonConstants.CLOCK_OUT);
        when(elmTransactionBigQueryService.selectQueryJob(anyString(), anyString(),anyString())).thenReturn(tableResult);
        when(tableResult.getValues()).thenReturn(listOfFieldValueList);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_LOCAL_TIME)).thenReturn(punchLocalTimeFieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.PUNCH_TYPE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.ADJUSTED_PUNCH_DATE)).thenReturn(fieldValue);
        when(fieldValueList.get(ElmTransactionBqHeaders.TOTAL_HOURS_WORKED)).thenReturn(fieldValue);
        when(fieldValue1.getStringValue()).thenReturn(TestData.TEST_PUNCH_LOCAL_TIMESTAMP);
        when(punchLocalTimeFieldValue.getStringValue()).thenReturn("");
        when(fieldValue.getStringValue()).thenReturn(TestData.TEST_LAST_EVENT_PUNCH_DATE);
        Assertions.assertDoesNotThrow(() -> cicoProcessorService.processCicoEvents(hdwTransaction, message, TestData.TEST_DC_NUMBER));
        //Verify that if selectQueryJob method invoked once
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(1)).selectQueryJob(anyString(), anyString(), anyString());
    }

}
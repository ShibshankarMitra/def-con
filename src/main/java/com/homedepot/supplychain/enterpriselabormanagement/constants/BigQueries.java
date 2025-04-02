package com.homedepot.supplychain.enterpriselabormanagement.constants;

public final class BigQueries {
   public static final String ELM_EVENTS_VIEW_QUERY_BY_TRACE_ID_AND_DC_NUMBER ="SELECT trace_id FROM `%s.%s.%s` WHERE trace_id='%s' and dc_number='%s' LIMIT 1";
   public static final String CICO_PUNCHES_QUERY_BY_TRACE_ID_AND_DC_NUMBER ="SELECT trace_id FROM `%s.%s.%s` WHERE trace_id='%s' and dc_number='%s' LIMIT 1";
   public static final String CICO_PUNCHES_QUERY_BY_USER_ID_AND_PUNCH_DATE ="SELECT punch_local_time, punch_type, punch_date, adjusted_punch_date FROM `%s.%s.%s` WHERE user_id='%s' and punch_date between DATE_SUB(DATE('%5$s'), INTERVAL 1 DAY) AND DATE('%5$s') order by bq_create_dttm desc";
   public static final String CICO_SUMMARY_QUERY_BY_USER_ID_AND_PUNCH_DATE ="SELECT total_hours_worked FROM `%s.%s.%s` WHERE user_id='%s' and punch_date='%s' order by bq_create_dttm desc LIMIT 1";
   public static final String CICO_SUMMARY_UPDATE_TOTAL_HOURS_WORKED ="UPDATE `%s.%s.%s` set total_hours_worked=%s, bq_update_dttm='%s' where user_id='%s' and punch_date='%s'";
   public static final String CICO_SUMMARY_INSERT ="INSERT INTO `%s.%s.%s` (user_id, user_name, punch_date, total_hours_worked, dc_number, bq_create_dttm, bq_update_dttm ) VALUES ('%s','%s','%s',%f,'%s','%s','%s')";
   private BigQueries(){
      //Constant class
   }
}
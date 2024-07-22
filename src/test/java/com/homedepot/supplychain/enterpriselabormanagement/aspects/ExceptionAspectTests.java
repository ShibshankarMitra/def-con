package com.homedepot.supplychain.enterpriselabormanagement.aspects;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmSystemException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@ExtendWith(SpringExtension.class)
class ExceptionAspectTests {
    @InjectMocks
    ExceptionAspect exceptionAspect;
    private ListAppender<ILoggingEvent> listAppender;

    private static final Logger logger= (Logger) LoggerFactory.getLogger(ExceptionAspect.class);

    @BeforeEach
    public void setUp() {
        //start the ListAppender
        listAppender = new ListAppender<>();
        listAppender.start();
        // add the appender to the logger
        // addAppender is outdated now
        logger.addAppender(listAppender);
    }

    @Test
    void testInterceptForElmBusinessException() {
        //Execute interceptor
        exceptionAspect.intercept(new ElmBusinessException("message_id", "error_message", new RuntimeException()));
        //Capture the Logs in the List
        List<ILoggingEvent> logsList = listAppender.list;

        //Assertions related to Logging Level and Logged Data
        Assertions.assertEquals(Level.ERROR, logsList.get(0)
                .getLevel());
        Assertions.assertTrue(logsList.get(0).getMessage().contains("PubSub_MessageID"));
        listAppender.stop();
    }

    @Test
    void testInterceptForElmSystemException() {
        //Execute interceptor
        exceptionAspect.intercept(new ElmSystemException("source_name", "message", new RuntimeException()));
        //Capture the Logs in the List
        List<ILoggingEvent> logsList = listAppender.list;

        //Assertions related to Logging Level and Logged Data
        Assertions.assertEquals(Level.ERROR, logsList.get(0)
                .getLevel());
        Assertions.assertTrue(logsList.get(0).getMessage().contains("Source"));
        listAppender.stop();
    }

    @Test
    void testInterceptForRuntimeException() {
        //Execute interceptor
        exceptionAspect.intercept(new RuntimeException("Message"));
        //Capture the Logs in the List
        List<ILoggingEvent> logsList = listAppender.list;

        //Assertions related to Logging Level and Logged Data
        Assertions.assertEquals(Level.ERROR, logsList.get(0)
                .getLevel());
        listAppender.stop();
    }
}
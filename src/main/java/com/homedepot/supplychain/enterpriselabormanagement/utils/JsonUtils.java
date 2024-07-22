package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homedepot.supplychain.enterpriselabormanagement.EnterpriseLaborManagementApplication;
import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ErrorMessages;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.JsonValidationException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Set;

public final class JsonUtils {
    private JsonUtils() {
        //util class
    }

    public static Object validateAndReadJsonMessage(String messageBody, String schemaFileName, Class<?> type) throws JsonProcessingException {
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(
                EnterpriseLaborManagementApplication.class.getResourceAsStream(CommonConstants.FILE_PATH_DELIMITER +
                        CommonConstants.JSON_SCHEMA_DIRECTORY+
                        CommonConstants.FILE_PATH_DELIMITER+
                        schemaFileName));
        Set<ValidationMessage> validationMessages = schema.validate(new ObjectMapper().readTree(messageBody));
        if (!ObjectUtils.isEmpty(validationMessages)) {
            throw new JsonValidationException(ErrorMessages.JSON_SCHEMA_VALIDATION_FAILED + validationMessages);
        } else {
            return new ObjectMapper().readValue(messageBody, type);
        }
    }

    public static String readJsonNode(String messageBody, String headerPath) throws JsonProcessingException {
        return new ObjectMapper().readTree(messageBody).at(headerPath).asText();
    }
}

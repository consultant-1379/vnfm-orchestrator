/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.vnfm.orchestrator.utils;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.utils.Utility.UNEXPECTED_EXCEPTION_OCCURRED;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

public class ErrorParser {

    private static final String STATUS = "status";
    private static final String DETAIL = "detail";
    private static final String WFS_ERROR_DETAILS = "errorDetails";
    private static final String TITLE = "title";
    private static final String INSTANCE = "instance";
    private static final String TYPE = "type";

    private final JSONObject errorObject;

    public ErrorParser(String error) {
        this.errorObject = error != null
                ? convertErrorIntoJSONFormat(error)
                : new JSONObject();
    }

    private JSONObject convertErrorIntoJSONFormat(String error) {
        int leftBracketIndex = error.indexOf("{");
        int rightBracketIndex = error.lastIndexOf("}");

        // error in JSON format
        if (leftBracketIndex > -1 && rightBracketIndex > -1) {
            return error.startsWith("{") && error.endsWith("}")
                    ? new JSONObject(error)
                    : new JSONObject(error.substring(leftBracketIndex, rightBracketIndex + 1));
        }

        // error in String format
        if (!error.startsWith("{") && !error.endsWith("}")) {
            JSONObject jsonError = new JSONObject();
            return jsonError.put(DETAIL, error);
        }

        return new JSONObject(error);
    }

    public int getStatus() {
        try {
            return errorObject.isNull(STATUS)
                    ? BAD_REQUEST.value()
                    : errorObject.getInt(STATUS);
        } catch (JSONException e) { // NOSONAR
            return Arrays.stream(HttpStatus.values())
                    .filter(s -> errorObject.getString(STATUS).equals(s.toString()))
                    .map(HttpStatus::value)
                    .findFirst()
                    .orElse(BAD_REQUEST.value());
        }
    }

    public String getDetails() {
        try {
            if (errorObject.isNull(DETAIL) && errorObject.isNull(WFS_ERROR_DETAILS)) {
                return UNEXPECTED_EXCEPTION_OCCURRED;
            } else if (errorObject.isNull(DETAIL)) {
                return errorObject.get(WFS_ERROR_DETAILS).toString();
            } else {
                return errorObject.getString(DETAIL);
            }
        } catch (JSONException e) { // NOSONAR
            return UNEXPECTED_EXCEPTION_OCCURRED;
        }
    }

    public String getTitle() {
        if (errorObject.isNull(TITLE)) {
            var status = HttpStatus.resolve(getStatus());
            return status != null
                    ? status.getReasonPhrase()
                    : StringUtils.EMPTY;
        }
        return errorObject.getString(TITLE);
    }

    public String getInstance() {
        if (!errorObject.isNull(INSTANCE)) {
            return errorObject.getString(INSTANCE);
        }
        return null;
    }

    public String getType() {
        if (!errorObject.isNull(TYPE)) {
            return errorObject.getString(TYPE);
        }
        return TYPE_BLANK;
    }
}

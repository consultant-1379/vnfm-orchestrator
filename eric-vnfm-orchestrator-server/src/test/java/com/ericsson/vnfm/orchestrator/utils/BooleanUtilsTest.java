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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class BooleanUtilsTest {

    @Test
    public void testParsingBooleanStringValues() {
        assertThat(BooleanUtils.getBooleanValue("true")).isTrue();
    }

    @Test
    public void testParsingBooleanPrimitiveValues() {
        assertThat(BooleanUtils.getBooleanValue(true)).isTrue();
    }

    @Test
    public void testParsingBooleanObjectValues() {
        assertThat(BooleanUtils.getBooleanValue(Boolean.valueOf(true))).isTrue();
    }

    @Test
    public void testExceptionForNonBooleanStringValues() {
        assertThatThrownBy(() -> BooleanUtils.getBooleanValue("text")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testExceptionForNonBooleanValues() {
        assertThatThrownBy(() -> BooleanUtils.getBooleanValue(new Object())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testParsingNonBooleanValuesReturnsDefault() {
        assertThat(BooleanUtils.getBooleanValue(new Object(), true)).isTrue();
    }
}

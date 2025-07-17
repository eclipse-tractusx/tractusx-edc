/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.edc.monitor.logger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LoggerMonitorTest {

    private Monitor sut;

    private ListAppender listAppender;

    private static final String MESSAGE = "This is a message.";

    @BeforeEach
    void setUp() {
        var context = (LoggerContext) LogManager.getContext(false);
        var config = context.getConfiguration();
        listAppender = config.getAppender("LIST");
        sut = new Log4j2Monitor();
    }

    @AfterEach
    void tearDown() {
        listAppender.clear();
    }

    @ParameterizedTest
    @ArgumentsSource(ProvideLogDataWithErrors.class)
    void debug(Throwable... throwables) {
        sut.debug(MESSAGE, throwables);
        assertEventWithErrors(listAppender.getEvents(), Level.DEBUG, MESSAGE, throwables);
    }

    @ParameterizedTest
    @ArgumentsSource(ProvideLogDataWithErrors.class)
    void info(Throwable... throwables) {
        sut.info(MESSAGE, throwables);
        assertEventWithErrors(listAppender.getEvents(), Level.INFO, MESSAGE, throwables);
    }

    @ParameterizedTest
    @ArgumentsSource(ProvideLogDataWithErrors.class)
    void warning(Throwable... throwables) {
        sut.warning(MESSAGE, throwables);
        assertEventWithErrors(listAppender.getEvents(), Level.WARN, MESSAGE, throwables);
    }

    @ParameterizedTest
    @ArgumentsSource(ProvideLogDataWithErrors.class)
    void severe(Throwable... throwables) {
        sut.severe(MESSAGE, throwables);
        assertEventWithErrors(listAppender.getEvents(), Level.ERROR, MESSAGE, throwables);
    }

    @Test
    void sanitization() {
        sut.info("This is a message.\r\nThis is another message.");
        var events = listAppender.getEvents();
        assertThat(events).first()
                .extracting("message").asString().isEqualTo("This is a message.  This is another message.");
    }

    private void assertEventWithErrors(List<LogEvent> events, Level level, String message, Throwable... throwables) {
        assertThat(events).hasSize(throwables.length);
        events.forEach(event -> {
            assertThat(event).extracting("level", "message.message").containsExactly(level, message);
            assertThat(event).extracting("thrown").isIn(Arrays.stream(throwables).toList());
        });
    }

    private static class ProvideLogDataWithErrors implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            return Stream.of(
                    Arguments.of((Object) new Throwable[]{ new EdcException("Test") }),
                    Arguments.of((Object) new Throwable[]{ new EdcException("Test"), new RuntimeException() })
            );
        }
    }

}

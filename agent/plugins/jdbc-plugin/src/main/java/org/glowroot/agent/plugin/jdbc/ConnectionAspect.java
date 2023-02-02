/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.agent.plugin.jdbc;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.BooleanProperty;
import org.glowroot.agent.plugin.api.config.ConfigService;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.jdbc.StatementAspect.HasStatementMirrorMixin;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ConnectionAspect {

    private static final ConfigService configService = Agent.getConfigService("jdbc");

    private static final BooleanProperty capturePreparedStatementCreation =
            configService.getBooleanProperty("capturePreparedStatementCreation");
    private static final BooleanProperty captureConnectionClose =
            configService.getBooleanProperty("captureConnectionClose");
    private static final BooleanProperty captureConnectionLifecycleTraceEntries =
            configService.getBooleanProperty("captureConnectionLifecycleTraceEntries");
    private static final BooleanProperty captureTransactionLifecycleTraceEntries =
            configService.getBooleanProperty("captureTransactionLifecycleTraceEntries");

    // ===================== Statement Preparation =====================

    // capture the sql used to create the PreparedStatement
    public static class PrepareAdvice {
        private static final TimerName timerName = Agent.getTimerName(PrepareAdvice.class);
        public static Timer onBefore(ThreadContext context) {
            if (capturePreparedStatementCreation.value()) {
                return context.startTimer(timerName);
            } else {
                return null;
            }
        }
        public static void onReturn(Object preparedStatement,
                String sql) {
            if (sql == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            if (preparedStatement instanceof HasStatementMirrorMixin) {
                ((HasStatementMirrorMixin) preparedStatement).glowroot$setStatementMirror(new PreparedStatementMirror(sql));
            }
        }
        public static void onAfter(Timer timer) {
            if (timer != null) {
                timer.stop();
            }
        }
    }

    public static class CreateStatementAdvice {
        public static void onReturn(Object statement) {
            if (statement instanceof HasStatementMirrorMixin) {
                ((HasStatementMirrorMixin) statement).glowroot$setStatementMirror(new StatementMirror());
            }
        }
    }

    public static class CommitAdvice {
        private static final TimerName timerName = Agent.getTimerName(CommitAdvice.class);
        public static TraceEntry onBefore(ThreadContext context) {
            return context.startTraceEntry(MessageSupplier.create("jdbc commit"), timerName);
        }
        public static void onReturn(TraceEntry traceEntry) {
            traceEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        public static void onThrow(Throwable t,
                TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }

    public static class RollbackAdvice {
        private static final TimerName timerName = Agent.getTimerName(RollbackAdvice.class);
        public static TraceEntry onBefore(ThreadContext context) {
            return context.startTraceEntry(MessageSupplier.create("jdbc rollback"), timerName);
        }
        public static void onReturn(TraceEntry traceEntry) {
            traceEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        public static void onThrow(Throwable t,
                TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }

    public static class CloseAdvice {
        private static final TimerName timerName = Agent.getTimerName(CloseAdvice.class);
        public static boolean isEnabled() {
            return captureConnectionClose.value() || captureConnectionLifecycleTraceEntries.value();
        }
        public static Object onBefore(ThreadContext context) {
            if (captureConnectionLifecycleTraceEntries.value()) {
                return context.startTraceEntry(MessageSupplier.create("jdbc connection close"),
                        timerName);
            } else {
                return context.startTimer(timerName);
            }
        }
        public static void onReturn(Object entryOrTimer) {
            if (entryOrTimer instanceof TraceEntry) {
                ((TraceEntry) entryOrTimer).endWithLocationStackTrace(
                        JdbcPluginProperties.stackTraceThresholdMillis(), MILLISECONDS);
            } else {
                ((Timer) entryOrTimer).stop();
            }
        }
        public static void onThrow(Throwable t, Object entryOrTimer) {
            if (entryOrTimer instanceof TraceEntry) {
                ((TraceEntry) entryOrTimer).endWithError(t);
            } else {
                ((Timer) entryOrTimer).stop();
            }
        }
    }

    public static class SetAutoCommitAdvice {
        private static final TimerName timerName = Agent.getTimerName(SetAutoCommitAdvice.class);
        public static boolean isEnabled() {
            return captureTransactionLifecycleTraceEntries.value();
        }
        public static TraceEntry onBefore(ThreadContext context,
                boolean autoCommit) {
            return context.startTraceEntry(
                    MessageSupplier.create("jdbc set autocommit: {}", Boolean.toString(autoCommit)),
                    timerName);
        }
        public static void onReturn(TraceEntry traceEntry) {
            traceEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        public static void onThrow(Throwable t,
                TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }
}

/*
 * Copyright 2011-2019 the original author or authors.
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

import java.util.List;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.QueryEntry;
import org.glowroot.agent.plugin.api.QueryMessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.checker.NonNull;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.BooleanProperty;
import org.glowroot.agent.plugin.api.config.ConfigListener;
import org.glowroot.agent.plugin.api.config.ConfigService;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;
import org.glowroot.agent.plugin.jdbc.PreparedStatementMirror.ByteArrayParameterValue;
import org.glowroot.agent.plugin.jdbc.PreparedStatementMirror.StreamingParameterValue;
import org.glowroot.agent.plugin.jdbc.message.BatchPreparedStatementMessageSupplier;
import org.glowroot.agent.plugin.jdbc.message.BatchPreparedStatementMessageSupplier2;
import org.glowroot.agent.plugin.jdbc.message.PreparedStatementMessageSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

// many of the pointcuts are not restricted to configService.isEnabled() because StatementMirrors
// must be tracked for their entire life
public class StatementAspect {

    private static final String QUERY_TYPE = "SQL";

    private static final ConfigService configService = Agent.getConfigService("jdbc");

    private static final BooleanProperty captureStatementClose =
            configService.getBooleanProperty("captureStatementClose");

    private static boolean captureBindParameters;

    static {
        configService.registerConfigListener(new ConfigListener() {
            @Override
            public void onChange() {
                captureBindParameters = !configService
                        .getListProperty("captureBindParametersIncludes").value().isEmpty();
            }
        });
    }

    public interface PreparedStatement {}

    // ===================== Mixin =====================

    // the field and method names are verbose since they will be mixed in to existing classes
    public static class HasStatementMirrorImpl implements HasStatementMirrorMixin {

        // does not need to be volatile, app/framework must provide visibility of Statements and
        // ResultSets if used across threads and this can piggyback
        private transient StatementMirror glowroot$statementMirror;

        @Override
        public StatementMirror glowroot$getStatementMirror() {
            return glowroot$statementMirror;
        }

        @Override
        public void glowroot$setStatementMirror(StatementMirror statementMirror) {
            glowroot$statementMirror = statementMirror;
        }

        @Override
        public boolean glowroot$hasStatementMirror() {
            return glowroot$statementMirror != null;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface HasStatementMirrorMixin {

        StatementMirror glowroot$getStatementMirror();

        void glowroot$setStatementMirror(StatementMirror statementMirror);

        boolean glowroot$hasStatementMirror();
    }

    // ================= Parameter Binding =================

    public static class SetXAdvice {
        public static boolean isEnabled() {
            return captureBindParameters;
        }
        public static void onReturn(HasStatementMirrorMixin preparedStatement,
                int parameterIndex, Object x) {
            PreparedStatementMirror mirror =
                    (PreparedStatementMirror) preparedStatement.glowroot$getStatementMirror();
            if (mirror != null) {
                mirror.setParameterValue(parameterIndex, x);
            }
        }
    }

    public static class SetStreamAdvice {
        public static boolean isEnabled() {
            return captureBindParameters;
        }
        public static void onReturn(HasStatementMirrorMixin preparedStatement,
                int parameterIndex, Object x) {
            PreparedStatementMirror mirror =
                    (PreparedStatementMirror) preparedStatement.glowroot$getStatementMirror();
            if (mirror != null) {
                if (x == null) {
                    mirror.setParameterValue(parameterIndex, null);
                } else {
                    mirror.setParameterValue(parameterIndex,
                            new StreamingParameterValue(x.getClass()));
                }
            }
        }
    }

    public static class SetBytesAdvice {
        public static boolean isEnabled() {
            return captureBindParameters;
        }
        public static void onReturn(HasStatementMirrorMixin preparedStatement,
                int parameterIndex, byte /*@Nullable*/ [] x) {
            PreparedStatementMirror mirror =
                    (PreparedStatementMirror) preparedStatement.glowroot$getStatementMirror();
            if (mirror != null) {
                if (x == null) {
                    mirror.setParameterValue(parameterIndex, null);
                } else {
                    setBytes(mirror, parameterIndex, x);
                }
            }
        }
        private static void setBytes(PreparedStatementMirror mirror, int parameterIndex, byte[] x) {
            boolean displayAsHex = JdbcPluginProperties.displayBinaryParameterAsHex(mirror.getSql(),
                    parameterIndex);
            mirror.setParameterValue(parameterIndex, new ByteArrayParameterValue(x, displayAsHex));
        }
    }

    public static class SetObjectAdvice {
        public static boolean isEnabled() {
            return captureBindParameters;
        }
        public static void onReturn(HasStatementMirrorMixin preparedStatement,
                int parameterIndex, Object x) {
            PreparedStatementMirror mirror =
                    (PreparedStatementMirror) preparedStatement.glowroot$getStatementMirror();
            if (mirror != null) {
                if (x == null) {
                    mirror.setParameterValue(parameterIndex, null);
                } else if (x instanceof byte[]) {
                    SetBytesAdvice.setBytes(mirror, parameterIndex, (byte[]) x);
                } else {
                    mirror.setParameterValue(parameterIndex, x);
                }
            }
        }
    }

    public static class SetNullAdvice {
        public static boolean isEnabled() {
            return captureBindParameters;
        }
        public static void onReturn(HasStatementMirrorMixin preparedStatement,
                int parameterIndex) {
            PreparedStatementMirror mirror =
                    (PreparedStatementMirror) preparedStatement.glowroot$getStatementMirror();
            if (mirror != null) {
                mirror.setParameterValue(parameterIndex, null);
            }
        }
    }

    public static class ClearParametersAdvice {
        public static boolean isEnabled() {
            return captureBindParameters;
        }
        public static void onReturn(HasStatementMirrorMixin preparedStatement) {
            PreparedStatementMirror mirror =
                    (PreparedStatementMirror) preparedStatement.glowroot$getStatementMirror();
            if (mirror != null) {
                mirror.clearParameters();
            }
        }
    }

    // ================== Statement Batching ==================

    public static class StatementAddBatchAdvice {
        public static void onReturn(HasStatementMirrorMixin statement,
                String sql) {
            if (sql == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            StatementMirror mirror = statement.glowroot$getStatementMirror();
            if (mirror != null) {
                mirror.addBatch(sql);
            }
        }
    }

    public static class PreparedStatementAddBatchAdvice {
        public static void onReturn(HasStatementMirrorMixin preparedStatement) {
            PreparedStatementMirror mirror =
                    (PreparedStatementMirror) preparedStatement.glowroot$getStatementMirror();
            if (mirror != null) {
                mirror.addBatch();
            }
        }
    }

    // Statement.clearBatch() can be used to re-initiate a prepared statement
    // that has been cached from a previous usage
    public static class ClearBatchAdvice {
        public static void onReturn(HasStatementMirrorMixin statement) {
            StatementMirror mirror = statement.glowroot$getStatementMirror();
            if (mirror != null) {
                mirror.clearBatch();
            }
        }
    }

    // =================== Statement Execution ===================

    public static class StatementExecuteAdvice {
        private static final TimerName timerName = Agent.getTimerName(StatementExecuteAdvice.class);
        public static boolean isEnabled(HasStatementMirrorMixin statement) {
            return statement.glowroot$hasStatementMirror();
        }
        public static QueryEntry onBefore(ThreadContext context,
                HasStatementMirrorMixin statement,
                String sql) {
            if (sql == null) {
                // seems nothing sensible to do here other than ignore
                return null;
            }
            StatementMirror mirror = statement.glowroot$getStatementMirror();
            if (mirror == null) {
                // this shouldn't happen since just checked hasGlowrootStatementMirror() above
                return null;
            }
            QueryEntry query = context.startQueryEntry(QUERY_TYPE, sql,
                    QueryMessageSupplier.create("jdbc query: "), timerName);
            mirror.setLastQueryEntry(query);
            return query;
        }
        public static void onReturn(QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithLocationStackTrace(
                        JdbcPluginProperties.stackTraceThresholdMillis(), MILLISECONDS);
            }
        }
        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithError(t);
            }
        }
    }

    public static class StatementExecuteQueryAdvice {
        public static boolean isEnabled(HasStatementMirrorMixin statement) {
            return statement.glowroot$hasStatementMirror();
        }
        public static QueryEntry onBefore(ThreadContext context,
                HasStatementMirrorMixin statement,
                String sql) {
            return StatementExecuteAdvice.onBefore(context, statement, sql);
        }
        public static void onReturn(HasStatementMirrorMixin resultSet,
                HasStatementMirrorMixin statement,
                QueryEntry queryEntry) {
            // Statement can always be retrieved from ResultSet.getStatement(), and
            // StatementMirror from that, but ResultSet.getStatement() is sometimes not super
            // duper fast due to ResultSet wrapping and other checks, so StatementMirror is
            // stored directly in ResultSet as an optimization
            if (resultSet != null) {
                StatementMirror mirror = statement.glowroot$getStatementMirror();
                resultSet.glowroot$setStatementMirror(mirror);
            }
            if (queryEntry != null) {
                queryEntry.endWithLocationStackTrace(
                        JdbcPluginProperties.stackTraceThresholdMillis(), MILLISECONDS);
            }
        }
        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithError(t);
            }
        }
    }

    public static class StatementExecuteUpdateAdvice {
        public static boolean isEnabled(HasStatementMirrorMixin statement) {
            return statement.glowroot$hasStatementMirror();
        }
        public static QueryEntry onBefore(ThreadContext context,
                HasStatementMirrorMixin statement,
                String sql) {
            return StatementExecuteAdvice.onBefore(context, statement, sql);
        }
        public static void onReturn(int rowCount,
                QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.setCurrRow(rowCount);
                queryEntry.endWithLocationStackTrace(
                        JdbcPluginProperties.stackTraceThresholdMillis(), MILLISECONDS);
            }
        }
        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithError(t);
            }
        }
    }

    public static class PreparedStatementExecuteAdvice {
        private static final TimerName timerName =
                Agent.getTimerName(PreparedStatementExecuteAdvice.class);
        public static boolean isEnabled(HasStatementMirrorMixin preparedStatement) {
            return preparedStatement.glowroot$hasStatementMirror();
        }
        public static QueryEntry onBefore(ThreadContext context,
                HasStatementMirrorMixin preparedStatement) {
            @SuppressWarnings("nullness") // just checked above in isEnabled()
            @NonNull
            PreparedStatementMirror mirror =
                    (PreparedStatementMirror) preparedStatement.glowroot$getStatementMirror();
            QueryMessageSupplier queryMessageSupplier;
            String queryText = mirror.getSql();
            if (captureBindParameters) {
                queryMessageSupplier =
                        new PreparedStatementMessageSupplier(mirror.getParameters(), queryText);
            } else {
                queryMessageSupplier = QueryMessageSupplier.create("jdbc query: ");
            }
            QueryEntry queryEntry =
                    context.startQueryEntry(QUERY_TYPE, queryText, queryMessageSupplier, timerName);
            mirror.setLastQueryEntry(queryEntry);
            return queryEntry;
        }
        public static void onReturn(QueryEntry queryEntry) {
            queryEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            queryEntry.endWithError(t);
        }
    }

    public static class PreparedStatementExecuteQueryAdvice {
        public static boolean isEnabled(HasStatementMirrorMixin preparedStatement) {
            return preparedStatement.glowroot$hasStatementMirror();
        }
        public static QueryEntry onBefore(ThreadContext context,
                HasStatementMirrorMixin preparedStatement) {
            return PreparedStatementExecuteAdvice.onBefore(context, preparedStatement);
        }
        public static void onReturn(HasStatementMirrorMixin resultSet,
                HasStatementMirrorMixin preparedStatement,
                QueryEntry queryEntry) {
            // PreparedStatement can always be retrieved from ResultSet.getStatement(), and
            // StatementMirror from that, but ResultSet.getStatement() is sometimes not super
            // duper fast due to ResultSet wrapping and other checks, so StatementMirror is
            // stored directly in ResultSet as an optimization
            if (resultSet != null) {
                StatementMirror mirror = preparedStatement.glowroot$getStatementMirror();
                resultSet.glowroot$setStatementMirror(mirror);
            }
            queryEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            queryEntry.endWithError(t);
        }
    }

    public static class PreparedStatementExecuteUpdateAdvice {
        public static boolean isEnabled(HasStatementMirrorMixin preparedStatement) {
            return preparedStatement.glowroot$hasStatementMirror();
        }
        public static QueryEntry onBefore(ThreadContext context,
                HasStatementMirrorMixin preparedStatement) {
            return PreparedStatementExecuteAdvice.onBefore(context, preparedStatement);
        }
        public static void onReturn(int rowCount,
                QueryEntry queryEntry) {
            queryEntry.setCurrRow(rowCount);
            queryEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            queryEntry.endWithError(t);
        }
    }

    public static class StatementExecuteBatchAdvice {
        private static final TimerName timerName =
                Agent.getTimerName(StatementExecuteBatchAdvice.class);
        public static boolean isEnabled(HasStatementMirrorMixin statement) {
            return statement.glowroot$hasStatementMirror();
        }
        public static QueryEntry onBefore(ThreadContext context,
                HasStatementMirrorMixin statement) {
            @SuppressWarnings("nullness") // just checked above in isEnabled()
            @NonNull
            StatementMirror mirror = statement.glowroot$getStatementMirror();
            if (statement instanceof PreparedStatement) {
                return onBeforePreparedStatement(context, (PreparedStatementMirror) mirror);
            } else {
                return onBeforeStatement(mirror, context);
            }
        }
        public static void onReturn(int[] rowCounts,
                QueryEntry queryEntry) {
            int totalRowCount = 0;
            boolean count = false;
            for (int rowCount : rowCounts) {
                if (rowCount > 0) {
                    // ignore Statement.SUCCESS_NO_INFO (-2) and Statement.EXECUTE_FAILED (-3)
                    totalRowCount += rowCount;
                    count = true;
                }
            }
            if (count) {
                queryEntry.setCurrRow(totalRowCount);
            }
            queryEntry.endWithLocationStackTrace(JdbcPluginProperties.stackTraceThresholdMillis(),
                    MILLISECONDS);
        }
        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            queryEntry.endWithError(t);
        }
        private static QueryEntry onBeforePreparedStatement(ThreadContext context,
                PreparedStatementMirror mirror) {
            QueryMessageSupplier queryMessageSupplier;
            String queryText = mirror.getSql();
            int batchSize = mirror.getBatchSize();
            if (batchSize <= 0) {
                queryText = "[empty batch] " + queryText;
                queryMessageSupplier = QueryMessageSupplier.create("jdbc query: ");
                batchSize = 1;
            } else if (captureBindParameters) {
                queryMessageSupplier = new BatchPreparedStatementMessageSupplier(
                        mirror.getBatchedParameters(), batchSize);
            } else {
                queryMessageSupplier = new BatchPreparedStatementMessageSupplier2(batchSize);
            }
            QueryEntry queryEntry = context.startQueryEntry(QUERY_TYPE, queryText, batchSize,
                    queryMessageSupplier, timerName);
            mirror.setLastQueryEntry(queryEntry);
            mirror.clearBatch();
            return queryEntry;
        }
        private static QueryEntry onBeforeStatement(StatementMirror mirror, ThreadContext context) {
            List<String> batchedSql = mirror.getBatchedSql();
            String concatenated;
            if (batchedSql.isEmpty()) {
                concatenated = "[empty batch]";
            } else {
                StringBuilder sb = new StringBuilder("[batch] ");
                boolean first = true;
                for (String sql : batchedSql) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(sql);
                    first = false;
                }
                concatenated = sb.toString();
            }
            QueryEntry queryEntry = context.startQueryEntry(QUERY_TYPE, concatenated,
                    QueryMessageSupplier.create("jdbc query: "), timerName);
            mirror.setLastQueryEntry(queryEntry);
            mirror.clearBatch();
            return queryEntry;
        }
    }

    // ================== Additional ResultSet Tracking ==================

    public static class StatementGetResultSetAdvice {
        public static boolean isEnabled(HasStatementMirrorMixin statement) {
            return statement.glowroot$hasStatementMirror();
        }
        public static void onReturn(HasStatementMirrorMixin resultSet,
                HasStatementMirrorMixin statement) {
            if (resultSet == null) {
                return;
            }
            StatementMirror mirror = statement.glowroot$getStatementMirror();
            resultSet.glowroot$setStatementMirror(mirror);
        }
    }

    // ================== Statement Closing ==================

    public static class CloseAdvice {
        private static final TimerName timerName = Agent.getTimerName(CloseAdvice.class);
        public static boolean isEnabled(HasStatementMirrorMixin statement) {
            return statement.glowroot$hasStatementMirror();
        }
        public static Timer onBefore(ThreadContext context,
                HasStatementMirrorMixin statement) {
            StatementMirror mirror = statement.glowroot$getStatementMirror();
            if (mirror != null) {
                // this should always be true since just checked hasGlowrootStatementMirror() above
                mirror.clearLastQueryEntry();
            }
            if (captureStatementClose.value()) {
                return context.startTimer(timerName);
            } else {
                return null;
            }
        }
        public static void onAfter(Timer timer) {
            if (timer != null) {
                timer.stop();
            }
        }
    }
}

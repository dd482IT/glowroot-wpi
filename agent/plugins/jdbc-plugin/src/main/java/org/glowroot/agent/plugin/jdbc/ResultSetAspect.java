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
import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.QueryEntry;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.checker.NonNull;
import org.glowroot.agent.plugin.api.config.BooleanProperty;
import org.glowroot.agent.plugin.api.config.ConfigService;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;
import org.glowroot.agent.plugin.jdbc.StatementAspect.HasStatementMirrorMixin;

public class ResultSetAspect {

    private static final Logger logger = Logger.getLogger(ResultSetAspect.class);
    private static final ConfigService configService = Agent.getConfigService("jdbc");

    public interface ResultSet {
        int getRow();
    }

    public static class NextAdvice {
        private static final BooleanProperty timerEnabled =
                configService.getBooleanProperty("captureResultSetNavigate");
        public static boolean isEnabled(HasStatementMirrorMixin resultSet) {
            return timerEnabled.value() && isEnabledCommon(resultSet);
        }
        public static Timer onBefore(HasStatementMirrorMixin resultSet) {
            return onBeforeCommon(resultSet);
        }
        public static void onReturn(boolean currentRowValid,
                HasStatementMirrorMixin resultSet) {
            StatementMirror mirror = resultSet.glowroot$getStatementMirror();
            if (mirror == null) {
                // this shouldn't happen since just checked above in isEnabled(), unless some
                // bizarre concurrent mis-usage of ResultSet
                return;
            }
            QueryEntry lastQueryEntry = mirror.getLastQueryEntry();
            if (lastQueryEntry == null) {
                // tracing must be disabled (e.g. exceeded trace entry limit)
                return;
            }
            if (currentRowValid) {
                // ResultSet.getRow() is sometimes not super duper fast due to ResultSet
                // wrapping and other checks, so this optimizes the common case
                lastQueryEntry.incrementCurrRow();
            } else {
                lastQueryEntry.rowNavigationAttempted();
            }
        }
        public static void onAfter(Timer timer) {
            timer.stop();
        }
    }

    public static class NavigateAdvice {
        private static final BooleanProperty timerEnabled =
                configService.getBooleanProperty("captureResultSetNavigate");
        public static boolean isEnabled(HasStatementMirrorMixin resultSet) {
            return timerEnabled.value() && isEnabledCommon(resultSet);
        }
        public static Timer onBefore(HasStatementMirrorMixin resultSet) {
            return onBeforeCommon(resultSet);
        }
        public static void onReturn(HasStatementMirrorMixin resultSet) {
            try {
                StatementMirror mirror = resultSet.glowroot$getStatementMirror();
                if (mirror == null) {
                    // this shouldn't happen since just checked above in isEnabled(), unless some
                    // bizarre concurrent mis-usage of ResultSet
                    return;
                }
                QueryEntry lastQueryEntry = mirror.getLastQueryEntry();
                if (lastQueryEntry == null) {
                    // tracing must be disabled (e.g. exceeded trace entry limit)
                    return;
                }
                lastQueryEntry.setCurrRow(((ResultSet) resultSet).getRow());
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
        public static void onAfter(Timer timer) {
            timer.stop();
        }
    }

    public static class ValueAdvice {
        private static final BooleanProperty timerEnabled =
                configService.getBooleanProperty("captureResultSetGet");
        public static boolean isEnabled(HasStatementMirrorMixin resultSet) {
            return timerEnabled.value() && isEnabledCommon(resultSet);
        }
        public static Timer onBefore(HasStatementMirrorMixin resultSet) {
            return onBeforeCommon(resultSet);
        }
        public static void onAfter(Timer timer) {
            timer.stop();
        }
    }

    public static class ValueAdvice2 {
        private static final BooleanProperty timerEnabled =
                configService.getBooleanProperty("captureResultSetGet");
        public static boolean isEnabled(HasStatementMirrorMixin resultSet) {
            return timerEnabled.value() && isEnabledCommon(resultSet);
        }
        public static Timer onBefore(HasStatementMirrorMixin resultSet) {
            return onBeforeCommon(resultSet);
        }
        public static void onAfter(Timer timer) {
            timer.stop();
        }
    }

    private static boolean isEnabledCommon(HasStatementMirrorMixin resultSet) {
        StatementMirror mirror = resultSet.glowroot$getStatementMirror();
        return mirror != null && mirror.getLastQueryEntry() != null;
    }

    private static Timer onBeforeCommon(HasStatementMirrorMixin resultSet) {
        @SuppressWarnings("nullness") // just checked above in isEnabledCommon()
        @NonNull
        StatementMirror mirror = resultSet.glowroot$getStatementMirror();
        @SuppressWarnings("nullness") // just checked above in isEnabledCommon()
        @NonNull
        QueryEntry lastQueryEntry = mirror.getLastQueryEntry();
        return lastQueryEntry.extend();
    }
}

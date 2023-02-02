/*
 * Copyright 2019 the original author or authors.
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
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.BooleanProperty;
import org.glowroot.agent.plugin.api.config.ConfigService;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class ObjectPoolAspect {

    private static final ConfigService configService = Agent.getConfigService("jdbc");

    private static final BooleanProperty captureConnectionPoolLeaks =
            configService.getBooleanProperty("captureConnectionPoolLeaks");

    private static final BooleanProperty captureConnectionPoolLeakDetails =
            configService.getBooleanProperty("captureConnectionPoolLeakDetails");

    public static class DbcpBorrowAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(Object resource, ThreadContext context) {
            if (resource != null) {
                context.trackResourceAcquired(resource, captureConnectionPoolLeakDetails.value());
            }
        }
    }

    public static class DbcpReturnAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(ThreadContext context,
                Object resource) {
            if (resource != null) {
                context.trackResourceReleased(resource);
            }
        }
    }

    public static class TomcatBorrowAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(Object resource, ThreadContext context) {
            if (resource != null) {
                context.trackResourceAcquired(resource, captureConnectionPoolLeakDetails.value());
            }
        }
    }

    public static class TomcatReturnAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(ThreadContext context,
                Object resource) {
            if (resource != null) {
                context.trackResourceReleased(resource);
            }
        }
    }

    public static class GlassfishBorrowAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(Object resource, ThreadContext context) {
            if (resource != null) {
                context.trackResourceAcquired(resource, captureConnectionPoolLeakDetails.value());
            }
        }
    }

    public static class GlassfishReturnAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(ThreadContext context,
                @SuppressWarnings("unused") Exception e,
                Object connectionHolder) {
            if (connectionHolder != null) {
                context.trackResourceReleased(connectionHolder);
            }
        }
    }

    public static class HikariBorrowAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(Object resource, ThreadContext context) {
            if (resource != null) {
                context.trackResourceAcquired(resource, captureConnectionPoolLeakDetails.value());
            }
        }
    }

    public static class HikariReturnAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(ThreadContext context, Object connectionProxy) {
            context.trackResourceReleased(connectionProxy);
        }
    }

    public static class BitronixBorrowAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(Object resource, ThreadContext context) {
            if (resource != null) {
                context.trackResourceAcquired(resource, captureConnectionPoolLeakDetails.value());
            }
        }
    }

    public static class BitronixReturnAdvice {
        public static boolean isEnabled() {
            return captureConnectionPoolLeaks.value();
        }
        public static void onReturn(ThreadContext context, Object connectionProxy) {
            context.trackResourceReleased(connectionProxy);
        }
    }
}

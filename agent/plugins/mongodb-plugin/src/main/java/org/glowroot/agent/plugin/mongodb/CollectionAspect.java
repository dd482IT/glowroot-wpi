/*
 * Copyright 2018-2019 the original author or authors.
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
package org.glowroot.agent.plugin.mongodb;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.QueryEntry;
import org.glowroot.agent.plugin.api.QueryMessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.ConfigListener;
import org.glowroot.agent.plugin.api.config.ConfigService;
import org.glowroot.agent.plugin.api.weaving.BindMethodName;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;
import org.glowroot.agent.plugin.mongodb.MongoIterableAspect.MongoIterableMixin;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CollectionAspect {

    private static final String QUERY_TYPE = "MongoDB";

    private static final ConfigService configService = Agent.getConfigService("mongodb");

    // visibility is provided by memoryBarrier in org.glowroot.config.ConfigService
    private static int stackTraceThresholdMillis;

    static {
        configService.registerConfigListener(new ConfigListener() {
            @Override
            public void onChange() {
                Double value = configService.getDoubleProperty("stackTraceThresholdMillis").value();
                stackTraceThresholdMillis = value == null ? Integer.MAX_VALUE : value.intValue();
            }
        });
    }

    public interface MongoCollection {

        Object getNamespace();
    }

    public interface DBCollection {

        String getFullName();
    }

    // TODO add MongoCollection.watch()
    public static class MongoCollectionAdvice {

        private static final TimerName timerName = Agent.getTimerName(MongoCollectionAdvice.class);

        public static QueryEntry onBefore(ThreadContext context,
                MongoCollection collection, String methodName) {
            Object namespace = collection.getNamespace();
            if (namespace == null) {
                return null;
            }
            String queryText = methodName + " " + namespace.toString();
            return context.startQueryEntry(QUERY_TYPE, queryText,
                    QueryMessageSupplier.create("mongodb query: "), timerName);
        }

        public static void onReturn(QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithLocationStackTrace(stackTraceThresholdMillis, MILLISECONDS);
            }
        }

        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithError(t);
            }
        }
    }

    public static class MongoFindAdvice {

        private static final TimerName timerName = Agent.getTimerName(MongoCollectionAdvice.class);

        public static QueryEntry onBefore(ThreadContext context,
                MongoCollection collection, String methodName) {
            Object namespace = collection.getNamespace();
            if (namespace == null) {
                return null;
            }
            String queryText = methodName + " " + namespace.toString();
            return context.startQueryEntry(QUERY_TYPE, queryText,
                    QueryMessageSupplier.create("mongodb query: "), timerName);
        }

        public static void onReturn(MongoIterableMixin mongoIterable,
                QueryEntry queryEntry) {
            if (queryEntry != null) {
                if (mongoIterable != null) {
                    mongoIterable.glowroot$setQueryEntry(queryEntry);
                }
                queryEntry.endWithLocationStackTrace(stackTraceThresholdMillis, MILLISECONDS);
            }
        }

        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithError(t);
            }
        }
    }

    // mongodb driver legacy API (prior to 3.7.0)
    public static class DBCollectionAdvice {

        private static final TimerName timerName = Agent.getTimerName(DBCollectionAdvice.class);

        public static QueryEntry onBefore(ThreadContext context,
                DBCollection collection, String methodName) {
            if (methodName.equals("getCount")) {
                methodName = "count";
            }
            String queryText = methodName + " " + collection.getFullName();
            return context.startQueryEntry(QUERY_TYPE, queryText,
                    QueryMessageSupplier.create("mongodb query: "), timerName);
        }

        public static void onReturn(QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithLocationStackTrace(stackTraceThresholdMillis, MILLISECONDS);
            }
        }

        public static void onThrow(Throwable t,
                QueryEntry queryEntry) {
            if (queryEntry != null) {
                queryEntry.endWithError(t);
            }
        }
    }
}

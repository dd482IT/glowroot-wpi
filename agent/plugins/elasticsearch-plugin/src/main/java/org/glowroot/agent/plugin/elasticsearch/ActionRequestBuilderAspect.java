/*
 * Copyright 2017-2018 the original author or authors.
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
package org.glowroot.agent.plugin.elasticsearch;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.AsyncQueryEntry;
import org.glowroot.agent.plugin.api.QueryEntry;
import org.glowroot.agent.plugin.api.QueryMessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.config.ConfigListener;
import org.glowroot.agent.plugin.api.config.ConfigService;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;
import org.glowroot.agent.plugin.elasticsearch.ActionFutureAspect.ActionFutureMixin;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ActionRequestBuilderAspect {

    private static final String QUERY_TYPE = "Elasticsearch";

    private static final ConfigService configService = Agent.getConfigService("elasticsearch");

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

    public interface ActionRequestBuilder {

        ActionRequest glowroot$request();
    }

    public interface SearchRequestBuilder {

        Object glowroot$sourceBuilder();
    }

    public interface ActionRequest {}

    public interface IndexRequest extends ActionRequest {
        String index();
        String type();
    }

    public interface GetRequest extends ActionRequest {
        String index();
        String type();
        String id();
    }

    public interface UpdateRequest extends ActionRequest {
        String index();
        String type();
        String id();
    }

    public interface DeleteRequest extends ActionRequest {
        String index();
        String type();
        String id();
    }

    public interface SearchRequest extends ActionRequest {
        String /*@Nullable*/ [] indices();
        String /*@Nullable*/ [] types();
    }

    public interface BytesReference {
        String toUtf8();
    }

    public static class ExecuteAdvice {
        private static final TimerName timerName = Agent.getTimerName(ExecuteAdvice.class);
        public static QueryEntry onBefore(ThreadContext context,
                ActionRequestBuilder actionRequestBuilder) {
            return context.startQueryEntry(QUERY_TYPE, getQueryText(actionRequestBuilder),
                    getQueryMessageSupplier(actionRequestBuilder), timerName);
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

    public static class ExecuteAsyncAdvice {
        private static final TimerName timerName = Agent.getTimerName(ExecuteAsyncAdvice.class);
        public static AsyncQueryEntry onBefore(ThreadContext context,
                ActionRequestBuilder actionRequestBuilder) {
            return context.startAsyncQueryEntry(QUERY_TYPE, getQueryText(actionRequestBuilder),
                    getQueryMessageSupplier(actionRequestBuilder), timerName);
        }
        public static void onReturn(ActionFutureMixin future,
                AsyncQueryEntry asyncQueryEntry) {
            if (asyncQueryEntry == null) {
                return;
            }
            asyncQueryEntry.stopSyncTimer();
            if (future == null) {
                asyncQueryEntry.end();
                return;
            }
            // to prevent race condition, setting async query entry before getting completed status,
            // and the converse is done when getting async query entry
            // ok if end() happens to get called twice
            future.glowroot$setAsyncQueryEntry(asyncQueryEntry);
            if (future.glowroot$isCompleted()) {
                // ListenableActionFuture completed really fast, prior to @OnReturn
                Throwable exception = future.glowroot$getException();
                if (exception == null) {
                    asyncQueryEntry.end();
                } else {
                    asyncQueryEntry.endWithError(exception);
                }
                return;
            }
        }
        public static void onThrow(Throwable t,
                AsyncQueryEntry asyncQueryEntry) {
            if (asyncQueryEntry != null) {
                asyncQueryEntry.stopSyncTimer();
                asyncQueryEntry.endWithError(t);
            }
        }
    }

    private static String getQueryText(ActionRequestBuilder actionRequestBuilder) {
        ActionRequest actionRequest = actionRequestBuilder.glowroot$request();
        if (actionRequest instanceof IndexRequest) {
            IndexRequest request = (IndexRequest) actionRequest;
            return "PUT " + request.index() + '/' + request.type();
        } else if (actionRequest instanceof GetRequest) {
            GetRequest request = (GetRequest) actionRequest;
            return "GET " + request.index() + '/' + request.type();
        } else if (actionRequest instanceof UpdateRequest) {
            UpdateRequest request = (UpdateRequest) actionRequest;
            return "PUT " + request.index() + '/' + request.type();
        } else if (actionRequest instanceof DeleteRequest) {
            DeleteRequest request = (DeleteRequest) actionRequest;
            return "DELETE " + request.index() + '/' + request.type();
        } else if (actionRequest instanceof SearchRequest) {
            SearchRequest request = (SearchRequest) actionRequest;
            return getQueryText(request, (SearchRequestBuilder) actionRequestBuilder);
        } else if (actionRequest == null) {
            return "(action request was null)";
        } else {
            return actionRequest.getClass().getName();
        }
    }

    private static String getQueryText(SearchRequest request,
            SearchRequestBuilder actionRequestBuilder) {
        StringBuilder sb = new StringBuilder("SEARCH ");
        String[] indices = request.indices();
        String[] types = request.types();
        if (indices != null && indices.length > 0) {
            if (types != null && types.length > 0) {
                appendTo(sb, indices);
                sb.append('/');
                appendTo(sb, types);
            } else {
                appendTo(sb, indices);
            }
        } else {
            if (types != null && types.length > 0) {
                sb.append("_any/");
                appendTo(sb, types);
            } else {
                sb.append('/');
            }
        }
        Object sourceBuilder = actionRequestBuilder.glowroot$sourceBuilder();
        if (sourceBuilder != null) {
            sb.append(' ');
            sb.append(sourceBuilder);
        }
        return sb.toString();
    }

    private static QueryMessageSupplier getQueryMessageSupplier(
            ActionRequestBuilder actionRequestBuilder) {
        ActionRequest actionRequest = actionRequestBuilder.glowroot$request();
        if (actionRequest instanceof IndexRequest) {
            return QueryMessageSupplier.create(Constants.QUERY_MESSAGE_PREFIX);
        } else if (actionRequest instanceof GetRequest) {
            GetRequest request = (GetRequest) actionRequest;
            return new QueryMessageSupplierWithId(request.id());
        } else if (actionRequest instanceof UpdateRequest) {
            UpdateRequest request = (UpdateRequest) actionRequest;
            return new QueryMessageSupplierWithId(request.id());
        } else if (actionRequest instanceof DeleteRequest) {
            DeleteRequest request = (DeleteRequest) actionRequest;
            return new QueryMessageSupplierWithId(request.id());
        } else if (actionRequest instanceof SearchRequest) {
            return QueryMessageSupplier.create(Constants.QUERY_MESSAGE_PREFIX);
        } else {
            return QueryMessageSupplier.create(Constants.QUERY_MESSAGE_PREFIX);
        }
    }

    private static void appendTo(StringBuilder sb, String[] values) {
        boolean first = true;
        for (String value : values) {
            if (!first) {
                sb.append(',');
            }
            sb.append(value);
            first = false;
        }
    }
}

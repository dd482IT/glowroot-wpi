/*
 * Copyright 2016-2019 the original author or authors.
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
package org.glowroot.agent.plugin.httpclient;

import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.AsyncTraceEntry;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ParameterHolder;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.httpclient._.Uris;

public class ApacheHttpAsyncClientAspect {

    public static class ExecuteAdvice {
        private static final TimerName timerName = Agent.getTimerName(ExecuteAdvice.class);
        public static AsyncTraceEntry onBefore(ThreadContext context,
                HttpUriRequest request,
                ParameterHolder<FutureCallback<HttpResponse>> callback) {
            if (request == null) {
                return null;
            }
            String method = request.getMethod();
            if (method == null) {
                method = "";
            } else {
                method += " ";
            }
            URI uriObj = request.getURI();
            String uri;
            if (uriObj == null) {
                uri = "";
            } else {
                uri = uriObj.toString();
            }
            AsyncTraceEntry asyncTraceEntry = context.startAsyncServiceCallEntry("HTTP",
                    method + Uris.stripQueryString(uri),
                    MessageSupplier.create("http client request: {}{}", method, uri), timerName);
            callback.set(createWrapper(context, callback, asyncTraceEntry));
            return asyncTraceEntry;
        }
        public static void onReturn(AsyncTraceEntry asyncTraceEntry) {
            if (asyncTraceEntry != null) {
                asyncTraceEntry.stopSyncTimer();
            }
        }
        public static void onThrow(Throwable t,
                AsyncTraceEntry asyncTraceEntry) {
            if (asyncTraceEntry != null) {
                asyncTraceEntry.stopSyncTimer();
                asyncTraceEntry.endWithError(t);
            }
        }
        private static FutureCallback<HttpResponse> createWrapper(ThreadContext context,
                ParameterHolder<FutureCallback<HttpResponse>> callback,
                AsyncTraceEntry asyncTraceEntry) {
            FutureCallback<HttpResponse> delegate = callback.get();
            if (delegate == null) {
                return new FutureCallbackWrapperForNullDelegate<HttpResponse>(asyncTraceEntry);
            } else {
                return new FutureCallbackWrapper<HttpResponse>(delegate, asyncTraceEntry,
                        context.createAuxThreadContext());
            }
        }
    }

    public static class ExecuteAdvice2 {
        public static AsyncTraceEntry onBefore(ThreadContext context,
                HttpUriRequest request,
                @SuppressWarnings("unused") Object httpContext,
                ParameterHolder<FutureCallback<HttpResponse>> callback) {
            return ExecuteAdvice.onBefore(context, request, callback);
        }
        public static void onReturn(AsyncTraceEntry asyncTraceEntry) {
            ExecuteAdvice.onReturn(asyncTraceEntry);
        }
        public static void onThrow(Throwable t,
                AsyncTraceEntry asyncTraceEntry) {
            ExecuteAdvice.onThrow(t, asyncTraceEntry);
        }
    }

    public static class ExecuteWithHostAdvice {
        private static final TimerName timerName = Agent.getTimerName(ExecuteWithHostAdvice.class);
        public static AsyncTraceEntry onBefore(ThreadContext context,
                HttpHost hostObj,
                HttpRequest request,
                ParameterHolder<FutureCallback<HttpResponse>> callback) {
            if (request == null) {
                return null;
            }
            RequestLine requestLine = request.getRequestLine();
            if (requestLine == null) {
                return null;
            }
            String method = requestLine.getMethod();
            if (method == null) {
                method = "";
            } else {
                method += " ";
            }
            String host = hostObj == null ? "" : hostObj.toURI();
            String uri = requestLine.getUri();
            if (uri == null) {
                uri = "";
            }
            AsyncTraceEntry asyncTraceEntry = context.startAsyncServiceCallEntry("HTTP",
                    method + Uris.stripQueryString(uri),
                    MessageSupplier.create("http client request: {}{}{}", method, host, uri),
                    timerName);
            callback.set(ExecuteAdvice.createWrapper(context, callback, asyncTraceEntry));
            return asyncTraceEntry;
        }
        public static void onReturn(AsyncTraceEntry asyncTraceEntry) {
            if (asyncTraceEntry != null) {
                asyncTraceEntry.stopSyncTimer();
            }
        }
        public static void onThrow(Throwable t,
                AsyncTraceEntry asyncTraceEntry) {
            if (asyncTraceEntry != null) {
                asyncTraceEntry.stopSyncTimer();
                asyncTraceEntry.endWithError(t);
            }
        }
    }

    public static class ExecuteWithHostAdvice2 {
        public static AsyncTraceEntry onBefore(ThreadContext context,
                HttpHost hostObj,
                HttpRequest request,
                @SuppressWarnings("unused") Object httpContext,
                ParameterHolder<FutureCallback<HttpResponse>> callback) {
            return ExecuteWithHostAdvice.onBefore(context, hostObj, request, callback);
        }
        public static void onReturn(AsyncTraceEntry asyncTraceEntry) {
            ExecuteWithHostAdvice.onReturn(asyncTraceEntry);
        }
        public static void onThrow(Throwable t,
                AsyncTraceEntry asyncTraceEntry) {
            ExecuteWithHostAdvice.onThrow(t, asyncTraceEntry);
        }
    }

    public static class ExecuteWithProducerConsumerAdvice {
        public static void onBefore(ThreadContext context,
                @SuppressWarnings("unused") Object producer,
                @SuppressWarnings("unused") Object consumer,
                ParameterHolder<FutureCallback<HttpResponse>> callback) {
            FutureCallback<HttpResponse> delegate = callback.get();
            if (delegate != null) {
                callback.set(new FutureCallbackWithoutEntryWrapper<HttpResponse>(delegate,
                        context.createAuxThreadContext()));
            }
        }
    }

    public static class ExecuteWithProducerConsumerAdvice2 {
        public static void onBefore(ThreadContext context, Object producer,
                Object consumer,
                @SuppressWarnings("unused") Object httpContext,
                ParameterHolder<FutureCallback<HttpResponse>> callback) {
            ExecuteWithProducerConsumerAdvice.onBefore(context, producer, consumer, callback);
        }
    }
}

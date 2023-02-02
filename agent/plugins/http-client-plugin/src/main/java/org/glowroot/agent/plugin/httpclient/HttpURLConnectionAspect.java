/*
 * Copyright 2017-2019 the original author or authors.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.httpclient._.Uris;

public class HttpURLConnectionAspect {

    private static final Logger logger = Logger.getLogger(HttpURLConnectionAspect.class);
    private static final AtomicBoolean inputStreamIssueAlreadyLogged = new AtomicBoolean();
    private static final AtomicBoolean outputStreamIssueAlreadyLogged = new AtomicBoolean();

    // the field and method names are verbose since they will be mixed in to existing classes
    public static class HasTraceEntryImpl implements HasTraceEntryMixin {

        private transient TraceEntry glowroot$traceEntry;

        @Override
        public TraceEntry glowroot$getTraceEntry() {
            return glowroot$traceEntry;
        }

        @Override
        public void glowroot$setTraceEntry(TraceEntry traceEntry) {
            glowroot$traceEntry = traceEntry;
        }

        @Override
        public boolean glowroot$hasTraceEntry() {
            return glowroot$traceEntry != null;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface HasTraceEntryMixin {

        TraceEntry glowroot$getTraceEntry();

        void glowroot$setTraceEntry(TraceEntry traceEntry);

        boolean glowroot$hasTraceEntry();
    }

    private static class TraceEntryOrTimer {

        private final TraceEntry traceEntry;
        private final Timer timer;

        private TraceEntryOrTimer(TraceEntry traceEntry) {
            this.traceEntry = traceEntry;
            timer = null;
        }

        private TraceEntryOrTimer(Timer timer) {
            this.timer = timer;
            traceEntry = null;
        }

        private void onReturn() {
            if (traceEntry != null) {
                traceEntry.end();
            } else if (timer != null) {
                timer.stop();
            }
        }

        private void onThrow(Throwable t) {
            if (traceEntry != null) {
                traceEntry.endWithError(t);
            } else if (timer != null) {
                timer.stop();
            }
        }
    }

    public static class ConnectAdvice {
        private static final TimerName timerName = Agent.getTimerName(ConnectAdvice.class);
        public static TraceEntryOrTimer onBefore(ThreadContext threadContext,
                HttpURLConnection httpURLConnection) {
            return onBefore(threadContext, httpURLConnection, false);
        }
        public static void onReturn(TraceEntryOrTimer entryOrTimer) {
            if (entryOrTimer != null) {
                entryOrTimer.onReturn();
            }
        }
        public static void onThrow(Throwable t,
                TraceEntryOrTimer entryOrTimer) {
            if (entryOrTimer != null) {
                entryOrTimer.onThrow(t);
            }
        }
        private static TraceEntryOrTimer onBefore(ThreadContext threadContext,
                HttpURLConnection httpURLConnection, boolean overrideGetWithPost) {
            if (!(httpURLConnection instanceof HasTraceEntryMixin)) {
                return null;
            }
            TraceEntry traceEntry =
                    ((HasTraceEntryMixin) httpURLConnection).glowroot$getTraceEntry();
            if (traceEntry != null) {
                return new TraceEntryOrTimer(traceEntry.extend());
            }
            String method = httpURLConnection.getRequestMethod();
            if (method == null) {
                method = "";
            } else if (overrideGetWithPost && method.equals("GET")) {
                // this is to match behavior in
                // sun.net.www.protocol.http.HttpURLConnection.getOutputStream0()
                method = "POST ";
            } else {
                method += " ";
            }
            URL urlObj = httpURLConnection.getURL();
            String url;
            if (urlObj == null) {
                url = "";
            } else {
                url = urlObj.toString();
            }
            traceEntry = threadContext.startServiceCallEntry("HTTP",
                    method + Uris.stripQueryString(url),
                    MessageSupplier.create("http client request: {}{}", method, url), timerName);
            ((HasTraceEntryMixin) httpURLConnection).glowroot$setTraceEntry(traceEntry);
            return new TraceEntryOrTimer(traceEntry);
        }
    }

    public static class GetInputStreamAdvice {
        public static TraceEntryOrTimer onBefore(ThreadContext threadContext,
                HttpURLConnection httpURLConnection) {
            return ConnectAdvice.onBefore(threadContext, httpURLConnection, false);
        }
        public static void onReturn(Object returnValue,
                HttpURLConnection httpURLConnection,
                TraceEntryOrTimer entryOrTimer) {
            if (httpURLConnection instanceof HasTraceEntryMixin) {
                if (returnValue instanceof HasTraceEntryMixin) {
                    TraceEntry traceEntry =
                            ((HasTraceEntryMixin) httpURLConnection).glowroot$getTraceEntry();
                    ((HasTraceEntryMixin) returnValue).glowroot$setTraceEntry(traceEntry);
                } else if (returnValue != null && !inputStreamIssueAlreadyLogged.getAndSet(true)) {
                    logger.info("found non-instrumented http url connection input stream, please"
                            + " report to the Glowroot project: {}",
                            returnValue.getClass().getName());
                }
            }
            ConnectAdvice.onReturn(entryOrTimer);
        }
        public static void onThrow(Throwable t,
                TraceEntryOrTimer entryOrTimer) {
            ConnectAdvice.onThrow(t, entryOrTimer);
        }
    }

    public static class GetOutputStreamAdvice {
        public static TraceEntryOrTimer onBefore(ThreadContext threadContext,
                HttpURLConnection httpURLConnection) {
            return ConnectAdvice.onBefore(threadContext, httpURLConnection, true);
        }
        public static void onReturn(Object returnValue,
                HttpURLConnection httpURLConnection,
                TraceEntryOrTimer entryOrTimer) {
            if (httpURLConnection instanceof HasTraceEntryMixin) {
                if (returnValue instanceof HasTraceEntryMixin) {
                    TraceEntry traceEntry =
                            ((HasTraceEntryMixin) httpURLConnection).glowroot$getTraceEntry();
                    ((HasTraceEntryMixin) returnValue).glowroot$setTraceEntry(traceEntry);
                } else if (returnValue != null && !outputStreamIssueAlreadyLogged.getAndSet(true)) {
                    logger.info("found non-instrumented http url connection output stream, please"
                            + " report to the Glowroot project: {}",
                            returnValue.getClass().getName());
                }
            }
            ConnectAdvice.onReturn(entryOrTimer);
        }
        public static void onThrow(Throwable t,
                TraceEntryOrTimer entryOrTimer) {
            ConnectAdvice.onThrow(t, entryOrTimer);
        }
    }

    public static class HttpInputStreamAdvice {
        public static Timer onBefore(InputStream inputStream) {
            if (!(inputStream instanceof HasTraceEntryMixin)) {
                return null;
            }
            TraceEntry traceEntry = ((HasTraceEntryMixin) inputStream).glowroot$getTraceEntry();
            if (traceEntry == null) {
                return null;
            }
            return traceEntry.extend();
        }
        public static void onAfter(Timer timer) {
            if (timer != null) {
                timer.stop();
            }
        }
    }

    public static class StreamingOutputStreamAdvice {
        public static Timer onBefore(OutputStream outputStream) {
            if (!(outputStream instanceof HasTraceEntryMixin)) {
                return null;
            }
            TraceEntry traceEntry =
                    ((HasTraceEntryMixin) outputStream).glowroot$getTraceEntry();
            if (traceEntry == null) {
                return null;
            }
            return traceEntry.extend();
        }
        public static void onAfter(Timer timer) {
            if (timer != null) {
                timer.stop();
            }
        }
    }
}

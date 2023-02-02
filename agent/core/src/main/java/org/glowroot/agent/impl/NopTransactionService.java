/*
 * Copyright 2015-2018 the original author or authors.
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
package org.glowroot.agent.impl;

import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.plugin.api.AsyncQueryEntry;
import org.glowroot.agent.plugin.api.AsyncTraceEntry;
import org.glowroot.agent.plugin.api.AuxThreadContext;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.QueryEntry;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.TraceEntry;

class NopTransactionService {

    static final TraceEntry TRACE_ENTRY = NopAsyncQueryEntry.INSTANCE;
    static final QueryEntry QUERY_ENTRY = NopAsyncQueryEntry.INSTANCE;
    static final AsyncTraceEntry ASYNC_TRACE_ENTRY = NopAsyncQueryEntry.INSTANCE;
    static final AsyncQueryEntry ASYNC_QUERY_ENTRY = NopAsyncQueryEntry.INSTANCE;

    private NopTransactionService() {}

    private static class NopAsyncQueryEntry implements AsyncQueryEntry {

        public static final NopAsyncQueryEntry INSTANCE = new NopAsyncQueryEntry();

        private NopAsyncQueryEntry() {}

        @Override
        public void end() {}

        @Override
        public void endWithLocationStackTrace(long threshold, TimeUnit unit) {}

        @Override
        public void endWithError(Throwable t) {}

        @Override
        public void endWithError(String message) {}

        @Override
        public void endWithError(String message, Throwable t) {}

        @Override
        public void endWithInfo(Throwable t) {}

        @Override
        public MessageSupplier getMessageSupplier() {
            return null;
        }

        @Override
        public Timer extend() {
            return NopTimer.INSTANCE;
        }

        @Override
        public void rowNavigationAttempted() {}

        @Override
        public void incrementCurrRow() {}

        @Override
        public void setCurrRow(long row) {}

        @Override
        public void stopSyncTimer() {}

        @Override
        public Timer extendSyncTimer(ThreadContext currThreadContext) {
            return NopTimer.INSTANCE;
        }
    }

    static class NopAuxThreadContext implements AuxThreadContext {

        static final NopAuxThreadContext INSTANCE = new NopAuxThreadContext();

        private NopAuxThreadContext() {}

        @Override
        public TraceEntry start() {
            return NopTransactionService.TRACE_ENTRY;
        }

        @Override
        public TraceEntry startAndMarkAsyncTransactionComplete() {
            return NopTransactionService.TRACE_ENTRY;
        }
    }

    static class NopTimer implements Timer {

        static final NopTimer INSTANCE = new NopTimer();

        private NopTimer() {}

        @Override
        public void stop() {}
    }
}

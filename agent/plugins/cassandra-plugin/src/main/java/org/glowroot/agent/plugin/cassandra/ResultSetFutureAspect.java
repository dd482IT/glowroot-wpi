/*
 * Copyright 2016-2018 the original author or authors.
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
package org.glowroot.agent.plugin.cassandra;

import org.glowroot.agent.plugin.api.AsyncQueryEntry;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.checker.NonNull;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.cassandra.ResultSetAspect.ResultSetMixin;

public class ResultSetFutureAspect {

    // the field and method names are verbose since they will be mixed in to existing classes
    public static class ResultSetFutureImpl implements ResultSetFutureMixin {

        private transient volatile boolean glowroot$completed;
        private transient volatile Throwable glowroot$exception;
        private transient volatile AsyncQueryEntry glowroot$asyncQueryEntry;

        @Override
        public void glowroot$setCompleted() {
            glowroot$completed = true;
        }

        @Override
        public boolean glowroot$isCompleted() {
            return glowroot$completed;
        }

        @Override
        public void glowroot$setException(Throwable exception) {
            glowroot$exception = exception;
        }

        @Override
        public Throwable glowroot$getException() {
            return glowroot$exception;
        }

        @Override
        public AsyncQueryEntry glowroot$getAsyncQueryEntry() {
            return glowroot$asyncQueryEntry;
        }

        @Override
        public void glowroot$setAsyncQueryEntry(AsyncQueryEntry asyncQueryEntry) {
            glowroot$asyncQueryEntry = asyncQueryEntry;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface ResultSetFutureMixin {

        void glowroot$setCompleted();

        boolean glowroot$isCompleted();

        void glowroot$setException(Throwable t);

        Throwable glowroot$getException();

        AsyncQueryEntry glowroot$getAsyncQueryEntry();

        void glowroot$setAsyncQueryEntry(AsyncQueryEntry asyncQueryEntry);
    }

    public static class FutureGetAdvice {
        public static boolean isEnabled(ResultSetFutureMixin resultSetFuture) {
            return resultSetFuture.glowroot$getAsyncQueryEntry() != null;
        }
        public static Timer onBefore(ThreadContext threadContext,
                ResultSetFutureMixin resultSetFuture) {
            @SuppressWarnings("nullness") // just checked above in isEnabled()
            @NonNull
            AsyncQueryEntry asyncQueryEntry = resultSetFuture.glowroot$getAsyncQueryEntry();
            return asyncQueryEntry.extendSyncTimer(threadContext);
        }
        public static void onReturn(ResultSetMixin resultSet,
                ResultSetFutureMixin resultSetFuture) {
            if (resultSet == null) {
                return;
            }
            // pass query entry to the result set so it can be used when iterating the result set
            AsyncQueryEntry asyncQueryEntry = resultSetFuture.glowroot$getAsyncQueryEntry();
            resultSet.glowroot$setQueryEntry(asyncQueryEntry);
        }
        public static void onAfter(Timer timer) {
            timer.stop();
        }
    }

    // waiting on async result
    public static class FutureGetUninterruptiblyAdvice {
        public static boolean isEnabled(ResultSetFutureMixin resultSetFuture) {
            return FutureGetAdvice.isEnabled(resultSetFuture);
        }
        public static Timer onBefore(ThreadContext threadContext,
                ResultSetFutureMixin resultSetFuture) {
            return FutureGetAdvice.onBefore(threadContext, resultSetFuture);
        }
        public static void onReturn(ResultSetMixin resultSet,
                ResultSetFutureMixin resultSetFuture) {
            FutureGetAdvice.onReturn(resultSet, resultSetFuture);
        }
        public static void onAfter(Timer timer) {
            FutureGetAdvice.onAfter(timer);
        }
    }

    public static class FutureSetExceptionAdvice {
        // using @OnBefore instead of @OnReturn to ensure that async trace entry is ended prior to
        // an overall transaction that may be waiting on this future has a chance to end
        public static void onBefore(ResultSetFutureMixin resultSetFuture,
                Throwable t) {
            if (t == null) {
                return;
            }
            // to prevent race condition, setting completed/exception status before getting async
            // query entry, and the converse is done when setting async query entry
            // ok if end() happens to get called twice
            resultSetFuture.glowroot$setCompleted();
            resultSetFuture.glowroot$setException(t);
            AsyncQueryEntry asyncQueryEntry = resultSetFuture.glowroot$getAsyncQueryEntry();
            if (asyncQueryEntry != null) {
                asyncQueryEntry.endWithError(t);
            }
        }
    }

    public static class FutureSetAdvice {
        // using @OnBefore instead of @OnReturn to ensure that async trace entry is ended prior to
        // an overall transaction that may be waiting on this future has a chance to end
        public static void onBefore(ResultSetFutureMixin resultSetFuture) {
            // to prevent race condition, setting completed status before getting async query entry,
            // and the converse is done when setting async query entry
            // ok if end() happens to get called twice
            resultSetFuture.glowroot$setCompleted();
            AsyncQueryEntry asyncQueryEntry = resultSetFuture.glowroot$getAsyncQueryEntry();
            if (asyncQueryEntry != null) {
                asyncQueryEntry.end();
            }
        }
    }
}

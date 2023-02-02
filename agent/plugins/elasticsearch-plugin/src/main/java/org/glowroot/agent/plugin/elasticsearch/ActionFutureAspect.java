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

import org.glowroot.agent.plugin.api.AsyncQueryEntry;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.checker.NonNull;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class ActionFutureAspect {

    // the field and method names are verbose since they will be mixed in to existing classes
    public static class ActionFutureImpl implements ActionFutureMixin {

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
    public interface ActionFutureMixin {

        void glowroot$setCompleted();

        boolean glowroot$isCompleted();

        void glowroot$setException(Throwable t);

        Throwable glowroot$getException();

        AsyncQueryEntry glowroot$getAsyncQueryEntry();

        void glowroot$setAsyncQueryEntry(AsyncQueryEntry asyncQueryEntry);
    }

    public static class FutureGetAdvice {
        public static boolean isEnabled(ActionFutureMixin actionFuture) {
            return actionFuture.glowroot$getAsyncQueryEntry() != null;
        }
        public static Timer onBefore(ThreadContext threadContext,
                ActionFutureMixin actionFuture) {
            @SuppressWarnings("nullness") // just checked above in isEnabled()
            @NonNull
            AsyncQueryEntry asyncQueryEntry = actionFuture.glowroot$getAsyncQueryEntry();
            return asyncQueryEntry.extendSyncTimer(threadContext);
        }
        public static void onAfter(Timer timer) {
            timer.stop();
        }
    }

    public static class FutureSetExceptionAdvice {
        // using @OnBefore instead of @OnReturn to ensure that async trace entry is ended prior to
        // an overall transaction that may be waiting on this future has a chance to end
        public static void onBefore(ActionFutureMixin actionFuture,
                Throwable t) {
            if (t == null) {
                return;
            }
            // to prevent race condition, setting completed/exception status before getting async
            // query entry, and the converse is done when setting async query entry
            // ok if end() happens to get called twice
            actionFuture.glowroot$setCompleted();
            actionFuture.glowroot$setException(t);
            AsyncQueryEntry asyncQueryEntry = actionFuture.glowroot$getAsyncQueryEntry();
            if (asyncQueryEntry != null) {
                asyncQueryEntry.endWithError(t);
            }
        }
    }

    public static class FutureSetAdvice {
        // using @OnBefore instead of @OnReturn to ensure that async trace entry is ended prior to
        // an overall transaction that may be waiting on this future has a chance to end
        public static void onBefore(ActionFutureMixin actionFuture) {
            // to prevent race condition, setting completed status before getting async query entry,
            // and the converse is done when setting async query entry
            // ok if end() happens to get called twice
            actionFuture.glowroot$setCompleted();
            AsyncQueryEntry asyncQueryEntry = actionFuture.glowroot$getAsyncQueryEntry();
            if (asyncQueryEntry != null) {
                asyncQueryEntry.end();
            }
        }
    }
}

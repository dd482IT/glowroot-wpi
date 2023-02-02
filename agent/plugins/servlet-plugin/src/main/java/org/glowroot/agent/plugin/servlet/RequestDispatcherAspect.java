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
package org.glowroot.agent.plugin.servlet;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class RequestDispatcherAspect {

    // the field and method names are verbose since they will be mixed in to existing classes
    public abstract static class RequestDispatcherImpl implements RequestDispatcherMixin {

        private transient String glowroot$path;

        @Override
        public String glowroot$getPath() {
            return glowroot$path;
        }

        @Override
        public void glowroot$setPath(String path) {
            glowroot$path = path;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface RequestDispatcherMixin {

        String glowroot$getPath();

        void glowroot$setPath(String path);
    }

    public static class GetParameterAdvice {
        public static void onReturn(RequestDispatcherMixin requestDispatcher,
                String path) {
            if (requestDispatcher == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            requestDispatcher.glowroot$setPath(path);
        }
    }

    public static class DispatchAdvice {
        private static final TimerName timerName = Agent.getTimerName(DispatchAdvice.class);
        public static TraceEntry onBefore(ThreadContext context,
                RequestDispatcherMixin requestDispatcher) {
            return context.startTraceEntry(MessageSupplier.create("servlet dispatch: {}",
                    requestDispatcher.glowroot$getPath()), timerName);
        }
        public static void onReturn(TraceEntry traceEntry) {
            traceEntry.end();
        }
        public static void onThrow(Throwable t,
                TraceEntry traceEntry) {
            traceEntry.endWithError(t);
        }
    }
}

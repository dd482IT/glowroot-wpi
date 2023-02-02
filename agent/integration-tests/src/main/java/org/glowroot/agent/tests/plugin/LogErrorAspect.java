/*
 * Copyright 2012-2016 the original author or authors.
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
package org.glowroot.agent.tests.plugin;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class LogErrorAspect {

    public static class LogErrorAdvice {

        private static final TimerName timerName = Agent.getTimerName(LogErrorAdvice.class);

        public static TraceEntry onBefore(ThreadContext context, String message) {
            return context.startTraceEntry(MessageSupplier.create("ERROR -- {}", message),
                    timerName);
        }

        public static void onAfter(TraceEntry traceEntry) {
            traceEntry.endWithError("test error message");
        }
    }

    public static class AddErrorEntryAdvice {

        private static final TimerName timerName = Agent.getTimerName(AddErrorEntryAdvice.class);

        public static TraceEntry onBefore(ThreadContext context) {
            TraceEntry traceEntry = context.startTraceEntry(
                    MessageSupplier.create("outer entry to test nesting level"), timerName);
            context.addErrorEntry("test add nested error entry message");
            return traceEntry;
        }

        public static void onAfter(TraceEntry traceEntry) {
            traceEntry.end();
        }
    }

    // this is just to generate an additional $glowroot$ method to test that consecutive
    // $glowroot$ methods in an entry stack trace are stripped out correctly
    public static class LogErrorAdvice2 {}
}

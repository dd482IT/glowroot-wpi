/*
 * Copyright 2013-2016 the original author or authors.
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
package org.glowroot.agent.ui.sandbox;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.OptionalThreadContext;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

// this is used to generate a trace with <multiple root nodes> (and with multiple timers) just to
// test this unusual situation
public class ExternalJvmMainAspect {

    public static class MainAdvice {

        private static final TimerName timerName = Agent.getTimerName(MainAdvice.class);

        public static TraceEntry onBefore(OptionalThreadContext context) {
            return context.startTransaction("Sandbox", "javaagent container main",
                    MessageSupplier
                            .create("org.glowroot.agent.it.harness.impl.JavaagentMain.main()"),
                    timerName);
        }

        public static void onAfter(TraceEntry traceEntry) {
            traceEntry.end();
        }
    }

    public static class TimerMarkerOneAdvice {

        private static final TimerName timerName = Agent.getTimerName(TimerMarkerOneAdvice.class);

        public static Timer onBefore(ThreadContext context) {
            return context.startTimer(timerName);
        }

        public static void onAfter(Timer timer) {
            timer.stop();
        }
    }

    public static class TimerMarkerTwoAdvice {

        private static final TimerName timerName = Agent.getTimerName(TimerMarkerTwoAdvice.class);

        public static Timer onBefore(ThreadContext context) {
            return context.startTimer(timerName);
        }

        public static void onAfter(Timer timer) {
            timer.stop();
        }
    }
}

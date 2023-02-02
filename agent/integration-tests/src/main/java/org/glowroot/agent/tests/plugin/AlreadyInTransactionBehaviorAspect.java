/*
 * Copyright 2018 the original author or authors.
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
import org.glowroot.agent.plugin.api.OptionalThreadContext;
import org.glowroot.agent.plugin.api.OptionalThreadContext.AlreadyInTransactionBehavior;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class AlreadyInTransactionBehaviorAspect {

    public static class LevelOneAdvice {

        private static final TimerName timerName = Agent.getTimerName(LevelOneAdvice.class);

        public static TraceEntry onBefore(OptionalThreadContext context,
                String arg) {
            return context.startTransaction("Test new", "abc",
                    MessageSupplier.create("message: {}", arg), timerName,
                    AlreadyInTransactionBehavior.valueOf(arg));
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

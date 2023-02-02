/*
 * Copyright 2011-2018 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.Message;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class LevelTwoAspect {

    public static class LevelTwoAdvice {

        private static final TimerName timerName = Agent.getTimerName(LevelTwoAdvice.class);

        public static TraceEntry onBefore(ThreadContext context, final String arg1,
                final String arg2) {
            return context.startTraceEntry(new MessageSupplier() {
                @Override
                public Message get() {
                    Map<String, String> detail = new LinkedHashMap<String, String>();
                    detail.put("arg1", arg1);
                    detail.put("arg2", arg2);
                    return Message.create("Level Two", detail);
                }
            }, timerName);
        }

        public static void onAfter(TraceEntry traceEntry) {
            traceEntry.end();
        }
    }
}

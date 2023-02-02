/*
 * Copyright 2016-2017 the original author or authors.
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
package org.glowroot.agent.plugin.jsp;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class HttpJspPageAspect {

    public static class HttpJspPageAdvice {

        private static final TimerName timerName = Agent.getTimerName(HttpJspPageAdvice.class);

        public static TraceEntry onBefore(ThreadContext context, Object httpJspPage) {
            // get filename from classname
            String filename = HttpJspPages.getFilename(httpJspPage.getClass());
            return context.startTraceEntry(MessageSupplier.create("jsp render: {}", filename),
                    timerName);
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

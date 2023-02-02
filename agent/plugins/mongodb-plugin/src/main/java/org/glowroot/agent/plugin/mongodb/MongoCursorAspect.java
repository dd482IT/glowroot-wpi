/*
 * Copyright 2019 the original author or authors.
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
package org.glowroot.agent.plugin.mongodb;

import org.glowroot.agent.plugin.api.QueryEntry;
import org.glowroot.agent.plugin.api.Timer;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.Mixin;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class MongoCursorAspect {

    // the field and method names are verbose since they will be mixed in to existing classes
    public static class MongoCursorImpl implements MongoCursorMixin {

        // does not need to be volatile, app/framework must provide visibility of MongoIterables if
        // used across threads and this can piggyback
        private transient QueryEntry glowroot$queryEntry;

        @Override
        public QueryEntry glowroot$getQueryEntry() {
            return glowroot$queryEntry;
        }

        @Override
        public void glowroot$setQueryEntry(QueryEntry queryEntry) {
            glowroot$queryEntry = queryEntry;
        }
    }

    // the method names are verbose since they will be mixed in to existing classes
    public interface MongoCursorMixin {

        QueryEntry glowroot$getQueryEntry();

        void glowroot$setQueryEntry(QueryEntry queryEntry);
    }

    public static class FirstAdvice {

        public static Timer onBefore(MongoCursorMixin mongoCursor) {
            QueryEntry queryEntry = mongoCursor.glowroot$getQueryEntry();
            return queryEntry == null ? null : queryEntry.extend();
        }

        public static void onReturn(Object document,
                MongoCursorMixin mongoIterable) {
            QueryEntry queryEntry = mongoIterable.glowroot$getQueryEntry();
            if (queryEntry == null) {
                return;
            }
            if (document != null) {
                queryEntry.incrementCurrRow();
            } else {
                queryEntry.rowNavigationAttempted();
            }
        }

        public static void onAfter(Timer timer) {
            if (timer != null) {
                timer.stop();
            }
        }
    }

    public static class IsExhaustedAdvice {

        public static Timer onBefore(MongoCursorMixin mongoCursor) {
            QueryEntry queryEntry = mongoCursor.glowroot$getQueryEntry();
            return queryEntry == null ? null : queryEntry.extend();
        }

        public static void onReturn(MongoCursorMixin mongoCursor) {
            QueryEntry queryEntry = mongoCursor.glowroot$getQueryEntry();
            if (queryEntry == null) {
                // tracing must be disabled (e.g. exceeded trace entry limit)
                return;
            }
            queryEntry.rowNavigationAttempted();
        }

        public static void onAfter(Timer timer) {
            if (timer != null) {
                timer.stop();
            }
        }
    }
}

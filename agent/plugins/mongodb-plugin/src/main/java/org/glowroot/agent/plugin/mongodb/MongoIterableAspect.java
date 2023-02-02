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
import org.glowroot.agent.plugin.mongodb.MongoCursorAspect.MongoCursorMixin;

public class MongoIterableAspect {

    // the field and method names are verbose since they will be mixed in to existing classes
    public static class MongoIterableImpl implements MongoIterableMixin {

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
    public interface MongoIterableMixin {

        QueryEntry glowroot$getQueryEntry();

        void glowroot$setQueryEntry(QueryEntry queryEntry);
    }

    public static class MapAdvice {

        public static void onReturn(MongoIterableMixin newMongoIterable,
                MongoIterableMixin mongoIterable) {
            if (newMongoIterable != null) {
                newMongoIterable.glowroot$setQueryEntry(mongoIterable.glowroot$getQueryEntry());
            }
        }
    }

    public static class FirstAdvice {

        public static Timer onBefore(MongoIterableMixin mongoIterable) {
            QueryEntry queryEntry = mongoIterable.glowroot$getQueryEntry();
            return queryEntry == null ? null : queryEntry.extend();
        }

        public static void onReturn(Object document,
                MongoIterableMixin mongoIterable) {
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

    public static class IteratorAdvice {

        public static void onReturn(MongoCursorMixin mongoCursor,
                MongoIterableMixin mongoIterable) {
            if (mongoCursor != null) {
                mongoCursor.glowroot$setQueryEntry(mongoIterable.glowroot$getQueryEntry());
            }
        }
    }
}

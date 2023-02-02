/*
 * Copyright 2015-2018 the original author or authors.
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
package org.glowroot.agent.tests.app;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class TraceGenerator {

    public abstract String transactionType();
    public abstract String transactionName();
    public abstract String headline();
    public abstract String error();
    public abstract String user();
    public abstract Map<String, String> attributes();

    void call(boolean active) throws InterruptedException {
        if (active) {
            MILLISECONDS.sleep(Long.MAX_VALUE);
        }
    }
}

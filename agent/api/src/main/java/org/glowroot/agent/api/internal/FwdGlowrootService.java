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
package org.glowroot.agent.api.internal;

import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.impl.GlowrootServiceImpl;

public class FwdGlowrootService implements GlowrootService {

    private final GlowrootServiceImpl impl;

    public FwdGlowrootService(GlowrootServiceImpl impl) {
        this.impl = impl;
    }

    @Override
    public void setTransactionType(String transactionType) {
        impl.setTransactionType(transactionType);
    }

    @Override
    public void setTransactionName(String transactionName) {
        impl.setTransactionName(transactionName);
    }

    @Override
    public void setTransactionUser(String user) {
        impl.setTransactionUser(user);
    }

    @Override
    public void addTransactionAttribute(String name, String value) {
        impl.addTransactionAttribute(name, value);
    }

    @Override
    public void setTransactionSlowThreshold(long threshold, TimeUnit unit) {
        impl.setTransactionSlowThreshold(threshold, unit);
    }

    @Override
    public void setTransactionOuter() {
        impl.setTransactionOuter();
    }
}

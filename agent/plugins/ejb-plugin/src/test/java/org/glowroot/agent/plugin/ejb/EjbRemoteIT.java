/**
 * Copyright 2018-2019 the original author or authors.
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
package org.glowroot.agent.plugin.ejb;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.Containers;
import org.glowroot.wire.api.model.TraceOuterClass.Trace;

import static org.assertj.core.api.Assertions.assertThat;

// NOTE EJB Remote instrumentation is currently implemented in core since the hooks needed are not
// yet exposed via the plugin api
public class EjbRemoteIT {

    private static Container container;

    public static void setUp() throws Exception {
        container = Containers.create();
    }

    public static void tearDown() throws Exception {
        container.close();
    }

    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    public void shouldCaptureARemoteBeanOne() throws Exception {
        Trace trace = container.execute(ExecuteARemoteBeanOne.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: ARemoteOne#one");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$ARemoteOne.one()");
    }

    public void shouldCaptureARemoteBeanTwo() throws Exception {
        Trace trace = container.execute(ExecuteARemoteBeanTwo.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: ARemoteTwo#two");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$ARemoteTwo.two()");
    }

    public void shouldCaptureA2RemoteBeanOne() throws Exception {
        Trace trace = container.execute(ExecuteA2RemoteBeanOne.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: ARemoteOne#one");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$ARemoteOne.one()");
    }

    public void shouldCaptureA2RemoteBeanTwo() throws Exception {
        Trace trace = container.execute(ExecuteA2RemoteBeanTwo.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: ARemoteTwo#two");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$ARemoteTwo.two()");
    }

    public void shouldCaptureA3RemoteBeanOne() throws Exception {
        Trace trace = container.execute(ExecuteA3RemoteBeanOne.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: ARemoteOne#one");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$ARemoteOne.one()");
    }

    public void shouldCaptureA3RemoteBeanTwo() throws Exception {
        Trace trace = container.execute(ExecuteA3RemoteBeanTwo.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: ARemoteTwo#two");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$ARemoteTwo.two()");
    }

    public void shouldCaptureBRemoteBeanOne() throws Exception {
        Trace trace = container.execute(ExecuteBRemoteBeanOne.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: BRemoteOne#one");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$BRemoteOne.one()");
    }

    public void shouldCaptureBRemoteBeanTwo() throws Exception {
        Trace trace = container.execute(ExecuteBRemoteBeanTwo.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: BRemoteTwo#two");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$BRemoteTwo.two()");
    }

    public void shouldCaptureB2RemoteBeanOne() throws Exception {
        Trace trace = container.execute(ExecuteB2RemoteBeanOne.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: BRemoteOne#one");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$BRemoteOne.one()");
    }

    public void shouldCaptureB2RemoteBeanTwo() throws Exception {
        Trace trace = container.execute(ExecuteB2RemoteBeanTwo.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: BRemoteTwo#two");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$BRemoteTwo.two()");
    }

    public void shouldCaptureB3RemoteBeanOne() throws Exception {
        Trace trace = container.execute(ExecuteB3RemoteBeanOne.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: BRemoteOne#one");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$BRemoteOne.one()");
    }

    public void shouldCaptureB3RemoteBeanTwo() throws Exception {
        Trace trace = container.execute(ExecuteB3RemoteBeanTwo.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: BRemoteTwo#two");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$BRemoteTwo.two()");
    }

    public void shouldCaptureCRemoteSessionBeanOne() throws Exception {
        Trace trace = container.execute(ExecuteCRemoteSessionBeanOne.class);
        Trace.Header header = trace.getHeader();
        assertThat(header.getTransactionType()).isEqualTo("Web");
        assertThat(header.getTransactionName()).isEqualTo("EJB remote: CRemoteSessionOne#one");
        assertThat(header.getHeadline()).isEqualTo(
                "EJB remote: org.glowroot.agent.plugin.ejb.EjbRemoteIT$CRemoteSessionOne.one()");
    }

    public static class ExecuteARemoteBeanOne implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new ARemoteBean().one();
        }
    }

    public static class ExecuteARemoteBeanTwo implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new ARemoteBean().two(2);
        }
    }

    public static class ExecuteA2RemoteBeanOne implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new A2RemoteBean().one();
        }
    }

    public static class ExecuteA2RemoteBeanTwo implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new A2RemoteBean().two(2);
        }
    }

    public static class ExecuteA3RemoteBeanOne implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new A3RemoteBean().one();
        }
    }

    public static class ExecuteA3RemoteBeanTwo implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new A3RemoteBean().two(2);
        }
    }

    public static class ExecuteBRemoteBeanOne implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new BRemoteBean().one();
        }
    }

    public static class ExecuteBRemoteBeanTwo implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new BRemoteBean().two(2);
        }
    }

    public static class ExecuteB2RemoteBeanOne implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new B2RemoteBean().one();
        }
    }

    public static class ExecuteB2RemoteBeanTwo implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new B2RemoteBean().two(2);
        }
    }

    public static class ExecuteB3RemoteBeanOne implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new B3RemoteBean().one();
        }
    }

    public static class ExecuteB3RemoteBeanTwo implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new B3RemoteBean().two(2);
        }
    }

    public static class ExecuteCRemoteSessionBeanOne implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            new CRemoteSessionBean().one();
        }
    }

    public static class ARemoteBean implements ARemoteOne, ARemoteTwo {
        @Override
        public void one() {}
        @Override
        public void two(int x) {}
    }

    public static class A2RemoteBean extends A2RemoteBase implements ARemoteOne, ARemoteTwo {}

    public static class A2RemoteBase {
        public void one() {}
        public void two(@SuppressWarnings("unused") int x) {}
    }

    public static class A3RemoteBean extends A3RemoteBase {}

    public static class A3RemoteBase implements ARemoteOne, ARemoteTwo {
        @Override
        public void one() {}
        @Override
        public void two(int x) {}
    }

    public interface ARemoteOne {
        String VALUE = Init.value();
        void one();
    }

    public interface ARemoteTwo {
        void two(int x);
    }

    private static class Init {
        private static String value() {
            return "";
        }
    }

    public static class BRemoteBean implements BRemoteOne, BRemoteTwo {
        @Override
        public void one() {}
        @Override
        public void two(int x) {}
    }

    public static class B2RemoteBean extends B2RemoteBase implements BRemoteOne, BRemoteTwo {}

    public static class B2RemoteBase {
        public void one() {}
        public void two(@SuppressWarnings("unused") int x) {}
    }

    public static class B3RemoteBean extends B3RemoteBase {}

    public static class B3RemoteBase implements BRemoteOne, BRemoteTwo {
        @Override
        public void one() {}
        @Override
        public void two(int x) {}
    }

    public interface BRemoteOne {
        void one();
    }

    public interface BRemoteTwo {
        void two(int x);
    }

    public static class CRemoteSessionBean implements CRemoteSessionOne {
        @Override
        public void one() {}
    }

    public interface CRemoteSessionOne extends One {}

    public interface One {
        void one();
    }
}

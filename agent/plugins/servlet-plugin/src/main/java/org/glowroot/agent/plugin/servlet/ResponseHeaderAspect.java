/*
 * Copyright 2014-2019 the original author or authors.
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
package org.glowroot.agent.plugin.servlet;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.checker.Nullable;
import org.glowroot.agent.plugin.api.weaving.BindClassMeta;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReceiver;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.servlet._.ResponseInvoker;
import org.glowroot.agent.plugin.servlet._.ServletMessageSupplier;
import org.glowroot.agent.plugin.servlet._.ServletPluginProperties;

public class ResponseHeaderAspect {

    public static class SetContentLengthAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, int value) {
            if (!ServletPluginProperties.captureContentLengthResponseHeader()) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.setResponseIntHeader("Content-Length", value);
            }
        }
    }

    public static class SetContentLengthLongAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, long value) {
            if (!ServletPluginProperties.captureContentLengthResponseHeader()) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.setResponseLongHeader("Content-Length", value);
            }
        }
    }

    public static class SetContentTypeAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, Object response,
                String value,
                ResponseInvoker responseInvoker) {
            if (value == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            if (!ServletPluginProperties.captureContentTypeResponseHeader()) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                if (responseInvoker.hasGetContentTypeMethod()) {
                    String contentType = responseInvoker.getContentType(response);
                    messageSupplier.setResponseHeader("Content-Type", contentType);
                } else {
                    messageSupplier.setResponseHeader("Content-Type", value);
                }
            }
        }
    }

    public static class SetCharacterEncodingAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, Object response,
                ResponseInvoker responseInvoker) {
            if (!ServletPluginProperties.captureContentTypeResponseHeader()) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null && responseInvoker.hasGetContentTypeMethod()) {
                String contentType = responseInvoker.getContentType(response);
                messageSupplier.setResponseHeader("Content-Type", contentType);
            }
        }
    }

    public static class SetLocaleAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, Object response,
                Locale locale,
                ResponseInvoker responseInvoker) {
            if (locale == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            boolean captureContentLanguage =
                    ServletPluginProperties.captureContentLanguageResponseHeader();
            boolean captureContentType = ServletPluginProperties.captureContentTypeResponseHeader();
            if (!captureContentLanguage && !captureContentType) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                if (captureContentLanguage) {
                    messageSupplier.setResponseHeader("Content-Language", locale.toString());
                }
                if (captureContentType && responseInvoker.hasGetContentTypeMethod()) {
                    String contentType = responseInvoker.getContentType(response);
                    messageSupplier.setResponseHeader("Content-Type", contentType);
                }
            }
        }
    }

    public static class SetHeaderAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, String name,
                String value) {
            if (name == null || value == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            if (!captureResponseHeader(name)) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.setResponseHeader(name, value);
            }
        }
    }

    public static class SetDateHeaderAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, String name,
                long value) {
            if (name == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            if (!captureResponseHeader(name)) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.setResponseDateHeader(name, value);
            }
        }
    }

    public static class SetIntHeaderAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, String name,
                int value) {
            if (name == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            if (!captureResponseHeader(name)) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.setResponseIntHeader(name, value);
            }
        }
    }

    public static class AddHeaderAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, String name,
                String value) {
            if (name == null || value == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            if (!captureResponseHeader(name)) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.addResponseHeader(name, value);
            }
        }
    }

    public static class AddDateHeaderAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, String name,
                long value) {
            if (name == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            if (!captureResponseHeader(name)) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.addResponseDateHeader(name, value);
            }
        }
    }

    public static class AddIntHeaderAdvice {
        public static boolean isEnabled() {
            // good to short-cut advice if no response headers need to be captured
            return ServletPluginProperties.captureResponseHeadersNonEmpty();
        }
        public static void onAfter(ThreadContext context, String name,
                int value) {
            if (name == null) {
                // seems nothing sensible to do here other than ignore
                return;
            }
            if (!captureResponseHeader(name)) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.addResponseIntHeader(name, value);
            }
        }
    }

    private static boolean captureResponseHeader(String name) {
        List<Pattern> capturePatterns = ServletPluginProperties.captureResponseHeaders();
        // converted to lower case for case-insensitive matching (patterns are lower case)
        String keyLowerCase = name.toLowerCase(Locale.ENGLISH);
        return DetailCapture.matchesOneOf(keyLowerCase, capturePatterns);
    }
}

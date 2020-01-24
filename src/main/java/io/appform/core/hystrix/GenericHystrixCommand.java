/*
 * Copyright (c) 2016 Santanu Sinha <santanu.sinha@gmail.com>
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

package io.appform.core.hystrix;

import com.netflix.hystrix.HystrixCommand;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Command that returns the single element
 */
public class GenericHystrixCommand<ReturnType> {

    public final static String TRACE_ID = "TRACE-ID";

    private final HystrixCommand.Setter setter;

    private final String traceId;

    public GenericHystrixCommand(HystrixCommand.Setter setter, String traceId) {
        this.setter = setter;
        this.traceId = traceId;
    }

    public HystrixCommand<ReturnType> executor(HandlerAdapter<ReturnType> function) throws Exception {
        final Map parentMDCContext = MDC.getCopyOfContextMap();
        final Span parentActiveSpan = GlobalTracer.get() != null ? GlobalTracer.get().activeSpan() : null;
        return new HystrixCommand<ReturnType>(setter) {
            @Override
            protected ReturnType run() throws Exception {
                Scope scope = null;
                try {
                    if (parentMDCContext != null){
                        MDC.setContextMap(parentMDCContext);
                    }
                    if (parentActiveSpan != null) {
                        scope = GlobalTracer.get().scopeManager().activate(parentActiveSpan, false);
                    }
                    MDC.put(TRACE_ID, traceId);
                    return function.run();
                } finally {
                    if (scope != null) {
                        scope.close();
                    }
                    MDC.remove(TRACE_ID);
                }
            }
        };
    }
}

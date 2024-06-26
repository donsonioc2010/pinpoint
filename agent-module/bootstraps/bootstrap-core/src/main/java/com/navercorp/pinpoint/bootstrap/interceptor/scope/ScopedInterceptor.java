/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.bootstrap.interceptor.scope;

import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PluginLogManager;
import com.navercorp.pinpoint.bootstrap.logging.PluginLogger;

import java.util.Objects;

/**
 * @author emeroad
 */
public class ScopedInterceptor implements AroundInterceptor {
    private final PluginLogger logger = PluginLogManager.getLogger(getClass());
    private final boolean debugEnabled = logger.isDebugEnabled();

    private final AroundInterceptor interceptor;
    private final InterceptorScope scope;
    private final ExecutionPolicy policy;

    public ScopedInterceptor(AroundInterceptor interceptor, InterceptorScope scope, ExecutionPolicy policy) {
        this.interceptor = Objects.requireNonNull(interceptor, "interceptor");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public void before(Object target, Object[] args) {
        final InterceptorScopeInvocation transaction = scope.getCurrentInvocation();
        
        if (transaction.tryEnter(policy)) {
            interceptor.before(target, args);
        } else {
            if (debugEnabled) {
                logger.debug("tryBefore() returns false: interceptorScopeTransaction: {}, executionPoint: {}. Skip interceptor {}", transaction, policy, interceptor.getClass());
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        final InterceptorScopeInvocation transaction = scope.getCurrentInvocation();
        
        if (transaction.canLeave(policy)) {
            try {
                interceptor.after(target, args, result, throwable);
            } finally {
                transaction.leave(policy);
            }
        } else {
            if (debugEnabled) {
                logger.debug("tryAfter() returns false: interceptorScopeTransaction: {}, executionPoint: {}. Skip interceptor {}", transaction, policy, interceptor.getClass());
            }
        }
    }
}

/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.apm.agent.ecs_logging.jbosslogging;

import co.elastic.apm.agent.ecs_logging.EcsLoggingInstrumentation;
import co.elastic.apm.agent.ecs_logging.EcsLoggingUtils;
import co.elastic.logging.jboss.logmanager.EcsFormatter;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.AssignReturned.ToFields.ToField;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;

import javax.annotation.Nullable;

import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Instruments {@link EcsFormatter#format}
 */
public abstract class JbossEcsServiceInstrumentation extends EcsLoggingInstrumentation {

    @Override
    public ElementMatcher.Junction<? super TypeDescription> getTypeMatcher() {
        return named("co.elastic.logging.jboss.logmanager.EcsFormatter");
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("format");
    }

    public static class Name extends JbossEcsServiceInstrumentation {

        public static class AdviceClass {

            @Nullable
            @Advice.AssignReturned.ToFields(@ToField(value = "serviceName", typing = Assigner.Typing.DYNAMIC))
            @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
            public static String onEnter(@Advice.This Object formatter,
                                         @Advice.FieldValue("serviceName") @Nullable String serviceName) {

                return EcsLoggingUtils.getOrWarnServiceName(formatter, serviceName);
            }
        }

    }

    public static class Version extends JbossEcsServiceInstrumentation {

        @Override
        public ElementMatcher.Junction<? super TypeDescription> getTypeMatcher() {
            return super.getTypeMatcher()
                // setServiceVersion introduced in 1.4.0
                .and(declaresMethod(named("setServiceVersion")));
        }

        public static class AdviceClass {

            @Nullable
            @Advice.AssignReturned.ToFields(@ToField(value = "serviceVersion", typing = Assigner.Typing.DYNAMIC))
            @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
            public static String onEnter(@Advice.This Object formatter,
                                         @Advice.FieldValue("serviceVersion") @Nullable String serviceVersion) {

                return EcsLoggingUtils.getOrWarnServiceVersion(formatter, serviceVersion);
            }
        }

    }

}

/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.method;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Defines the algorithm for searching handler methods exhaustively including interfaces and parent
 * classes while also dealing with parameterized methods as well as interface and class-based proxies.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class HandlerMethodSelector {

	/**
	 *
	 */
	public static Set<Method> selectMethods(final Class<?> handlerType, final MethodFilter handlerMethodFilter) {
		final Set<Method> handlerMethods = new LinkedHashSet<Method>();

		Set<Class<?>> handlerTypes = new LinkedHashSet<Class<?>>();

		Class<?> specificHandlerType = null;

		if (!Proxy.isProxyClass(handlerType)) {
			handlerTypes.add(handlerType);
			specificHandlerType = handlerType;
		}

		//找到handler所实现的所有接口，因为我们需要遍历所有方法
		handlerTypes.addAll(Arrays.asList(handlerType.getInterfaces()));

		for (Class<?> currentHandlerType : handlerTypes) {

			final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);
			ReflectionUtils.doWithMethods(currentHandlerType, new ReflectionUtils.MethodCallback() {

				public void doWith(Method method) {

					Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);

					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

					if (handlerMethodFilter.matches(specificMethod) &&
							(bridgedMethod == specificMethod || !handlerMethodFilter.matches(bridgedMethod))) {
						handlerMethods.add(specificMethod);
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}
		return handlerMethods;
	}

}

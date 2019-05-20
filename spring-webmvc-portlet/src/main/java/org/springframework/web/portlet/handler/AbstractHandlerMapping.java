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

package org.springframework.web.portlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.portlet.HandlerExecutionChain;
import org.springframework.web.portlet.HandlerInterceptor;
import org.springframework.web.portlet.HandlerMapping;

import javax.portlet.PortletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport implements HandlerMapping, Ordered {

	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered

	private Object defaultHandler;

	private final List<Object> interceptors = new ArrayList<Object>();

	private boolean applyWebRequestInterceptorsToRenderPhaseOnly = true;

	private HandlerInterceptor[] adaptedInterceptors;


	/**
	 *
	 */
	public final void setOrder(int order) {
	  this.order = order;
	}

	public final int getOrder() {
	  return this.order;
	}

	/**
	 *
	 */
	public void setDefaultHandler(Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 *
	 */
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 *
	 */
	public void setInterceptors(Object[] interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 */
	public void setApplyWebRequestInterceptorsToRenderPhaseOnly(boolean applyWebRequestInterceptorsToRenderPhaseOnly) {
		this.applyWebRequestInterceptorsToRenderPhaseOnly = applyWebRequestInterceptorsToRenderPhaseOnly;
	}



	@Override
	protected void initApplicationContext() throws BeansException {
		extendInterceptors(this.interceptors);
		initInterceptors();
	}

	/**
	 *
	 */
	protected void extendInterceptors(List<?> interceptors) {
	}

	/**
	 *
	 */
	protected void initInterceptors() {

		if (!this.interceptors.isEmpty()) {
			this.adaptedInterceptors = new HandlerInterceptor[this.interceptors.size()];
			for (int i = 0; i < this.interceptors.size(); i++) {
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				this.adaptedInterceptors[i] = adaptInterceptor(interceptor);
			}
		}
	}

	/**
	 *
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter(
					(WebRequestInterceptor) interceptor, this.applyWebRequestInterceptorsToRenderPhaseOnly);
		}
		else {
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 *
	 */
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		return this.adaptedInterceptors;
	}


	/**
	 *
	 */
	public final HandlerExecutionChain getHandler(PortletRequest request) throws Exception {
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = getApplicationContext().getBean(handlerName);
		}
		return getHandlerExecutionChain(handler, request);
	}

	/**
	 *
	 */
	protected abstract Object getHandlerInternal(PortletRequest request) throws Exception;

	/**
	 *
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, PortletRequest request) {
		if (handler instanceof HandlerExecutionChain) {
			HandlerExecutionChain chain = (HandlerExecutionChain) handler;
			chain.addInterceptors(getAdaptedInterceptors());
			return chain;
		}
		else {
			return new HandlerExecutionChain(handler, getAdaptedInterceptors());
		}
	}

}

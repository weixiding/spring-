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

package org.springframework.web.servlet.mvc.method;

import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 三个接口的实现，分别调用模板方法交给子类去实现
 * 			supportsInternal    handleInternal  getLastModifiedInternal
 */
public abstract class AbstractHandlerMethodAdapter extends WebContentGenerator implements HandlerAdapter, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;


	public AbstractHandlerMethodAdapter() {
		// 不进行请求方法的支持类型检查
		super(false);
	}


	/**
	 * Specify the order value for this HandlerAdapter bean.
	 * <p>Default value is {@code Integer.MAX_VALUE}, meaning that it's non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}


	/**
	 * This implementation expects the handler to be an {@link HandlerMethod}.
	 * @param handler the handler instance to check
	 * @return whether or not this adapter can adapt the given handler
	 */
	public final boolean supports(Object handler) {
		return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));
	}

	/**
	 *
	 */
	protected abstract boolean supportsInternal(HandlerMethod handlerMethod);

	/**
	 *
	 */
	public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return handleInternal(request, response, (HandlerMethod) handler);
	}

	/**
	 * 具体的请求处理交给子类去实现
	 */
	protected abstract ModelAndView handleInternal(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception;

	/**
	 *
	 */
	public final long getLastModified(HttpServletRequest request, Object handler) {
		return getLastModifiedInternal(request, (HandlerMethod) handler);
	}

	/**
	 *
	 */
	protected abstract long getLastModifiedInternal(HttpServletRequest request, HandlerMethod handlerMethod);

}

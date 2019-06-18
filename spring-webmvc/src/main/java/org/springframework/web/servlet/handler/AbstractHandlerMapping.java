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

package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
	AbstractHandlerMapping的作用就是初始化了这三个数据类型，
	并实现了getHandler方法 返回一个chain ,但是具体的handler的查找方法交给了子类去实现getHandlerInternal()
		interceptors
		adaptedInterceptors
		mappedInterceptors
*/
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
		implements HandlerMapping, Ordered {

	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered

	private Object defaultHandler;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final List<Object> interceptors = new ArrayList<Object>();

	private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<HandlerInterceptor>();

	private final List<MappedInterceptor> mappedInterceptors = new ArrayList<MappedInterceptor>();



	public final void setOrder(int order) {
	  this.order = order;
	}

	public final int getOrder() {
	  return this.order;
	}


	public void setDefaultHandler(Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}


	public Object getDefaultHandler() {
		return this.defaultHandler;
	}


	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}


	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}


	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}


	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}


	public UrlPathHelper getUrlPathHelper() {
		return urlPathHelper;
	}


	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}


	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	public void setInterceptors(Object[] interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}


	/**
	 * 为什么会调用这个方法呢，这是应为我们实现的父类中继承了applicationcontext 的aware接口，也就是会调用setapplicaiton'方法
	 * 这个方法会回调initApplicationContext方法，进行我们的初始化工作,在这里重写了父类的方法
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		//抽象方法，供子类实现,用于子类添加拦截器
		extendInterceptors(this.interceptors);
		//用于将spring mvc的容器和父容器中的所有mappedInterceptor类型的bean添加到mappedInterceptors属性中去
		detectMappedInterceptors(this.mappedInterceptors);
		//
		initInterceptors();
	}


	protected void extendInterceptors(List<Object> interceptors) {
	}


	protected void detectMappedInterceptors(List<MappedInterceptor> mappedInterceptors) {
		mappedInterceptors.addAll(
				BeanFactoryUtils.beansOfTypeIncludingAncestors(
						getApplicationContext(), MappedInterceptor.class, true, false).values());
	}


	protected void initInterceptors() {

		/*
			这里主要将MappedInterceptor 分拣为两类，并赋值给指定的属性
			List<HandlerInterceptor> adaptedInterceptors

			List<MappedInterceptor> mappedInterceptors


			interceptors的来源有：
				在xml配置文件中使用属性赋值
				从ioc容器进行抽取

		 */
		if (!this.interceptors.isEmpty()) {
			for (int i = 0; i < this.interceptors.size(); i++) {
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				if (interceptor instanceof MappedInterceptor) {
					this.mappedInterceptors.add((MappedInterceptor) interceptor);
				}
				else {
					this.adaptedInterceptors.add(adaptInterceptor(interceptor));
				}
			}
		}
	}


	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
		}
		else {
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}


	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		int count = this.adaptedInterceptors.size();
		return (count > 0 ? this.adaptedInterceptors.toArray(new HandlerInterceptor[count]) : null);
	}


	protected final MappedInterceptor[] getMappedInterceptors() {
		int count = this.mappedInterceptors.size();
		return (count > 0 ? this.mappedInterceptors.toArray(new MappedInterceptor[count]) : null);
	}


	public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {

		//通过该方法查找handler，这是抽象方法，交给子类完成
		Object handler = getHandlerInternal(request);


		//如果没有获取到则使用默认的handler
		if (handler == null) {

			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		// 如果找到的handler是string类型,则以他为名称到spring ioc容器中查找该bean
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = getApplicationContext().getBean(handlerName);
		}
		return getHandlerExecutionChain(handler, request);
	}


	protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;


	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {

		//创建一个HandlerExecutionChain
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));
		//添加所有AdaptedInterceptors
		chain.addInterceptors(getAdaptedInterceptors());
		//将符合标准的mappedInterceptor也添加进去
		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
		for (MappedInterceptor mappedInterceptor : this.mappedInterceptors) {
			if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
				chain.addInterceptor(mappedInterceptor.getInterceptor());
			}
		}
		//返回chain
		return chain;
	}

}

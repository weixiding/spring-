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

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;

/**
 *		泛型T 的作用是作为匹配条件,,该类实现了获取handler ，注册handler的功能,并通过InitializingBean的afterPropertiesSet方法
 *		进行了注册
 *
 *		子类需要实现getMappingForMethod来生成 request condition
 *				  getMappingPathPatterns 来生成	request condition 所匹配的url路径
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {

	private boolean detectHandlerMethodsInAncestorContexts = false;

	//保存着匹配条件和HandlerMethod的对应关系
	private final Map<T, HandlerMethod> handlerMethods = new LinkedHashMap<T, HandlerMethod>();

	//保存着匹配条件和url的对应关系
	private final MultiValueMap<String, T> urlMap = new LinkedMultiValueMap<String, T>();



	/**
	 *
	 */
	public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
		this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
	}

	/**
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		return Collections.unmodifiableMap(this.handlerMethods);
	}

	/**
	 */
	public void afterPropertiesSet() {
		initHandlerMethods();
	}

	/**
	 *
	 */
	protected void initHandlerMethods() {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for request mappings in application context: " + getApplicationContext());
		}
		//获取springmvc上下文的所有注册的bean
		String[] beanNames = (this.detectHandlerMethodsInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(getApplicationContext(), Object.class) :
				getApplicationContext().getBeanNamesForType(Object.class));

		for (String beanName : beanNames) {

			//判断他是不是handler 类的对象,抽象方法，交给子类去实现
			if (isHandler(getApplicationContext().getType(beanName))){

				//解析其中的HandlerMethod进行注册
				detectHandlerMethods(beanName);
			}
		}

		//抽象方法，目前尚无实现
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
	 *
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 *springmvc解析bean并获取其中的HandlerMethod
	 */
	protected void detectHandlerMethods(final Object handler) {

		//获取当前handler的类型 class
		Class<?> handlerType =
				(handler instanceof String ? getApplicationContext().getType((String) handler) : handler.getClass());

		// Avoid repeated calls to getMappingForMethod which would rebuild RequestMatchingInfo instances
		final Map<Method, T> mappings = new IdentityHashMap<Method, T>();

		//因为有些是CGLIB代理生成的，获取真实的类
		final Class<?> userType = ClassUtils.getUserClass(handlerType);

		Set<Method> methods = HandlerMethodSelector.selectMethods(userType, new MethodFilter() {
			public boolean matches(Method method) {
				//模板方法获取handlerMethod的mapping属性
				T mapping = getMappingForMethod(method, userType);
				if (mapping != null) {
					mappings.put(method, mapping);
					return true;
				}
				else {
					return false;
				}
			}
		});


		//mappings 初始化完成



		//对查找到的HandlerMethod进行注册，保存至内部类mappingRegistry对象中
		for (Method method : methods) {
			/*
				上述的mapping属性指代的是RequestMappingInfo对象，内部包含@RequestMapping注解的内部属性，
				比如method、params、consumes、produces、value以及对应的属性判断类
			 */
			registerHandlerMethod(handler, method, mappings.get(method)/*对应的request condition*/);
		}
	}

	/**
	 *
	 */
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 *
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		HandlerMethod newHandlerMethod = createHandlerMethod(handler, method);



		HandlerMethod oldHandlerMethod = this.handlerMethods.get(mapping);

		//检查当前筛选条件下是否有handlermethod 如果有而且不相等的话，抛出异常
		if (oldHandlerMethod != null && !oldHandlerMethod.equals(newHandlerMethod)) {
			throw new IllegalStateException("Ambiguous mapping found. Cannot map '" + newHandlerMethod.getBean() +
					"' bean method \n" + newHandlerMethod + "\nto " + mapping + ": There is already '" +
					oldHandlerMethod.getBean() + "' bean method\n" + oldHandlerMethod + " mapped.");
		}
		//添加到handler methods中
		this.handlerMethods.put(mapping, newHandlerMethod);
		if (logger.isInfoEnabled()) {
			logger.info("Mapped \"" + mapping + "\" onto " + newHandlerMethod);
		}
		//根据筛选条件生成urls
		Set<String> patterns = getMappingPathPatterns(mapping);
		for (String pattern : patterns) {
			if (!getPathMatcher().isPattern(pattern)) {
				//添加到url map 中
				this.urlMap.add(pattern, mapping);
			}
		}
	}

	/**
	 *
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod;
		if (handler instanceof String) {
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName, getApplicationContext(), method);
		}
		else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		return handlerMethod;
	}

	/**
	 */
	protected abstract Set<String> getMappingPathPatterns(T mapping);

	/**
	 *
	 * 具体的注册交给子类去实现
	 */
	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
	}


	/**
	 *
	 * 获取处理器
	 */
	@Override
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);


		if (logger.isDebugEnabled()) {
			logger.debug("Looking up handler method for path " + lookupPath);
		}

		//重点看一下这个方法
		HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
		if (logger.isDebugEnabled()) {
			if (handlerMethod != null) {
				logger.debug("Returning handler method [" + handlerMethod + "]");
			}
			else {
				logger.debug("Did not find handler method for [" + lookupPath + "]");
			}
		}
		return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
	}

	/**
	 *
	 * 获取handler Method
	 */
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
		//Match是一个内部类，保存 匹配条件和handler
		List<Match> matches = new ArrayList<Match>();

		//找到该路径所匹配的匹配条件的列表
		List<T> directPathMatches = this.urlMap.get(lookupPath);

		if (directPathMatches != null) {

			addMatchingMappings(directPathMatches, matches, request);
		}
		if (matches.isEmpty()) {
			// No choice but to go through all mappings...
			addMatchingMappings(this.handlerMethods.keySet(), matches, request);
		}



		if (!matches.isEmpty()) {
			//根据匹配条件进行排序
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
			Collections.sort(matches, comparator);


			if (logger.isTraceEnabled()) {
				logger.trace("Found " + matches.size() + " matching mapping(s) for [" + lookupPath + "] : " + matches);
			}
			//取到最优的
			Match bestMatch = matches.get(0);
			if (matches.size() > 1) {
				Match secondBestMatch = matches.get(1);
				if (comparator.compare(bestMatch, secondBestMatch) == 0) {
					Method m1 = bestMatch.handlerMethod.getMethod();
					Method m2 = secondBestMatch.handlerMethod.getMethod();
					throw new IllegalStateException(
							"Ambiguous handler methods mapped for HTTP path '" + request.getRequestURL() + "': {" +
							m1 + ", " + m2 + "}");
				}
			}
			handleMatch(bestMatch.mapping, lookupPath, request);
			return bestMatch.handlerMethod;
		}
		else {
			return handleNoMatch(handlerMethods.keySet(), lookupPath, request);
		}
	}

	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, HttpServletRequest request) {
		for (T mapping : mappings) {
			T match = getMatchingMapping(mapping, request);
			if (match != null) {
				matches.add(new Match(match, this.handlerMethods.get(mapping)));
			}
		}
	}

	/**
	 */
	protected abstract T getMatchingMapping(T mapping, HttpServletRequest request);

	/**
	 */
	protected abstract Comparator<T> getMappingComparator(HttpServletRequest request);

	/**
	 */
	protected void handleMatch(T mapping, String lookupPath, HttpServletRequest request) {
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
	}

	/**
	 *
	 */
	protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, HttpServletRequest request)
			throws Exception {

		return null;
	}


	/**
	 *  内部类，用于保存 匹配条件和HandlerMethod
	 */
	private class Match {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		public Match(T mapping, HandlerMethod handlerMethod) {
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	private class MatchComparator implements Comparator<Match> {

		private final Comparator<T> comparator;

		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}

}

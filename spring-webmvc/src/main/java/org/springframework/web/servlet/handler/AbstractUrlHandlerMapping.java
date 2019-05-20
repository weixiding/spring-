/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 *    该类实现的功能是 通过url获取handler 通过url注册handler
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping {

	private Object rootHandler;

	private boolean lazyInitHandlers = false;


	//该属性存放的时handler 与对应的url
	private final Map<String, Object> handlerMap = new LinkedHashMap<String, Object>();


	/**
	 *
	 */
	public void setRootHandler(Object rootHandler) {
		this.rootHandler = rootHandler;
	}

	/**
	 *
	 */
	public Object getRootHandler() {
		return this.rootHandler;
	}

	/**
	 *
	 */
	public void setLazyInitHandlers(boolean lazyInitHandlers) {
		this.lazyInitHandlers = lazyInitHandlers;
	}

	/**
	 *  实现自定义的handler 查找方法
	 */
	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);

		//获取handler的工程就比较复杂了
		Object handler = lookupHandler(lookupPath, request);

		//在map中没有的话
		if (handler == null) {

			Object rawHandler = null;
			//如果是/ 的话设置为root handler
			if ("/".equals(lookupPath)) {
				rawHandler = getRootHandler();
			}
			//如果不是设置为defaulthandler
			if (rawHandler == null) {
				rawHandler = getDefaultHandler();
			}

			if (rawHandler != null) {
				// 如果获得的handler 是string的话，就在ioc容器中获取
				if (rawHandler instanceof String) {
					String handlerName = (String) rawHandler;
					rawHandler = getApplicationContext().getBean(handlerName);
				}

				//可以检验获取的handler 和对应的request匹配,也是模板方法，但是子类并没有实现
				validateHandler(rawHandler, request);

				handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
			}
		}
		if (handler != null && logger.isDebugEnabled()) {
			logger.debug("Mapping [" + lookupPath + "] to " + handler);
		}
		else if (handler == null && logger.isTraceEnabled()) {
			logger.trace("No handler mapping found for [" + lookupPath + "]");
		}
		return handler;
	}

	/**
	 *在map中查找handler
	 */
	protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {

		// 直接在map中查找
		Object handler = this.handlerMap.get(urlPath);
		if (handler != null) {
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = getApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			return buildPathExposingHandler(handler, urlPath, urlPath, null);
		}
		// Pattern match?  正则表达式匹配,获取所有匹配的key
		List<String> matchingPatterns = new ArrayList<String>();
		for (String registeredPattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(registeredPattern, urlPath)) {
				matchingPatterns.add(registeredPattern);
			}
		}
		//找到最合适的handler
		String bestPatternMatch = null;
		Comparator<String> patternComparator = getPathMatcher().getPatternComparator(urlPath);
		if (!matchingPatterns.isEmpty()) {
			Collections.sort(matchingPatterns, patternComparator);
			if (logger.isDebugEnabled()) {
				logger.debug("Matching patterns for request [" + urlPath + "] are " + matchingPatterns);
			}
			bestPatternMatch = matchingPatterns.get(0);
		}


		if (bestPatternMatch != null) {
			handler = this.handlerMap.get(bestPatternMatch);
			// 如果是string 从容器中获取bean
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = getApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestPatternMatch, urlPath);

			// 有可能取不出最优的，这里进行处理
			Map<String, String> uriTemplateVariables = new LinkedHashMap<String, String>();
			for (String matchingPattern : matchingPatterns) {
				if (patternComparator.compare(bestPatternMatch, matchingPattern) == 0) {
					Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, urlPath);
					Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
					uriTemplateVariables.putAll(decodedVars);
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("URI Template variables for request [" + urlPath + "] are " + uriTemplateVariables);
			}

			//该方法用于给找到的handler注册两个拦截器，pathExposingHandlerInterceptor  UriTemplateVariableHandlerInterceptor
			return buildPathExposingHandler(handler, bestPatternMatch, pathWithinMapping, uriTemplateVariables);
		}
		// No handler found...
		return null;
	}

	/**
	 *
	 */
	protected void validateHandler(Object handler, HttpServletRequest request) throws Exception {
	}

	/**
	 * 注册了两个拦截器
	 */
	protected Object buildPathExposingHandler(Object rawHandler, String bestMatchingPattern,
			String pathWithinMapping, Map<String, String> uriTemplateVariables) {

		HandlerExecutionChain chain = new HandlerExecutionChain(rawHandler);
		chain.addInterceptor(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
		if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
			chain.addInterceptor(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
		}
		return chain;
	}

	/**
	 *
	 */
	protected void exposePathWithinMapping(String bestMatchingPattern, String pathWithinMapping, HttpServletRequest request) {
		request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, bestMatchingPattern);
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
	}

	/**
	 *
	 */
	protected void exposeUriTemplateVariables(Map<String, String> uriTemplateVariables, HttpServletRequest request) {
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);
	}

	/**
	 *注册handler到 map中，是子类调用的,,有两个注册方法
	 *
	 * 将多个url映射到一个处理器
	 */
	protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
		Assert.notNull(urlPaths, "URL path array must not be null");
		for (String urlPath : urlPaths) {
			registerHandler(urlPath, beanName);
		}
	}

	/**
	 *将一个url映射到一个处理器
	 */
	protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
		Assert.notNull(urlPath, "URL path must not be null");
		Assert.notNull(handler, "Handler object must not be null");
		Object resolvedHandler = handler;

		// Eagerly resolve handler if referencing singleton via name.
		if (!this.lazyInitHandlers && handler instanceof String) {
			String handlerName = (String) handler;
			if (getApplicationContext().isSingleton(handlerName)) {
				resolvedHandler = getApplicationContext().getBean(handlerName);
			}
		}

		Object mappedHandler = this.handlerMap.get(urlPath);
		if (mappedHandler != null) {
			if (mappedHandler != resolvedHandler) {

				//如果存在两个相同的handler 映射一个url就抛出异常
				throw new IllegalStateException(
						"Cannot map " + getHandlerDescription(handler) + " to URL path [" + urlPath +
						"]: There is already " + getHandlerDescription(mappedHandler) + " mapped.");
			}
		}
		else {
			//如果请求路径是/就放入root handler属性中
			if (urlPath.equals("/")) {
				if (logger.isInfoEnabled()) {
					logger.info("Root mapping to " + getHandlerDescription(handler));
				}
				setRootHandler(resolvedHandler);
			}
			//放入defaulthandler属性中
			else if (urlPath.equals("/*")) {
				if (logger.isInfoEnabled()) {
					logger.info("Default mapping to " + getHandlerDescription(handler));
				}
				setDefaultHandler(resolvedHandler);
			}
			else {
				this.handlerMap.put(urlPath, resolvedHandler);
				if (logger.isInfoEnabled()) {
					logger.info("Mapped URL path [" + urlPath + "] onto " + getHandlerDescription(handler));
				}
			}
		}
	}

	private String getHandlerDescription(Object handler) {
		return "handler " + (handler instanceof String ? "'" + handler + "'" : "of type [" + handler.getClass() + "]");
	}


	/**
	 *
	 */
	public final Map<String, Object> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}

	/**
	 */
	protected boolean supportsTypeLevelMappings() {
		return false;
	}


	/**
	 */
	private class PathExposingHandlerInterceptor extends HandlerInterceptorAdapter {

		private final String bestMatchingPattern;

		private final String pathWithinMapping;

		public PathExposingHandlerInterceptor(String bestMatchingPattern, String pathWithinMapping) {
			this.bestMatchingPattern = bestMatchingPattern;
			this.pathWithinMapping = pathWithinMapping;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			exposePathWithinMapping(this.bestMatchingPattern, this.pathWithinMapping, request);
			request.setAttribute(HandlerMapping.INTROSPECT_TYPE_LEVEL_MAPPING, supportsTypeLevelMappings());
			return true;
		}

	}

	/**
	 */
	private class UriTemplateVariablesHandlerInterceptor extends HandlerInterceptorAdapter {

		private final Map<String, String> uriTemplateVariables;

		public UriTemplateVariablesHandlerInterceptor(Map<String, String> uriTemplateVariables) {
			this.uriTemplateVariables = uriTemplateVariables;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			exposeUriTemplateVariables(this.uriTemplateVariables, request);
			return true;
		}
	}

}

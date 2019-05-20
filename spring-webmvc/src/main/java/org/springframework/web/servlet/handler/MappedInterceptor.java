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

import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 *
 */
public final class MappedInterceptor {

	private final String[] includePatterns;

	private final String[] excludePatterns;

	private final HandlerInterceptor interceptor;


	/**
	 *
	 */
	public MappedInterceptor(String[] includePatterns, HandlerInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**

	 */
	public MappedInterceptor(String[] includePatterns, String[] excludePatterns, HandlerInterceptor interceptor) {
		this.includePatterns = includePatterns;
		this.excludePatterns = excludePatterns;
		this.interceptor = interceptor;
	}

	/**

	 */
	public MappedInterceptor(String[] includePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**

	 */
	public MappedInterceptor(String[] includePatterns, String[] excludePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, excludePatterns, new WebRequestHandlerInterceptorAdapter(interceptor));
	}


	/**
	 */
	public String[] getPathPatterns() {
		return this.includePatterns;
	}

	/**
	 */
	public HandlerInterceptor getInterceptor() {
		return this.interceptor;
	}

	/**
	 */
	public boolean matches(String lookupPath, PathMatcher pathMatcher) {
		if (this.excludePatterns != null) {
			for (String pattern : this.excludePatterns) {
				if (pathMatcher.match(pattern, lookupPath)) {
					return false;
				}
			}
		}
		if (this.includePatterns == null) {
			return true;
		}
		else {
			for (String pattern : this.includePatterns) {
				if (pathMatcher.match(pattern, lookupPath)) {
					return true;
				}
			}
			return false;
		}
	}
}

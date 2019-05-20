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

package org.springframework.web.servlet.mvc.condition;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public interface RequestCondition<T> {

	/**
	 *将不同的筛选条件合并
	 */
	T combine(T other);

	/**
	 *根据request查找匹配到的筛选条件
	 */
	T getMatchingCondition(HttpServletRequest request);

	/**
	 *不同筛选条件比较,用于排序
	 */
	int compareTo(T other, HttpServletRequest request);

}

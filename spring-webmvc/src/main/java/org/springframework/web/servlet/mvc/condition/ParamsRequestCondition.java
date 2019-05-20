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

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * params对应http request parameter
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {
	// 保存解析出来的param匹配条件
	private final Set<ParamExpression> expressions;


	/**
	 * Create a new instance from the given param expressions.
	 * @param params expressions with syntax defined in {@link RequestMapping#params()};
	 * 	if 0, the condition will match to every request.
	 */
	public ParamsRequestCondition(String... params) {
		this(parseExpressions(params));
	}

	private ParamsRequestCondition(Collection<ParamExpression> conditions) {
		this.expressions = Collections.unmodifiableSet(new LinkedHashSet<ParamExpression>(conditions));
	}

	//解析param
	private static Collection<ParamExpression> parseExpressions(String... params) {
		Set<ParamExpression> expressions = new LinkedHashSet<ParamExpression>();
		if (params != null) {
			for (String param : params) {
				expressions.add(new ParamExpression(param));
			}
		}
		return expressions;
	}


	/**
	 * Return the contained request parameter expressions.
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<NameValueExpression<String>>(this.expressions);
	}
	//该方法直接返回匹配条件的集合
	@Override
	protected Collection<ParamExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 合并
	 */
	public ParamsRequestCondition combine(ParamsRequestCondition other) {
		Set<ParamExpression> set = new LinkedHashSet<ParamExpression>(this.expressions);
		set.addAll(other.expressions);
		return new ParamsRequestCondition(set);
	}

	/**
	 * 如果匹配所有的参数表达式的话就返回this
	 * 比如我们对多个参数进行限定的话就会有多个参数限定表达式,只有全部满足，才能判断请求符合条件
	 *
	 */
	public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
		for (ParamExpression expression : expressions) {
			if (!expression.match(request)) {
				return null;
			}
		}
		return this;
	}

	/**
	 *compareTo根据匹配条件的多少来判定顺序
	 */
	public int compareTo(ParamsRequestCondition other, HttpServletRequest request) {
		return (other.expressions.size() - this.expressions.size());
	}


	/**
	 * Parses and matches a single param expression to a request.
	 */
	static class ParamExpression extends AbstractNameValueExpression<String> {

		ParamExpression(String expression) {
			super(expression);
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return WebUtils.hasSubmitParameter(request, name);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return value.equals(request.getParameter(name));
		}
	}

}

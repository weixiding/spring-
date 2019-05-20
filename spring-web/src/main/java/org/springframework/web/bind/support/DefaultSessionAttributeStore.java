/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.bind.support;

import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequest;

/**
 * 用来保存sessionAttribute 参数的工具类
 */
public class DefaultSessionAttributeStore implements SessionAttributeStore {

	private String attributeNamePrefix = "";


	/**
	 * 设置一个属性名的前缀
	 */
	public void setAttributeNamePrefix(String attributeNamePrefix) {
		this.attributeNamePrefix = (attributeNamePrefix != null ? attributeNamePrefix : "");
	}
	//存储session
	//WebRequest 是 spring把我们的http request进行的封装
	public void storeAttribute(WebRequest request, String attributeName, Object attributeValue) {
		//断言
		Assert.notNull(request, "WebRequest must not be null");
		Assert.notNull(attributeName, "Attribute name must not be null");
		Assert.notNull(attributeValue, "Attribute value must not be null");


		String storeAttributeName = getAttributeNameInSession(request, attributeName);
		request.setAttribute(storeAttributeName, attributeValue, WebRequest.SCOPE_SESSION);
	}
	//获取session
	public Object retrieveAttribute(WebRequest request, String attributeName) {
		Assert.notNull(request, "WebRequest must not be null");
		Assert.notNull(attributeName, "Attribute name must not be null");
		String storeAttributeName = getAttributeNameInSession(request, attributeName);
		return request.getAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
	}
	//清除session
	public void cleanupAttribute(WebRequest request, String attributeName) {
		Assert.notNull(request, "WebRequest must not be null");
		Assert.notNull(attributeName, "Attribute name must not be null");
		String storeAttributeName = getAttributeNameInSession(request, attributeName);
		request.removeAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
	}


	/**
	 * 获取在session的参数名称
	 */
	protected String getAttributeNameInSession(WebRequest request, String attributeName) {
		return this.attributeNamePrefix + attributeName;
	}

}

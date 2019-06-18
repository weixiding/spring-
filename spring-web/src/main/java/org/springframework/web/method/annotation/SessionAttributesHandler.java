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

package org.springframework.web.method.annotation;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.context.request.WebRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
knownAttributeNames 参数的设置在两个位置 ：  1  在初始化的时候   2 在 isHandlerSessionAttribute 中,
在 retrieveAttributes的时候会调用isHandlerSessionAttribute  ，如果是type类型的 则回见该类型的参数的名字传递进去

 */
public class SessionAttributesHandler {

	private final Set<String> attributeNames = new HashSet<String>();

	private final Set<Class<?>> attributeTypes = new HashSet<Class<?>>();

	// 该参数的设置在两个位置 ：  1  在初始化的时候   2 在 isHandlerSessionAttribute 中,,该属性的作用是检索工具，可以快速的查找或删除
	private final Map<String, Boolean> knownAttributeNames = new ConcurrentHashMap<String, Boolean>(4);

	private final SessionAttributeStore sessionAttributeStore;


	/**
	 *
	 */
	public SessionAttributesHandler(Class<?> handlerType, SessionAttributeStore sessionAttributeStore) {
		Assert.notNull(sessionAttributeStore, "SessionAttributeStore may not be null.");
		this.sessionAttributeStore = sessionAttributeStore;
		//在这个handler类中找到一个注释 @SessionAttributes
		SessionAttributes annotation = AnnotationUtils.findAnnotation(handlerType, SessionAttributes.class);
		if (annotation != null) {
			//使用注释中的value  和 type给属性赋值
			this.attributeNames.addAll(Arrays.asList(annotation.value()));
			this.attributeTypes.addAll(Arrays.<Class<?>>asList(annotation.types()));
		}

		for (String attributeName : this.attributeNames) {
			//设置已知的属性名称
			this.knownAttributeNames.put(attributeName, Boolean.TRUE);
		}
	}

	/**
	 * Whether the controller represented by this instance has declared any
	 * session attributes through an {@link SessionAttributes} annotation.
	 */
	public boolean hasSessionAttributes() {
		return ((this.attributeNames.size() > 0) || (this.attributeTypes.size() > 0));
	}

	/**
	 *用于判断 是否需要缓存，如果需要的话返回true 并将参数名称放入 knownAttributeNames中
	 */
	public boolean isHandlerSessionAttribute(String attributeName, Class<?> attributeType) {
		Assert.notNull(attributeName, "Attribute name must not be null");
		if (this.attributeNames.contains(attributeName) || this.attributeTypes.contains(attributeType)) {
			this.knownAttributeNames.put(attributeName, Boolean.TRUE);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 对外的保存 session attribute 的方法
	 */
	public void storeAttributes(WebRequest request, Map<String, ?> attributes) {
		for (String name : attributes.keySet()) {
			Object value = attributes.get(name);
			Class<?> attrType = (value != null) ? value.getClass() : null;

			//调用他来判断是否要缓存 ，如果是的话，进行参数名称的缓存,,
			if (isHandlerSessionAttribute(name, attrType)) {
				this.sessionAttributeStore.storeAttribute(request, name, value);
			}
		}
	}

	/**
	 * 获取 session attribute 的属性,,通过 knownAttributeNames进行全部的检索
	 */
	public Map<String, Object> retrieveAttributes(WebRequest request) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		for (String name : this.knownAttributeNames.keySet()) {
			Object value = this.sessionAttributeStore.retrieveAttribute(request, name);
			if (value != null) {
				attributes.put(name, value);
			}
		}
		return attributes;
	}

	/**
	 * 通过knownAttributeNames检索进行全部的删除
	 */
	public void cleanupAttributes(WebRequest request) {
		for (String attributeName : this.knownAttributeNames.keySet()) {
			this.sessionAttributeStore.cleanupAttribute(request, attributeName);
		}
	}

	/**
	 * 内部方法
	 */
	Object retrieveAttribute(WebRequest request, String attributeName) {
		return this.sessionAttributeStore.retrieveAttribute(request, attributeName);
	}

}

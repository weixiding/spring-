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
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {

	//封装我们的路径和处理器的对应关系    ,,,,,是在属性赋值的
	private final Map<String, Object> urlMap = new HashMap<String, Object>();


	/**
	 *
	 */
	public void setMappings(Properties mappings) {
		CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
	}

	/**
	 *
	 */
	public void setUrlMap(Map<String, ?> urlMap) {
		this.urlMap.putAll(urlMap);
	}

	/**
	 *
	 */
	public Map<String, ?> getUrlMap() {
		return this.urlMap;
	}


	/**
	 * 此处重写了该方法，并调用了父类的方法
	 */
	@Override
	public void initApplicationContext() throws BeansException {

		super.initApplicationContext();

		//注册
		registerHandlers(this.urlMap);
	}

	/**
	 *
	 */
	protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
		if (urlMap.isEmpty()) {
			logger.warn("Neither 'urlMap' nor 'mappings' set on SimpleUrlHandlerMapping");
		}
		else {
			for (Map.Entry<String, Object> entry : urlMap.entrySet()) {
				String url = entry.getKey();
				Object handler = entry.getValue();
				// Prepend with slash if not already present.
				if (!url.startsWith("/")) {
					url = "/" + url;
				}
				// Remove whitespace from handler bean name.
				if (handler instanceof String) {
					handler = ((String) handler).trim();
				}
				registerHandler(url, handler);
			}
		}
	}

}

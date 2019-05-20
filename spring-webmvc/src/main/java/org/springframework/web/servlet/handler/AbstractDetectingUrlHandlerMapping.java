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
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContextException;
import org.springframework.util.ObjectUtils;

/**
 * 实现注册handler的功能，但是据体的url构建的过程还是交给了子类去完成的
 */
public abstract class AbstractDetectingUrlHandlerMapping extends AbstractUrlHandlerMapping {

	private boolean detectHandlersInAncestorContexts = false;


	/**
	 *
	 */
	public void setDetectHandlersInAncestorContexts(boolean detectHandlersInAncestorContexts) {
		this.detectHandlersInAncestorContexts = detectHandlersInAncestorContexts;
	}


	/**
	 * 重写父类的方法
	 */
	@Override
	public void initApplicationContext() throws ApplicationContextException {
		//基本功能是初始化拦截器
		super.initApplicationContext();

		detectHandlers();
	}

	/**
	 *
	 */
	protected void detectHandlers() throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for URL mappings in application context: " + getApplicationContext());
		}

		//获取spring ioc容器中所有bean的名字
		String[] beanNames = (this.detectHandlersInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(getApplicationContext(), Object.class) :
				getApplicationContext().getBeanNamesForType(Object.class));

		// Take any bean name that we can determine URLs for.
		for (String beanName : beanNames) {
			//对每个bean进行解析，生成urls如果有的话就注册到父类的map中去,,模板方法交给子类去实现
			String[] urls = determineUrlsForHandler(beanName);
			if (!ObjectUtils.isEmpty(urls)) {
				// URL paths found: Let's consider it a handler.
				registerHandler(urls, beanName);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Rejected bean name '" + beanName + "': no URL paths identified");
				}
			}
		}
	}


	/**
	 * 根据beanName进行解析，生成URl
	 *
	 */
	protected abstract String[] determineUrlsForHandler(String beanName);

}

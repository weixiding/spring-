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

package org.springframework.web.servlet.mvc.support;

import org.springframework.web.servlet.handler.AbstractDetectingUrlHandlerMapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public abstract class AbstractControllerUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping  {

	private ControllerTypePredicate predicate = new AnnotationControllerTypePredicate();

	private Set<String> excludedPackages = Collections.singleton("org.springframework.web.servlet.mvc");

	private Set<Class<?>> excludedClasses = Collections.emptySet();


	/**
	 * Set whether to activate or deactivate detection of annotated controllers.
	 */
	public void setIncludeAnnotatedControllers(boolean includeAnnotatedControllers) {
		this.predicate = (includeAnnotatedControllers ?
				new AnnotationControllerTypePredicate() : new ControllerTypePredicate());
	}

	/**
	 * 设置排除指定的包
	 */
	public void setExcludedPackages(String... excludedPackages) {
		this.excludedPackages = (excludedPackages != null) ?
				new HashSet<String>(Arrays.asList(excludedPackages)) : new HashSet<String>();
	}

	/**
	 * 设置排除指定的类
	 */
	public void setExcludedClasses(Class<?>... excludedClasses) {
		this.excludedClasses = (excludedClasses != null) ?
				new HashSet<Class<?>>(Arrays.asList(excludedClasses)) : new HashSet<Class<?>>();
	}


	/**
	 * 对于实现了controller接口或者注解的类进行URl的解析
	 */
	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		Class<?> beanClass = getApplicationContext().getType(beanName);
		if (isEligibleForMapping(beanName, beanClass)) {
			//将符合调价的bean找出来，构建url,但是具体的构架过程还是交给了子类去实现
			return buildUrlsForHandler(beanName, beanClass);
		}
		else {
			return null;
		}
	}

	/**
	 * 查找
	 */
	protected boolean isEligibleForMapping(String beanName, Class<?> beanClass) {

		if (beanClass == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Excluding controller bean '" + beanName + "' from class name mapping " +
						"because its bean type could not be determined");
			}
			return false;
		}

		//如果是要排除的类
		if (this.excludedClasses.contains(beanClass)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Excluding controller bean '" + beanName + "' from class name mapping " +
						"because its bean class is explicitly excluded: " + beanClass.getName());
			}
			return false;
		}


		String beanClassName = beanClass.getName();
		//如果是要排除的包
		for (String packageName : this.excludedPackages) {
			if (beanClassName.startsWith(packageName)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Excluding controller bean '" + beanName + "' from class name mapping " +
							"because its bean class is defined in an excluded package: " + beanClass.getName());
				}
				return false;
			}
		}


		return isControllerType(beanClass);
	}

	/**
	 * Determine whether the given bean class indicates a controller type
	 * that is supported by this mapping strategy.
	 * @param beanClass the class to introspect
	 */
	protected boolean isControllerType(Class<?> beanClass) {
		return this.predicate.isControllerType(beanClass);
	}

	/**
	 * Determine whether the given bean class indicates a controller type
	 * that dispatches to multiple action methods.
	 * @param beanClass the class to introspect
	 */
	protected boolean isMultiActionControllerType(Class<?> beanClass) {
		return this.predicate.isMultiActionControllerType(beanClass);
	}


	/**
	 * Abstract template method to be implemented by subclasses.
	 * @param beanName the name of the bean
	 * @param beanClass the type of the bean
	 * @return the URLs determined for the bean
	 */
	protected abstract String[] buildUrlsForHandler(String beanName, Class<?> beanClass);

}

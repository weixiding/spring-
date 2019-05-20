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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * bean实例化策略接口
 */
public interface InstantiationStrategy {

	/**
	 * Return an instance of the bean with the given name in this factory.
		使用默认的构造函数创建
	 */
	Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner)
			throws BeansException;

	/**
	 * Return an instance of the bean with the given name in this factory,
	 * 通过制定的构造函数创建bean
	 */
	Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
			Constructor<?> ctor, Object[] args) throws BeansException;

	/**
	 * Return an instance of the bean with the given name in this factory,
	 * 通过工厂方法创建

	 */
	Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
			Object factoryBean, Method factoryMethod, Object[] args) throws BeansException;

}

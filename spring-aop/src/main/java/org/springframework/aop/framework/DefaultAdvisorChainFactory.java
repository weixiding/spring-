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

package org.springframework.aop.framework;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.support.MethodMatchers;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 生成拦截器链的工厂
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {


	/**
	 * 从提供的配置实例config中获取advisor列表,遍历处理这些advisor.如果是IntroductionAdvisor,
	 * 则判断此Advisor能否应用到目标类targetClass上.如果是PointcutAdvisor,则判断
	 * 此Advisor能否应用到目标方法method上.将满足条件的Advisor通过AdvisorAdaptor转化成Interceptor列表返回.
	 */
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, Class targetClass) {

		// This is somewhat tricky... we have to process introductions first,
		// but we need to preserve order in the ultimate list.


		//创建一个list对象，他是由配置的通知器的个数决定的
		List<Object> interceptorList = new ArrayList<Object>(config.getAdvisors().length);

		//查看是否包含IntroductionAdvisor  即引介增强
		boolean hasIntroductions = hasMatchingIntroductions(config, targetClass);

		//这里实际上注册一系列AdvisorAdapter,用于将Advisor转化成MethodInterceptor
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();

		//循环遍历切面
		for (Advisor advisor : config.getAdvisors()) {

			if (advisor instanceof PointcutAdvisor) {
				// Add it conditionally.
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				//使我们这个类的曾强
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(targetClass)) {

					//将Advisor转化成Interceptor
					MethodInterceptor[] interceptors = registry.getInterceptors(advisor);

					//获取方法匹配器
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					//
					if (MethodMatchers.matches(mm, method, targetClass, hasIntroductions)) {
						//如果是动态方法匹配的话
						if (mm.isRuntime()) {
							for (MethodInterceptor interceptor : interceptors) {
								//添加类型为InterceptorAndDynamicMethodMatcher的动态方法匹配，
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						//静态方法匹配如果能够匹配上的话就机型封装，并添加到list中
						else {
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
			/*
					引介增强
			 */
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;

				if (config.isPreFiltered() || ia.getClassFilter().matches(targetClass)) {

					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}
		return interceptorList;

		//这个方法执行完成后，Advised中配置能够应用到连接点或者目标类的Advisor全部被转化成了MethodInterceptor.
	}

	/**
	 * Determine whether the Advisors contain matching introductions.
	 */
	private static boolean hasMatchingIntroductions(Advised config, Class targetClass) {
		for (int i = 0; i < config.getAdvisors().length; i++) {
			Advisor advisor = config.getAdvisors()[i];
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(targetClass)) {
					return true;
				}
			}
		}
		return false;
	}

}

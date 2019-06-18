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

import org.springframework.beans.BeanUtils;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *包含两个功能：
 * 	1 初始化 model
 * 	2 处理器执行后将model中响应的参数更新到session attribute中
 */
public final class ModelFactory {

	private final List<InvocableHandlerMethod> attributeMethods;

	private final WebDataBinderFactory binderFactory;

	private final SessionAttributesHandler sessionAttributesHandler;

	/**
	 * Create a new instance with the given {@code @ModelAttribute} methods.
	 * @param attributeMethods for model initialization
	 * @param binderFactory for adding {@link BindingResult} attributes
	 * @param sessionAttributesHandler for access to session attributes
	 */
	public ModelFactory(List<InvocableHandlerMethod> attributeMethods,
						WebDataBinderFactory binderFactory,
						SessionAttributesHandler sessionAttributesHandler) {
		this.attributeMethods = (attributeMethods != null) ? attributeMethods : new ArrayList<InvocableHandlerMethod>();
		this.binderFactory = binderFactory;
		this.sessionAttributesHandler = sessionAttributesHandler;
	}

	/**
	 * 	初始化 model
	 * 		从sessionAttributesHandler中初始化
	 * 		从	@modelAttribute注释的方法中初始化
	 * 		从	释了 @@modelAttribute 又在 @sessionAttribute 中的参数初始化
	 */
	public void initModel(NativeWebRequest request, ModelAndViewContainer mavContainer, HandlerMethod handlerMethod)
			throws Exception {
		//从 session attribute 中取出参数 保存到ModelAndViewContainer 中
		Map<String, ?> attributesInSession = this.sessionAttributesHandler.retrieveAttributes(request);
		mavContainer.mergeAttributes(attributesInSession);

		//调用 @modelAttribute 注解的方法并将结果设置到model中
		invokeModelAttributeMethods(request, mavContainer);

		//遍历即注释了 @@modelAttribute 又在 @sessionAttribute 中的参数
		for (String name : findSessionAttributeArguments(handlerMethod)) {
			if (!mavContainer.containsAttribute(name)) {
				//注意这个是从全局的session中获取的
				Object value = this.sessionAttributesHandler.retrieveAttribute(request, name);
				if (value == null) {
					throw new HttpSessionRequiredException("Expected session attribute '" + name + "'");
				}
				mavContainer.addAttribute(name, value);
			}
		}
	}

	/**
	 *
	 */
	private void invokeModelAttributeMethods(NativeWebRequest request, ModelAndViewContainer mavContainer)
			throws Exception {

		for (InvocableHandlerMethod attrMethod : this.attributeMethods) {
			String modelName = attrMethod.getMethodAnnotation(ModelAttribute.class).value();
			//如果参数名已经在其中 ，则跳过
			if (mavContainer.containsAttribute(modelName)) {
				continue;
			}
			//执行方法
			Object returnValue = attrMethod.invokeForRequest(request, mavContainer);

			if (!attrMethod.isVoid()){
				//解析即将放入到 mavcontainer 中的属性的名称
				String returnValueName = getNameForReturnValue(returnValue, attrMethod.getReturnType());
				if (!mavContainer.containsAttribute(returnValueName)) {
					mavContainer.addAttribute(returnValueName, returnValue);
				}
			}
		}
	}

	/**
	 * Return all {@code @ModelAttribute} arguments declared as session
	 * attributes via {@code @SessionAttributes}.
	 */
	private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
		List<String> result = new ArrayList<String>();
		for (MethodParameter param : handlerMethod.getMethodParameters()) {
			if (param.hasParameterAnnotation(ModelAttribute.class)) {

				//重点关注的方法
				String name = getNameForParameter(param);
				if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, param.getParameterType())) {
					result.add(name);
				}
			}
		}
		return result;
	}

	/**
	 * 获取参数名称的规则解析
	 */
	public static String getNameForReturnValue(Object returnValue, MethodParameter returnType) {
		ModelAttribute annot = returnType.getMethodAnnotation(ModelAttribute.class);
		if (annot != null && StringUtils.hasText(annot.value())) {
			//如果设置了value  则直接将value 的值进行返回
			return annot.value();
		}
		else {
			Method method = returnType.getMethod();
			Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, returnType.getDeclaringClass());
			//如果是 type的话，进行解析
			return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
		}
	}

	/**
	 * Derives the model attribute name for a method parameter based on:
	 * <ol>
	 * 	<li>The parameter {@code @ModelAttribute} annotation value
	 * 	<li>The parameter type
	 * </ol>
	 * @return the derived name; never {@code null} or an empty string
	 */
	public static String getNameForParameter(MethodParameter parameter) {
		ModelAttribute annot = parameter.getParameterAnnotation(ModelAttribute.class);
		//如果是 @sessionAttribute 的value 中的值的话直接返回，如果的type 的话进行名称的解析
		String attrName = (annot != null) ? annot.value() : null;
		return StringUtils.hasText(attrName) ? attrName :  Conventions.getVariableNameForParameter(parameter);
	}

	/**
	 *  1 对session进行设置
	 *  2 判断请求是否已经处理完成或者是 redirect类型的返回值，其实就是判断需不需要进行页面渲染
	 **/
	public void updateModel(NativeWebRequest request, ModelAndViewContainer mavContainer) throws Exception {

		if (mavContainer.getSessionStatus().isComplete()){
			this.sessionAttributesHandler.cleanupAttributes(request);
		}
		else {
			this.sessionAttributesHandler.storeAttributes(request, mavContainer.getModel());
		}

		if (!mavContainer.isRequestHandled()) {
			//如果需要渲染   则给相应的参数设置binding Result
			updateBindingResult(request, mavContainer.getModel());
		}
	}

	/**
	 * Add {@link BindingResult} attributes to the model for attributes that require it.
	 */
	private void updateBindingResult(NativeWebRequest request, ModelMap model) throws Exception {
		List<String> keyNames = new ArrayList<String>(model.keySet());
		for (String name : keyNames) {
			Object value = model.get(name);
			//判断是否需要添加binding result
			if (isBindingCandidate(name, value)) {
				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;

				if (!model.containsAttribute(bindingResultKey)) {
					WebDataBinder dataBinder = binderFactory.createBinder(request, value, name);
					model.put(bindingResultKey, dataBinder.getBindingResult());
				}
			}
		}
	}

	/**
	 * Whether the given attribute requires a {@link BindingResult} in the model.
	 */
	private boolean isBindingCandidate(String attributeName, Object value) {
		//先判断是不是其他结果的 binding result ，通过参数名前缀判断
		if (attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
			return false;
		}
		//判断是不是session管理的属性
		Class<?> attrType = (value != null) ? value.getClass() : null;
		if (this.sessionAttributesHandler.isHandlerSessionAttribute(attributeName, attrType)) {
			return true;
		}
		//最后检查不是null 数组 集合 map 和基本数据类型的话返回 true
		return (value != null && !value.getClass().isArray() && !(value instanceof Collection) &&
				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
	}

}

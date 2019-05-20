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

package org.springframework.web.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.EventResponse;
import javax.portlet.EventRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceRequest;

/**
 *
 */
public interface HandlerInterceptor {

	/**
	 *
	 */
	boolean preHandleAction(ActionRequest request, ActionResponse response, Object handler)
			throws Exception;

	/**
	 *
	 */
	void afterActionCompletion(
			ActionRequest request, ActionResponse response, Object handler, Exception ex)
			throws Exception;

	/**
	 *
	 */
	boolean preHandleRender(RenderRequest request, RenderResponse response, Object handler)
			throws Exception;

	/**
	 *
	 */
	void postHandleRender(
			RenderRequest request, RenderResponse response, Object handler, ModelAndView modelAndView)
			throws Exception;

	/**
	 *
	 */
	void afterRenderCompletion(
			RenderRequest request, RenderResponse response, Object handler, Exception ex)
			throws Exception;

	/**
	 *
	 */
	boolean preHandleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws Exception;

	/**
	 *
	 */
	void postHandleResource(
			ResourceRequest request, ResourceResponse response, Object handler, ModelAndView modelAndView)
			throws Exception;

	/**
	 *
	 */
	void afterResourceCompletion(
			ResourceRequest request, ResourceResponse response, Object handler, Exception ex)
			throws Exception;


	/**
	 *
	 */
	boolean preHandleEvent(EventRequest request, EventResponse response, Object handler)
			throws Exception;

	/**
	 *
	 */
	void afterEventCompletion(
			EventRequest request, EventResponse response, Object handler, Exception ex)
			throws Exception;

}

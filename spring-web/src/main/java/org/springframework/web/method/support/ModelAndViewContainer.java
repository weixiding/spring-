/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.method.support;

import org.springframework.ui.ModelMap;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.SimpleSessionStatus;

import java.util.Map;

public class ModelAndViewContainer {
	//如果为true 则在处理器返回redirect视图时，一定不使用 defaultModel
	private boolean ignoreDefaultModelOnRedirect = false;
	//视图 可以是string类型的逻辑视图，也可以是实际视图
	private Object view;
	//默认使用的model
	private final ModelMap defaultModel = new BindingAwareModelMap();
	// redirect类型的model
	private ModelMap redirectModel;
	//处理器返回redirect 视图的标志
	private boolean redirectModelScenario = false;
	//用于设置session attribute 使用完的标志
	private final SessionStatus sessionStatus = new SimpleSessionStatus();
	//请求是否已经处理完成的标志
	private boolean requestHandled = false;



	public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
		this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
	}

	public void setViewName(String viewName) {
		this.view = viewName;
	}


	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}


	public void setView(Object view) {
		this.view = view;
	}


	public Object getView() {
		return this.view;
	}


	public boolean isViewReference() {
		return (this.view instanceof String);
	}


	public ModelMap getModel() {
		if (useDefaultModel()) {
			return this.defaultModel;
		}
		else {
			if (this.redirectModel == null) {
				this.redirectModel = new ModelMap();
			}
			return this.redirectModel;
		}
	}


	private boolean useDefaultModel() {
		return (!this.redirectModelScenario || (this.redirectModel == null && !this.ignoreDefaultModelOnRedirect));
	}


	public void setRedirectModel(ModelMap redirectModel) {
		this.redirectModel = redirectModel;
	}

	public void setRedirectModelScenario(boolean redirectModelScenario) {
		this.redirectModelScenario = redirectModelScenario;
	}


	public SessionStatus getSessionStatus() {
		return this.sessionStatus;
	}


	public void setRequestHandled(boolean requestHandled) {
		this.requestHandled = requestHandled;
	}


	public boolean isRequestHandled() {
		return this.requestHandled;
	}


	public ModelAndViewContainer addAttribute(String name, Object value) {
		getModel().addAttribute(name, value);
		return this;
	}

	public ModelAndViewContainer addAttribute(Object value) {
		getModel().addAttribute(value);
		return this;
	}


	public ModelAndViewContainer addAllAttributes(Map<String, ?> attributes) {
		getModel().addAllAttributes(attributes);
		return this;
	}


	public ModelAndViewContainer mergeAttributes(Map<String, ?> attributes) {
		getModel().mergeAttributes(attributes);
		return this;
	}


	public ModelAndViewContainer removeAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				getModel().remove(key);
			}
		}
		return this;
	}

	public boolean containsAttribute(String name) {
		return getModel().containsAttribute(name);
	}



	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ModelAndViewContainer: ");
		if (!isRequestHandled()) {
			if (isViewReference()) {
				sb.append("reference to view with name '").append(this.view).append("'");
			}
			else {
				sb.append("View is [").append(this.view).append(']');
			}
			if (useDefaultModel()) {
				sb.append("; default model ");
			}
			else {
				sb.append("; redirect model ");
			}
			sb.append(getModel());
		}
		else {
			sb.append("Request handled directly");
		}
		return sb.toString();
	}

}

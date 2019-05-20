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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;

/**
 *
 */
public abstract class ApplicationObjectSupport implements ApplicationContextAware {

	/** Logger that is available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** ApplicationContext this object runs in */
	private ApplicationContext applicationContext;

	/** MessageSourceAccessor for easy message access */
	private MessageSourceAccessor messageSourceAccessor;


	public final void setApplicationContext(ApplicationContext context) throws BeansException {
		if (context == null && !isContextRequired()) {
			// Reset internal context state.
			this.applicationContext = null;
			this.messageSourceAccessor = null;
		}
		else if (this.applicationContext == null) {
			// Initialize with passed-in context.
			if (!requiredContextClass().isInstance(context)) {
				throw new ApplicationContextException(
						"Invalid application context: needs to be of type [" + requiredContextClass().getName() + "]");
			}
			this.applicationContext = context;
			this.messageSourceAccessor = new MessageSourceAccessor(context);
			initApplicationContext(context);
		}
		else {
			// Ignore reinitialization if same context passed in.
			if (this.applicationContext != context) {
				throw new ApplicationContextException(
						"Cannot reinitialize with different application context: current one is [" +
						this.applicationContext + "], passed-in one is [" + context + "]");
			}
		}
	}

	/**
	 */
	protected boolean isContextRequired() {
		return false;
	}

	/**
	 *
	 */
	protected Class requiredContextClass() {
		return ApplicationContext.class;
	}

	/**
	 *
	 */
	protected void initApplicationContext(ApplicationContext context) throws BeansException {
		//初始化方法，在子类中实现，记住，这是无参的构造方法
		initApplicationContext();
	}

	/**
	 *
	 */
	protected void initApplicationContext() throws BeansException {
	}


	/**
	 *
	 */
	public final ApplicationContext getApplicationContext() throws IllegalStateException {
		if (this.applicationContext == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.applicationContext;
	}

	/**
	 * Return a MessageSourceAccessor for the application context
	 * used by this object, for easy message access.
	 * @throws IllegalStateException if not running in an ApplicationContext
	 */
	protected final MessageSourceAccessor getMessageSourceAccessor() throws IllegalStateException {
		if (this.messageSourceAccessor == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.messageSourceAccessor;
	}

}

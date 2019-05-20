/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 	该类没有任何自定义方法，只有模板设计模式中重写的getresourcepath方法。该类的核心代码在refresh上面，这会进行beandefinition的加载工作。
 */
public class FileSystemXmlApplicationContext extends AbstractXmlApplicationContext {


	public FileSystemXmlApplicationContext() {
	}


	public FileSystemXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}


	public FileSystemXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[] {configLocation}, true, null);
	}


	public FileSystemXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}


	public FileSystemXmlApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException {
		this(configLocations, true, parent);
	}


	public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/*
		分析工作:bean的加载过程
			调用refresh方法，该方法在父类AbstractApplicationContext中实现
	 */
	public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent)
			throws BeansException {

		super(parent);
		setConfigLocations(configLocations);
		if (refresh) {
			//加载bean，该函数启动bean的加载过程
			refresh();
		}
	}

	/*
			模板设计模式，我们重写了父类 defaultresourceloader 的资源加载方式,实现不通过的资源加载途径
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		if (path != null && path.startsWith("/")) {
			path = path.substring(1);
		}
		return new FileSystemResource(path);
	}

}

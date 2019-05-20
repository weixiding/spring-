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

package org.springframework.web.context.request;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
public interface WebRequest extends RequestAttributes {


	String getHeader(String headerName);


	String[] getHeaderValues(String headerName);


	Iterator<String> getHeaderNames();


	String getParameter(String paramName);


	String[] getParameterValues(String paramName);


	Iterator<String> getParameterNames();


	Map<String, String[]> getParameterMap();


	Locale getLocale();


	String getContextPath();


	String getRemoteUser();


	Principal getUserPrincipal();


	boolean isUserInRole(String role);


	boolean isSecure();


	boolean checkNotModified(long lastModifiedTimestamp);


	boolean checkNotModified(String etag);


	String getDescription(boolean includeClientInfo);

}

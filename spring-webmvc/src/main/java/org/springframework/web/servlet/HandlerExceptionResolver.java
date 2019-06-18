/*
 *
 */

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 异常处理解析的类
 *
 * 异常解析主要包含两部分内容，1 给 modelAndView 设置内容 2 设置 response 的相关内容
 */
public interface HandlerExceptionResolver {

	/**
	 *处理异常，最终返回一个ModelAndView
	 */
	ModelAndView resolveException(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);

}

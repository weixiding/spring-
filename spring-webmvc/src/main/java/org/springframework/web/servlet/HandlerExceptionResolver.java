/*
 *
 */

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 异常处理解析的类
 */
public interface HandlerExceptionResolver {

	/**
	 *处理异常，最终返回一个ModelAndView
	 */
	ModelAndView resolveException(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);

}


package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.util.NestedServletException;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/*

	对 @responseStatus 注释的支持
	对返回值的处理
	对异步处理结果的处理
 */

public class ServletInvocableHandlerMethod extends InvocableHandlerMethod {

	private HttpStatus responseStatus;

	private String responseReason;

	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;


	/**
	 * Creates an instance from the given handler and method.
	 */
	public ServletInvocableHandlerMethod(Object handler, Method method) {
		super(handler, method);
		initResponseStatus();
	}

	/**
	 * Create an instance from a {@code HandlerMethod}.
	 */
	public ServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
		initResponseStatus();
	}

	private void initResponseStatus() {
		ResponseStatus annot = getMethodAnnotation(ResponseStatus.class);
		if (annot != null) {
			this.responseStatus = annot.value();
			this.responseReason = annot.reason();
		}
	}

	/**
	 * Register {@link HandlerMethodReturnValueHandler} instances to use to
	 * handle return values.
	 */
	public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite returnValueHandlers) {
		this.returnValueHandlers = returnValueHandlers;
	}

	/**
	 * 该方法核心处理逻辑是 调用returnValueHandlers.handleReturnValue 进行处理
	 */
	public final void invokeAndHandle(ServletWebRequest webRequest,
			ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {

		Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
		//处理 @responseStatus 注释
		setResponseStatus(webRequest);

		if (returnValue == null) {
			//如果返回值为null 并且 其中任意条件为其中任意一个直接返回
			if (isRequestNotModified(webRequest) || hasResponseStatus() || mavContainer.isRequestHandled()) {
				mavContainer.setRequestHandled(true);
				return;
			}
		}
		else if (StringUtils.hasText(this.responseReason)) {
			mavContainer.setRequestHandled(true);
			return;
		}

		mavContainer.setRequestHandled(false);

		try {
			this.returnValueHandlers.handleReturnValue(returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
		}
		catch (Exception ex) {
			if (logger.isTraceEnabled()) {
				logger.trace(getReturnValueHandlingErrorMessage("Error handling return value", returnValue), ex);
			}
			throw ex;
		}
	}

	/**
	 * Set the response status according to the {@link ResponseStatus} annotation.
	 */
	private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
		if (this.responseStatus == null) {
			return;
		}

		if (StringUtils.hasText(this.responseReason)) {
			webRequest.getResponse().sendError(this.responseStatus.value(), this.responseReason);
		}
		else {
			webRequest.getResponse().setStatus(this.responseStatus.value());
		}

		// 设置在request中的属性，为了在 redirect中使用
		webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, this.responseStatus);
	}

	/**
	 * Does the given request qualify as "not modified"?
	 * @see ServletWebRequest#checkNotModified(long)
	 * @see ServletWebRequest#checkNotModified(String)
	 */
	private boolean isRequestNotModified(ServletWebRequest webRequest) {
		return webRequest.isNotModified();
	}

	/**
	 * Does this method have the response status instruction?
	 */
	private boolean hasResponseStatus() {
		return responseStatus != null;
	}

	private String getReturnValueHandlingErrorMessage(String message, Object returnValue) {
		StringBuilder sb = new StringBuilder(message);
		if (returnValue != null) {
			sb.append(" [type=" + returnValue.getClass().getName() + "] ");
		}
		sb.append("[value=" + returnValue + "]");
		return getDetailedErrorMessage(sb.toString());
	}

	/**
	 * Return a ServletInvocableHandlerMethod that will process the value returned
	 * from an async operation essentially either applying return value handling or
	 * raising an exception if the end result is an Exception.
	 */
	ServletInvocableHandlerMethod wrapConcurrentResult(final Object result) {

		return new CallableHandlerMethod(new Callable<Object>() {

			public Object call() throws Exception {
				if (result instanceof Exception) {
					throw (Exception) result;
				}
				else if (result instanceof Throwable) {
					throw new NestedServletException("Async processing failed", (Throwable) result);
				}
				return result;
			}
		});
	}


	/**
	 * A ServletInvocableHandlerMethod sub-class that invokes a given
	 * {@link Callable} and "inherits" the annotations of the containing class
	 * instance, useful for invoking a Callable returned from a HandlerMethod.
	 */
	private class CallableHandlerMethod extends ServletInvocableHandlerMethod {

		public CallableHandlerMethod(Callable<?> callable) {
			super(callable, ClassUtils.getMethod(callable.getClass(), "call"));
			this.setHandlerMethodReturnValueHandlers(ServletInvocableHandlerMethod.this.returnValueHandlers);
		}

		@Override
		public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
			return ServletInvocableHandlerMethod.this.getMethodAnnotation(annotationType);
		}
	}

}

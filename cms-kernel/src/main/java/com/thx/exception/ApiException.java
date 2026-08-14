package com.thx.exception;


/**
 * 面向 REST 接口的业务异常：抛出后由 {@link ExceptionHandleController#handleApi} 统一捕获，
 * 转成 JSON 格式的 {@code ResponseVo}（HTTP 状态码 500 + 异常消息）返回给前端，
 * 而不是像其它异常那样被转发到 /error 页面（那是给传统页面跳转场景准备的）。
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = -7331810807939951990L;

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public ApiException() {
        super();
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ApiException(String message) {
        super(message);
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

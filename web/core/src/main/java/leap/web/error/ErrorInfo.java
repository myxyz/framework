/*
 * Copyright 2014 the original author or authors.
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
package leap.web.error;

public class ErrorInfo {

	private final int       status;
	private final String    message;
	private final Throwable exception;
	
	public ErrorInfo(int status,String message) {
		this.status    = status;
		this.message   = message;
		this.exception = null;
	}
	
	public ErrorInfo(int status,String message, Throwable exception) {
		this.status    = status;
		this.message   = message;
		this.exception = exception;
	}

	public ErrorInfo(int status, Throwable exception) {
		this.status    = status;
		this.exception = exception;
		this.message   = null != exception ? exception.getMessage() : null;
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public Throwable getException() {
		return exception;
	}
}

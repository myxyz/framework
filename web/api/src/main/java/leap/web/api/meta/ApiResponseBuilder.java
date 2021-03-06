/*
 * Copyright 2015 the original author or authors.
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
package leap.web.api.meta;

import leap.lang.http.HTTP;
import leap.lang.meta.MType;

public class ApiResponseBuilder extends ApiObjectWithDescBuilder<ApiResponse> {
	
	public static ApiResponseBuilder ok() {
		ApiResponseBuilder r = new ApiResponseBuilder();
		
		r.setStatus(HTTP.SC_OK);
		r.setSummary("OK");
		
		return r;
	}
	
	protected int   status = HTTP.SC_OK;
	protected MType type;
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public MType getType() {
		return type;
	}

	public void setType(MType type) {
		this.type = type;
	}

	@Override
    public ApiResponse build() {
	    return new ApiResponse(summary, description, status, type, attrs);
    }
	
}
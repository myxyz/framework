/*
 * Copyright 2016 the original author or authors.
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
package app.controllers;

import leap.lang.New;
import leap.webunit.WebTestBase;
import org.junit.Test;

public class RequestBeanControllerTest extends WebTestBase {

    @Test
    public void testSimpleRequestBean() {
        forPost("/request_bean/test?id=1").setBody("Hello").send().assertOk();
        forPost("/request_bean/test0?id=1").setJson(New.hashMap("name","Hello")).send().assertOk();
        forPost("/request_bean/test1?id=1").setJson(New.hashMap("name","Hello")).send().assertOk();
        forPost("/request_bean/test2?id=1").setJson(New.hashMap("name","Hello")).send().assertOk();
        forPost("/request_bean/test3?id=1").setJson(New.hashMap("name","Hello")).send().assertOk();
    }

    @Test
    public void testMultiRequestBean() {
        forPost("/request_bean/test4?id=1&name=Hello").send().assertOk();
    }

    @Test
    public void testCombineRequestAndBody() {
        forPost("/request_bean/test5?id=1").setJson(New.hashMap("name","Hello")).send().assertOk();
    }

    @Test
    public void testBodyBean() {
        forPost("/request_bean/test6?id=1").setJson(New.hashMap("name","Hello")).send().assertOk();
        forPost("/request_bean/test7?id=1").setJson(New.hashMap("name","Hello")).send().assertOk();
    }
}

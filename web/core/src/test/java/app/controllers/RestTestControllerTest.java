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

import leap.webunit.WebTestBase;
import org.junit.Test;

public class RestTestControllerTest extends WebTestBase {

    @Test
    public void testGetObject() {
        get("/rest_test").assertOk().assertContentNotEmpty();
    }

    @Test
    public void testCreateObject() {
        post("/rest_test").assertOk().assertContentEmpty();
    }

    @Test
    public void testGetChildren() {
        get("/rest_test/children").assertOk();
    }

    @Test
    public void testGetNested() {
        get("/rest_test/nested").assertOk();
        get("/rest_test/nested1").assertOk();
        get("/rest_test/nested2").assertOk();
        get("/rest_test/nested2/nested3").assertOk();
        get("/rest_test/rest_sub1").assertOk();
        get("/rest_test/rest_sub2").assertOk();
    }
}

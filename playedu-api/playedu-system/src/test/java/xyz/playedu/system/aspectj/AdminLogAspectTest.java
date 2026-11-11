/*
 * Copyright (C) 2023 杭州白书科技有限公司
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
package xyz.playedu.system.aspectj;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Test;
import xyz.playedu.common.constant.SystemConstant;

class AdminLogAspectTest {
    private final AdminLogAspect aspect = new AdminLogAspect();

    @Test
    void redactsSensitiveFieldsInNestedObjectsAndArrays() {
        JSONObject result =
                aspect.excludeProperties(
                        "{\"email\":\"student@example.com\",\"word\":\"keep\",\"id_card\":\"sample\","
                            + "\"users\":[{\"idCard\":\"sample\",\"password\":\"secret\"}]}");
        assertEquals("student@example.com", result.getStr("email"));
        assertEquals("keep", result.getStr("word"));
        assertEquals(SystemConstant.CONFIG_MASK, result.getStr("id_card"));
        JSONObject user = result.getJSONArray("users").getJSONObject(0);
        assertEquals(SystemConstant.CONFIG_MASK, user.getStr("idCard"));
        assertEquals(SystemConstant.CONFIG_MASK, user.getStr("password"));
    }
}

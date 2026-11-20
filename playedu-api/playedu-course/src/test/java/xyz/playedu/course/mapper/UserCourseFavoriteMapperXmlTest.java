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
package xyz.playedu.course.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserCourseFavoriteMapperXmlTest {
    private Configuration configuration;

    @BeforeEach
    void parseMapper() throws IOException {
        configuration = new Configuration();
        String resource = "mapper/UserCourseFavoriteMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder builder =
                    new XMLMapperBuilder(
                            input, configuration, resource, configuration.getSqlFragments());
            builder.parse();
        }
    }

    @Test
    void buildsOpenCourseQueriesWithoutAnEmptyDepartmentCondition() {
        String pageSql = sql("paginate", List.of());
        String countSql = sql("paginateCount", List.of());

        assertTrue(pageSql.contains("c.is_show = 1"));
        assertTrue(pageSql.contains("NOT EXISTS"));
        assertTrue(pageSql.contains("ORDER BY f.created_at DESC, f.id DESC"));
        assertTrue(countSql.contains("SELECT COUNT(1)"));
        assertFalse(pageSql.contains("access_rel"));
        assertFalse(countSql.contains("access_rel"));
    }

    @Test
    void appliesTheSameDepartmentFilterToPageAndCount() {
        String pageSql = sql("paginate", List.of(1, 2, 10));
        String countSql = sql("paginateCount", List.of(1, 2, 10));

        for (String sql : List.of(pageSql, countSql)) {
            assertTrue(sql.contains("f.user_id = ?"));
            assertTrue(sql.contains("OR EXISTS"));
            assertTrue(sql.contains("access_rel.range_id IN ( ? , ? , ? )"));
        }
    }

    private String sql(String statement, List<Integer> departmentIds) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 10);
        params.put("accessDepIds", departmentIds);
        params.put("pageStart", 0);
        params.put("pageSize", 20);
        BoundSql boundSql =
                configuration
                        .getMappedStatement(
                                "xyz.playedu.course.mapper.UserCourseFavoriteMapper." + statement)
                        .getBoundSql(params);
        return boundSql.getSql().replaceAll("\\s+", " ").trim();
    }
}

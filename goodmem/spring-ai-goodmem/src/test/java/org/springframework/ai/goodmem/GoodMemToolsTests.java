/*
 * Copyright 2023-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.goodmem;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.tool.annotation.Tool;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic structural tests verifying that the GoodMem tool surface exposes the expected
 * {@link Tool}-annotated methods. These tests do not require a live GoodMem instance.
 */
class GoodMemToolsTests {

	@Test
	void allExpectedToolsAreAnnotated() {
		List<String> expectedToolNames = Arrays.asList("goodmem_create_space", "goodmem_list_spaces",
				"goodmem_create_memory", "goodmem_retrieve_memories", "goodmem_get_memory", "goodmem_delete_memory",
				"goodmem_list_embedders");
		List<String> actualNames = Arrays.stream(GoodMemTools.class.getDeclaredMethods())
			.map(method -> method.getAnnotation(Tool.class))
			.filter(annotation -> annotation != null)
			.map(Tool::name)
			.toList();
		assertThat(actualNames).containsExactlyInAnyOrderElementsOf(expectedToolNames);
	}

	@Test
	void allToolsHaveDescriptions() {
		for (Method method : GoodMemTools.class.getDeclaredMethods()) {
			Tool tool = method.getAnnotation(Tool.class);
			if (tool == null) {
				continue;
			}
			assertThat(tool.description()).as("description for tool " + tool.name()).isNotBlank();
		}
	}

}

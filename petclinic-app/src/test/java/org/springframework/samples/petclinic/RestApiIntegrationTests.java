/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.samples.petclinic;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end tests of the REST contract against the real application context and the H2
 * seed data. Each test runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RestApiIntegrationTests {

	private static final String OWNER_JSON = """
			{"firstName":"Ana","lastName":"Garcia","address":"Calle 1","city":"Madrid","telephone":"6001112222"}
			""";

	@Autowired
	private MockMvc mvc;

	@Test
	void listsOwnersByLastNamePrefixWithOneBasedPagination() throws Exception {
		this.mvc.perform(get("/api/v1/owners").param("lastName", "  Dav  ").param("page", "1").param("size", "5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(5))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[0].lastName").value("Davis"));
	}

	@Test
	void outOfRangePageFallsBackToTheFirstPage() throws Exception {
		this.mvc.perform(get("/api/v1/owners").param("lastName", "Dav").param("page", "99"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.content").isNotEmpty());
	}

	@Test
	void returnsOwnerWithPetsAndTypes() throws Exception {
		this.mvc.perform(get("/api/v1/owners/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("George"))
			.andExpect(jsonPath("$.pets[0].name").value("Leo"))
			.andExpect(jsonPath("$.pets[0].type.name").value("cat"));
	}

	@Test
	void returnsProblemDetailWhenOwnerDoesNotExist() throws Exception {
		this.mvc.perform(get("/api/v1/owners/9999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("Owner 9999 not found"));
	}

	@Test
	void createsAnOwner() throws Exception {
		this.mvc
			.perform(post("/api/v1/owners").contentType(MediaType.APPLICATION_JSON).content(OWNER_JSON))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.lastName").value("Garcia"));
	}

	@Test
	void rejectsAnInvalidTelephoneWithFieldErrors() throws Exception {
		String body = """
				{"firstName":"Ana","lastName":"Garcia","address":"Calle 1","city":"Madrid","telephone":"123"}
				""";
		this.mvc.perform(post("/api/v1/owners").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors[0].field").value("telephone"));
	}

	@Test
	void updatesAnOwner() throws Exception {
		String body = """
				{"firstName":"George","lastName":"Franklin","address":"New address","city":"Madison","telephone":"6085551023"}
				""";
		this.mvc
			.perform(put("/api/v1/owners/1").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.address").value("New address"));
	}

	@Test
	void createsAPetWithItsType() throws Exception {
		String body = """
				{"name":"Nina","birthDate":"2020-01-01","typeId":2}
				""";
		this.mvc
			.perform(post("/api/v1/owners/1/pets").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Nina"))
			.andExpect(jsonPath("$.type.name").value("dog"));
	}

	@Test
	void rejectsADuplicatePetNameForTheSameOwner() throws Exception {
		String body = """
				{"name":"leo","birthDate":"2020-01-01","typeId":1}
				""";
		this.mvc
			.perform(post("/api/v1/owners/1/pets").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void rejectsAPetWithoutType() throws Exception {
		String body = """
				{"name":"Nina","birthDate":"2020-01-01"}
				""";
		this.mvc.perform(post("/api/v1/owners/1/pets").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("typeId"));
	}

	@Test
	void createsAVisitWithTomorrowAsDefaultDate() throws Exception {
		String body = """
				{"description":"checkup"}
				""";
		this.mvc
			.perform(post("/api/v1/owners/6/pets/7/visits").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.date").value(LocalDate.now().plusDays(1).toString()))
			.andExpect(jsonPath("$.petId").value(7));
	}

	@Test
	void rejectsAVisitInThePast() throws Exception {
		String body = """
				{"description":"checkup","date":"2020-01-01"}
				""";
		this.mvc
			.perform(post("/api/v1/owners/6/pets/7/visits").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("date"));
	}

	@Test
	void listsTheVisitsOfAPet() throws Exception {
		this.mvc.perform(get("/api/v1/owners/6/pets/7/visits"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].description").value("rabies shot"));
	}

	@Test
	void rejectsVisitsForAPetOfAnotherOwner() throws Exception {
		this.mvc.perform(get("/api/v1/owners/1/pets/7/visits")).andExpect(status().isNotFound());
	}

	@Test
	void listsVetsWithSpecialties() throws Exception {
		this.mvc.perform(get("/api/v1/vets"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(6))
			.andExpect(jsonPath("$[2].lastName").value("Douglas"))
			.andExpect(jsonPath("$[2].specialties.length()").value(2));
	}

	@Test
	void returnsProblemDetailWhenVetDoesNotExist() throws Exception {
		this.mvc.perform(get("/api/v1/vets/9999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void exposesTheOpenApiContract() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.paths['/api/v1/owners']").exists());
	}

	@Test
	void exposesHealth() throws Exception {
		this.mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

}

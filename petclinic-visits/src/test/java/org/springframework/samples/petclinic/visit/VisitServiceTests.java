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
package org.springframework.samples.petclinic.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.common.exception.NotFoundException;
import org.springframework.samples.petclinic.owner.application.PetQueryService;
import org.springframework.samples.petclinic.visit.api.VisitRequest;
import org.springframework.samples.petclinic.visit.application.VisitService;

@ExtendWith(MockitoExtension.class)
class VisitServiceTests {

	@Mock
	private VisitRepository visits;

	@Mock
	private PetQueryService pets;

	@InjectMocks
	private VisitService service;

	@Test
	void createDefaultsTheDateToTomorrow() {
		when(this.pets.belongsToOwner(6, 7)).thenReturn(true);
		when(this.visits.save(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Visit visit = this.service.create(6, 7, new VisitRequest("checkup", null));

		assertThat(visit.getDate()).isEqualTo(LocalDate.now().plusDays(1));
		assertThat(visit.getPetId()).isEqualTo(7);
		assertThat(visit.getDescription()).isEqualTo("checkup");
	}

	@Test
	void createKeepsAnExplicitFutureDate() {
		LocalDate future = LocalDate.now().plusMonths(1);
		when(this.pets.belongsToOwner(6, 7)).thenReturn(true);
		when(this.visits.save(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Visit visit = this.service.create(6, 7, new VisitRequest("checkup", future));

		assertThat(visit.getDate()).isEqualTo(future);
	}

	@Test
	void createRejectsAPetThatDoesNotBelongToTheOwner() {
		when(this.pets.belongsToOwner(1, 7)).thenReturn(false);

		assertThatThrownBy(() -> this.service.create(1, 7, new VisitRequest("checkup", null)))
			.isInstanceOf(NotFoundException.class);
		verify(this.visits, never()).save(any(Visit.class));
	}

	@Test
	void listRejectsAPetThatDoesNotBelongToTheOwner() {
		when(this.pets.belongsToOwner(1, 7)).thenReturn(false);

		assertThatThrownBy(() -> this.service.findByPet(1, 7)).isInstanceOf(NotFoundException.class);
	}

}

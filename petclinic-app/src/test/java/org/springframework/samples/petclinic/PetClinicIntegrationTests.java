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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PetClinicIntegrationTests {

	@Autowired
	private VetRepository vets;

	@Autowired
	private OwnerRepository owners;

	@Autowired
	private PetTypeRepository types;

	@Autowired
	private VisitRepository visits;

	@Test
	void contextLoads() {
		assertThat(this.owners).isNotNull();
		assertThat(this.vets).isNotNull();
		assertThat(this.visits).isNotNull();
	}

	@Test
	void vetsAreServedFromCache() {
		assertThat(this.vets.findAll()).isNotEmpty();
		assertThat(this.vets.findAll()).isNotEmpty();
	}

	@Test
	void seedDataIsAvailable() {
		assertThat(this.owners.findById(1)).isPresent();
		assertThat(this.types.findPetTypes()).isNotEmpty();
		assertThat(this.visits.findByPetIdOrderByDateAsc(7)).hasSize(2);
	}

	@Test
	void visitKeepsPetReference() {
		Visit visit = new Visit();
		visit.setPetId(7);
		visit.setDate(LocalDate.now().plusDays(1));
		visit.setDescription("integration test");

		Visit saved = this.visits.save(visit);

		assertThat(saved.getId()).isNotNull();
		assertThat(this.visits.findByPetIdOrderByDateAsc(7)).extracting(Visit::getId).contains(saved.getId());
	}

	@Test
	void petCanBeReadWithItsType() {
		Owner owner = this.owners.findById(1).orElseThrow();
		Pet pet = owner.getPets().get(0);
		PetType type = pet.getType();
		assertThat(type).isNotNull();
		assertThat(type.getName()).isEqualTo("cat");
	}

}

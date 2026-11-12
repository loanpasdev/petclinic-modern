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
package org.springframework.samples.petclinic.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.common.exception.ConflictException;
import org.springframework.samples.petclinic.common.exception.NotFoundException;
import org.springframework.samples.petclinic.owner.api.OwnerRequest;
import org.springframework.samples.petclinic.owner.api.PetRequest;
import org.springframework.samples.petclinic.owner.application.OwnerService;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTests {

	@Mock
	private OwnerRepository owners;

	@Mock
	private PetTypeRepository petTypes;

	@InjectMocks
	private OwnerService service;

	@Test
	void searchTrimsTheLastNameAndUsesAOneBasedPage() {
		PageRequest expected = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "lastName"));
		when(this.owners.findByLastNameStartingWith("Dav", expected)).thenReturn(new PageImpl<>(java.util.List.of()));

		this.service.search("  Dav  ", 1, 5);

		verify(this.owners).findByLastNameStartingWith("Dav", expected);
	}

	@Test
	void createTrimsTextFields() {
		when(this.owners.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Owner owner = this.service.create(new OwnerRequest(" Ana ", " Garcia ", " Calle 1 ", " Madrid ", "6001112222"));

		assertThat(owner.getFirstName()).isEqualTo("Ana");
		assertThat(owner.getLastName()).isEqualTo("Garcia");
		assertThat(owner.getAddress()).isEqualTo("Calle 1");
		assertThat(owner.getCity()).isEqualTo("Madrid");
	}

	@Test
	void addPetRejectsADuplicateNameIgnoringCase() {
		Owner owner = ownerWithPet("Rex");
		when(this.owners.findById(1)).thenReturn(Optional.of(owner));

		assertThatThrownBy(() -> this.service.addPet(1, petRequest("rex")))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("already in use");
	}

	@Test
	void addPetRejectsAnUnknownType() {
		Owner owner = ownerWithPet("Rex");
		when(this.owners.findById(1)).thenReturn(Optional.of(owner));
		when(this.petTypes.findById(99)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.addPet(1, new PetRequest("New", LocalDate.now(), 99)))
			.isInstanceOf(NotFoundException.class)
			.hasMessageContaining("Pet type 99");
	}

	@Test
	void addPetStoresTheResolvedType() {
		Owner owner = ownerWithPet("Rex");
		PetType dog = new PetType();
		dog.setId(2);
		dog.setName("dog");
		when(this.owners.findById(1)).thenReturn(Optional.of(owner));
		when(this.petTypes.findById(2)).thenReturn(Optional.of(dog));
		when(this.owners.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<Owner> saved = ArgumentCaptor.forClass(Owner.class);
		this.service.addPet(1, new PetRequest("New", LocalDate.now(), 2));

		verify(this.owners).save(saved.capture());
		assertThat(saved.getValue().getPet("New").getType()).isEqualTo(dog);
	}

	@Test
	void updatePetRejectsTheNameOfAnotherPet() {
		Owner owner = ownerWithPet("Rex");
		Pet other = new Pet();
		other.setId(2);
		other.setName("Tom");
		owner.addPet(other);
		when(this.owners.findById(1)).thenReturn(Optional.of(owner));

		assertThatThrownBy(() -> this.service.updatePet(1, 2, petRequest("rex")))
			.isInstanceOf(ConflictException.class);
	}

	private Owner ownerWithPet(String name) {
		Owner owner = new Owner();
		owner.setId(1);
		owner.setFirstName("Ana");
		owner.setLastName("Garcia");
		owner.setAddress("Calle 1");
		owner.setCity("Madrid");
		owner.setTelephone("6001112222");
		Pet pet = new Pet();
		pet.setId(1);
		pet.setName(name);
		owner.addPet(pet);
		return owner;
	}

	private PetRequest petRequest(String name) {
		return new PetRequest(name, LocalDate.now(), 1);
	}

}

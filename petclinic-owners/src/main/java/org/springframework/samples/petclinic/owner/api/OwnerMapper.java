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
package org.springframework.samples.petclinic.owner.api;

import java.util.List;

import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;

/**
 * Maps owner-module entities to their public representations.
 */
public final class OwnerMapper {

	private OwnerMapper() {
	}

	public static OwnerResponse toResponse(Owner owner) {
		List<PetResponse> pets = owner.getPets().stream().map(OwnerMapper::toResponse).toList();
		return new OwnerResponse(owner.getId(), owner.getFirstName(), owner.getLastName(), owner.getAddress(),
				owner.getCity(), owner.getTelephone(), pets);
	}

	public static PetResponse toResponse(Pet pet) {
		return new PetResponse(pet.getId(), pet.getName(), pet.getBirthDate(), toResponse(pet.getType()));
	}

	public static PetTypeResponse toResponse(PetType type) {
		return type == null ? null : new PetTypeResponse(type.getId(), type.getName());
	}

}

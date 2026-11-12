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

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.application.OwnerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for pets. Pets are always addressed through their owner because the
 * legacy application treated them as part of the owner aggregate.
 */
@RestController
@RequestMapping("/api/v1/owners/{ownerId}/pets")
public class PetController {

	private final OwnerService owners;

	public PetController(OwnerService owners) {
		this.owners = owners;
	}

	@PostMapping
	public ResponseEntity<PetResponse> create(@PathVariable Integer ownerId, @Valid @RequestBody PetRequest request) {
		Owner owner = this.owners.addPet(ownerId, request);
		Pet pet = owner.getPet(request.name().trim());
		return ResponseEntity.created(URI.create("/api/v1/owners/" + ownerId + "/pets/" + pet.getId()))
			.body(OwnerMapper.toResponse(pet));
	}

	@PutMapping("/{petId}")
	public PetResponse update(@PathVariable Integer ownerId, @PathVariable Integer petId,
			@Valid @RequestBody PetRequest request) {
		Owner owner = this.owners.updatePet(ownerId, petId, request);
		return OwnerMapper.toResponse(owner.getPet(petId));
	}

}

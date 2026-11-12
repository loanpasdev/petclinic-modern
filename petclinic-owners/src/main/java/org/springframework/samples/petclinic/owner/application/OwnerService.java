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
package org.springframework.samples.petclinic.owner.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.common.exception.ConflictException;
import org.springframework.samples.petclinic.common.exception.NotFoundException;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.owner.api.OwnerRequest;
import org.springframework.samples.petclinic.owner.api.PetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the owner aggregate. All business rules that the legacy
 * application enforced live here so they are independent from the transport layer.
 */
@Service
@Transactional(readOnly = true)
public class OwnerService {

	private final OwnerRepository owners;

	private final PetTypeRepository petTypes;

	public OwnerService(OwnerRepository owners, PetTypeRepository petTypes) {
		this.owners = owners;
		this.petTypes = petTypes;
	}

	/**
	 * Search owners by last-name prefix. The legacy screen used a 1-based page number and
	 * redirected out-of-range pages to the first page; both behaviours are preserved.
	 * @param lastName prefix to search for, trimmed; empty means "all owners"
	 * @param page 1-based page number
	 * @param size page size
	 * @return a page of matching owners
	 */
	public Page<Owner> search(String lastName, int page, int size) {
		String term = lastName == null ? "" : lastName.trim();
		Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, size, Sort.by(Sort.Direction.ASC, "lastName"));
		Page<Owner> result = this.owners.findByLastNameStartingWith(term, pageable);
		if (result.getTotalPages() > 0 && page > result.getTotalPages()) {
			pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.ASC, "lastName"));
			result = this.owners.findByLastNameStartingWith(term, pageable);
		}
		return result;
	}

	public Owner find(Integer id) {
		return this.owners.findById(id)
			.orElseThrow(() -> new NotFoundException("Owner %d not found".formatted(id)));
	}

	@Transactional
	public Owner create(OwnerRequest request) {
		Owner owner = new Owner();
		apply(owner, request);
		return this.owners.save(owner);
	}

	@Transactional
	public Owner update(Integer id, OwnerRequest request) {
		Owner owner = find(id);
		apply(owner, request);
		return this.owners.save(owner);
	}

	@Transactional
	public Owner addPet(Integer ownerId, PetRequest request) {
		Owner owner = find(ownerId);
		ensureNameIsFree(owner, request.name(), null);
		Pet pet = new Pet();
		apply(pet, request);
		owner.addPet(pet);
		return this.owners.save(owner);
	}

	@Transactional
	public Owner updatePet(Integer ownerId, Integer petId, PetRequest request) {
		Owner owner = find(ownerId);
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new NotFoundException("Pet %d not found for owner %d".formatted(petId, ownerId));
		}
		ensureNameIsFree(owner, request.name(), petId);
		apply(pet, request);
		return this.owners.save(owner);
	}

	public List<PetType> findPetTypes() {
		return this.petTypes.findPetTypes();
	}

	private void apply(Owner owner, OwnerRequest request) {
		owner.setFirstName(request.firstName().trim());
		owner.setLastName(request.lastName().trim());
		owner.setAddress(request.address().trim());
		owner.setCity(request.city().trim());
		owner.setTelephone(request.telephone().trim());
	}

	private void apply(Pet pet, PetRequest request) {
		pet.setName(request.name().trim());
		pet.setBirthDate(request.birthDate());
		pet.setType(findPetType(request.typeId()));
	}

	private PetType findPetType(Integer typeId) {
		return this.petTypes.findById(typeId)
			.orElseThrow(() -> new NotFoundException("Pet type %d not found".formatted(typeId)));
	}

	private void ensureNameIsFree(Owner owner, String name, Integer excludedPetId) {
		Pet existing = owner.getPet(name);
		if (existing != null && !existing.getId().equals(excludedPetId)) {
			throw new ConflictException("Pet name '%s' is already in use for this owner".formatted(name.trim()));
		}
	}

}

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
package org.springframework.samples.petclinic.visit.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.samples.petclinic.common.exception.NotFoundException;
import org.springframework.samples.petclinic.owner.application.PetQueryService;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.samples.petclinic.visit.api.VisitRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for visits. It depends on the public {@link PetQueryService} port
 * of the owners module instead of its repositories.
 */
@Service
@Transactional(readOnly = true)
public class VisitService {

	private final VisitRepository visits;

	private final PetQueryService pets;

	public VisitService(VisitRepository visits, PetQueryService pets) {
		this.visits = visits;
		this.pets = pets;
	}

	public List<Visit> findByPet(Integer ownerId, Integer petId) {
		requirePet(ownerId, petId);
		return this.visits.findByPetIdOrderByDateAsc(petId);
	}

	@Transactional
	public Visit create(Integer ownerId, Integer petId, VisitRequest request) {
		requirePet(ownerId, petId);
		Visit visit = new Visit();
		visit.setPetId(petId);
		visit.setDescription(request.description().trim());
		visit.setDate(request.date() != null ? request.date() : LocalDate.now().plusDays(1));
		return this.visits.save(visit);
	}

	private void requirePet(Integer ownerId, Integer petId) {
		if (!this.pets.belongsToOwner(ownerId, petId)) {
			throw new NotFoundException("Pet %d not found for owner %d".formatted(petId, ownerId));
		}
	}

}

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
package org.springframework.samples.petclinic.vet.api;

import java.util.List;

import org.springframework.samples.petclinic.common.exception.NotFoundException;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for the veterinary catalogue.
 */
@RestController
@RequestMapping("/api/v1/vets")
public class VetController {

	private final VetRepository vets;

	public VetController(VetRepository vets) {
		this.vets = vets;
	}

	@GetMapping
	public List<VetResponse> list() {
		return this.vets.findAll().stream().map(VetController::toResponse).toList();
	}

	@GetMapping("/{id}")
	public VetResponse get(@PathVariable Integer id) {
		Vet vet = this.vets.findById(id)
			.orElseThrow(() -> new NotFoundException("Vet %d not found".formatted(id)));
		return toResponse(vet);
	}

	private static VetResponse toResponse(Vet vet) {
		List<SpecialtyResponse> specialties = vet.getSpecialties()
			.stream()
			.map(specialty -> new SpecialtyResponse(specialty.getId(), specialty.getName()))
			.toList();
		return new VetResponse(vet.getId(), vet.getFirstName(), vet.getLastName(), specialties);
	}

}

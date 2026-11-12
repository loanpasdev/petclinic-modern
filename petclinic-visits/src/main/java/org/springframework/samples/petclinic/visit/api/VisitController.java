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
package org.springframework.samples.petclinic.visit.api;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.application.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for the visits of a pet.
 */
@RestController
@RequestMapping("/api/v1/owners/{ownerId}/pets/{petId}/visits")
public class VisitController {

	private final VisitService visits;

	public VisitController(VisitService visits) {
		this.visits = visits;
	}

	@GetMapping
	public List<VisitResponse> list(@PathVariable Integer ownerId, @PathVariable Integer petId) {
		return this.visits.findByPet(ownerId, petId).stream().map(VisitController::toResponse).toList();
	}

	@PostMapping
	public ResponseEntity<VisitResponse> create(@PathVariable Integer ownerId, @PathVariable Integer petId,
			@Valid @RequestBody VisitRequest request) {
		Visit created = this.visits.create(ownerId, petId, request);
		return ResponseEntity
			.created(URI.create("/api/v1/owners/%d/pets/%d/visits/%d".formatted(ownerId, petId, created.getId())))
			.body(toResponse(created));
	}

	private static VisitResponse toResponse(Visit visit) {
		return new VisitResponse(visit.getId(), visit.getPetId(), visit.getDate(), visit.getDescription());
	}

}

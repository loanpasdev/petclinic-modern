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
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.common.web.PageResponse;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.application.OwnerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for owners.
 */
@RestController
@RequestMapping("/api/v1/owners")
@Validated
public class OwnerController {

	private final OwnerService owners;

	public OwnerController(OwnerService owners) {
		this.owners = owners;
	}

	@GetMapping
	public PageResponse<OwnerResponse> list(@RequestParam(defaultValue = "") String lastName,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "5") @Min(1) @Max(100) int size) {
		Page<Owner> result = this.owners.search(lastName, page, size);
		List<OwnerResponse> content = result.getContent().stream().map(OwnerMapper::toResponse).toList();
		int effectivePage = (result.getTotalPages() > 0 && page > result.getTotalPages()) ? 1 : page;
		return PageResponse.of(content, effectivePage, size, result.getTotalElements(), result.getTotalPages());
	}

	@GetMapping("/{id}")
	public OwnerResponse get(@PathVariable Integer id) {
		return OwnerMapper.toResponse(this.owners.find(id));
	}

	@PostMapping
	public ResponseEntity<OwnerResponse> create(@Valid @RequestBody OwnerRequest request) {
		Owner created = this.owners.create(request);
		return ResponseEntity.created(URI.create("/api/v1/owners/" + created.getId()))
			.body(OwnerMapper.toResponse(created));
	}

	@PutMapping("/{id}")
	public OwnerResponse update(@PathVariable Integer id, @Valid @RequestBody OwnerRequest request) {
		return OwnerMapper.toResponse(this.owners.update(id, request));
	}

}

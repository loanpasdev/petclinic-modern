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

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Visit} domain objects.
 */
public interface VisitRepository extends JpaRepository<Visit, Integer> {

	/**
	 * Retrieve the visits of a pet, oldest first.
	 * @param petId the pet identifier
	 * @return the visits of the pet (empty when there are none)
	 */
	List<Visit> findByPetIdOrderByDateAsc(Integer petId);

}

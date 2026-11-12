/*
 * PetClinic · interfaz web estática.
 * Consume la API REST pública (/api/v1) sin frameworks ni dependencias externas.
 */
(() => {
  'use strict';

  const API = '/api/v1';
  // Tamaño de página de la interfaz. La API conserva el default legacy de 5; la SPA pide 6
  // por página como decisión de presentación (la API admite cualquier tamaño entre 1 y 100).
  const PAGE_SIZE = 6;

  const state = {
    view: 'owners',
    search: '',
    page: 1,
    totalPages: 0,
    totalElements: 0,
    owners: [],
    petTypes: [],
    vets: [],
    specialty: null,
    loadingOwners: false,
  };

  const els = {
    ownersGrid: document.getElementById('owners-grid'),
    ownersPagination: document.getElementById('owners-pagination'),
    pageInfo: document.getElementById('page-info'),
    ownersSubtitle: document.getElementById('owners-subtitle'),
    search: document.getElementById('owner-search'),
    clearSearch: document.getElementById('clear-search'),
    vetsGrid: document.getElementById('vets-grid'),
    specialtyFilter: document.getElementById('specialty-filter'),
    modalRoot: document.getElementById('modal-root'),
    toastRoot: document.getElementById('toast-root'),
    statOwners: document.getElementById('stat-owners'),
    statPets: document.getElementById('stat-pets'),
    statVets: document.getElementById('stat-vets'),
    statSpecialties: document.getElementById('stat-specialties'),
  };

  /* ------------------------------------------------------------------ API */

  class ApiError extends Error {
    constructor(status, body) {
      super(`HTTP ${status}`);
      this.status = status;
      this.body = body;
    }
  }

  async function api(path, { method = 'GET', body } = {}) {
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    const response = await fetch(API + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });

    const text = await response.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = text;
      }
    }

    if (!response.ok) throw new ApiError(response.status, data);
    return data;
  }

  function errorMessage(error) {
    if (error instanceof ApiError) {
      const base = error.body?.detail || error.body?.title || `Error ${error.status}`;
      const fields = Array.isArray(error.body?.errors)
        ? error.body.errors.map((item) => item.message).filter(Boolean)
        : [];
      return fields.length ? `${base}: ${fields.join(' · ')}` : base;
    }
    return error?.message || 'No se pudo completar la operación';
  }

  /* ---------------------------------------------------------------- Utils */

  const escapeHtml = (value) =>
    String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');

  const initials = (first, last) =>
    `${(first || '').trim().charAt(0)}${(last || '').trim().charAt(0)}`.toUpperCase() || '?';

  const PET_EMOJI = {
    cat: '🐱',
    dog: '🐶',
    lizard: '🦎',
    snake: '🐍',
    bird: '🐦',
    hamster: '🐹',
    rabbit: '🐰',
    horse: '🐴',
    fish: '🐟',
  };

  const petEmoji = (type) => PET_EMOJI[String(type || '').toLowerCase()] || '🐾';

  function formatDate(value) {
    if (!value) return '';
    const date = new Date(`${value}T00:00:00`);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    }).format(date);
  }

  function visitDay(value) {
    const date = new Date(`${value}T00:00:00`);
    if (Number.isNaN(date.getTime())) return { day: '--', month: '' };
    return {
      day: new Intl.DateTimeFormat('es-ES', { day: '2-digit' }).format(date),
      month: new Intl.DateTimeFormat('es-ES', { month: 'short' }).format(date).replace('.', ''),
    };
  }

  function petAge(birthDate) {
    if (!birthDate) return 'Edad desconocida';
    const birth = new Date(`${birthDate}T00:00:00`);
    if (Number.isNaN(birth.getTime())) return 'Edad desconocida';

    const now = new Date();
    let months = (now.getFullYear() - birth.getFullYear()) * 12 + (now.getMonth() - birth.getMonth());
    if (now.getDate() < birth.getDate()) months -= 1;

    if (months < 0) return 'Recién nacido';
    if (months < 1) return 'Menos de 1 mes';
    if (months < 12) return `${months} ${months === 1 ? 'mes' : 'meses'}`;
    const years = Math.floor(months / 12);
    return `${years} ${years === 1 ? 'año' : 'años'}`;
  }

  const isoDate = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const tomorrow = () => {
    const date = new Date();
    date.setDate(date.getDate() + 1);
    return isoDate(date);
  };

  const today = () => isoDate(new Date());

  /* ---------------------------------------------------------------- Icons */

  const icon = (name) => {
    const paths = {
      pin: '<path d="M12 21s7-5.6 7-11a7 7 0 1 0-14 0c0 5.4 7 11 7 11Z"/><circle cx="12" cy="10" r="2.5"/>',
      phone:
        '<path d="M6.5 3h2.2l1.6 4-2 1.3a12 12 0 0 0 5.4 5.4l1.3-2 4 1.6v2.2A2.5 2.5 0 0 1 16.5 18C9.6 17.6 4.4 12.4 4 5.5A2.5 2.5 0 0 1 6.5 3Z"/>',
      pencil:
        '<path d="M4 20h4.2L20 8.2 15.8 4 4 15.8V20Z"/><path d="m13.6 6.2 4.2 4.2"/>',
      plus: '<path d="M12 5v14M5 12h14"/>',
      calendar:
        '<rect x="4" y="5" width="16" height="16" rx="2"/><path d="M8 3v4M16 3v4M4 10h16"/>',
      close: '<path d="m6 6 12 12M18 6 6 18"/>',
      paw:
        '<path d="M12 13.2c-2.6 0-5.6 3.1-5.6 5.5 0 1.5 1.1 2.3 2.5 2.3 1 0 1.9-.5 3.1-.5s2.1.5 3.1.5c1.4 0 2.5-.8 2.5-2.3 0-2.4-3-5.5-5.6-5.5Z"/><ellipse cx="5.4" cy="10.6" rx="2.1" ry="2.7" transform="rotate(-18 5.4 10.6)"/><ellipse cx="9.2" cy="6.6" rx="2" ry="2.7" transform="rotate(-8 9.2 6.6)"/><ellipse cx="14.8" cy="6.6" rx="2" ry="2.7" transform="rotate(8 14.8 6.6)"/><ellipse cx="18.6" cy="10.6" rx="2.1" ry="2.7" transform="rotate(18 18.6 10.6)"/>',
      stethoscope:
        '<path d="M5 3v5a4 4 0 0 0 8 0V3"/><path d="M9 12v3a5 5 0 0 0 10 0v-2"/><circle cx="19" cy="10" r="2"/>',
      check: '<path d="m5 12 4.5 4.5L19 7"/>',
      alert: '<path d="M12 4 3 20h18L12 4Z"/><path d="M12 10v4M12 17h.01"/>',
    };
    const stroke = name === 'paw' ? 'fill="currentColor" stroke="none"' : 'fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"';
    return `<svg viewBox="0 0 24 24" aria-hidden="true" ${stroke}>${paths[name] || ''}</svg>`;
  };

  /* --------------------------------------------------------------- Toasts */

  function toast(message, type = 'success') {
    const node = document.createElement('div');
    node.className = `toast${type === 'error' ? ' toast--error' : ''}`;
    node.setAttribute('role', type === 'error' ? 'alert' : 'status');
    node.innerHTML = `<span class="toast-icon">${type === 'error' ? '!' : '✓'}</span><span>${escapeHtml(message)}</span>`;
    els.toastRoot.appendChild(node);
    window.setTimeout(() => node.remove(), 4200);
  }

  /* --------------------------------------------------------------- Modales */

  let lastFocused = null;

  function openModal(html, { wide = false } = {}) {
    lastFocused = document.activeElement;
    els.modalRoot.innerHTML = `
      <div class="modal-backdrop">
        <div class="modal${wide ? ' modal--wide' : ''}" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          ${html}
        </div>
      </div>`;
    document.body.style.overflow = 'hidden';

    const backdrop = els.modalRoot.querySelector('.modal-backdrop');
    backdrop.addEventListener('mousedown', (event) => {
      if (event.target === backdrop) closeModal();
    });

    const focusable =
      els.modalRoot.querySelector('input:not([type="hidden"]), select, textarea') ||
      els.modalRoot.querySelector('button');
    focusable?.focus();
  }

  function closeModal() {
    els.modalRoot.innerHTML = '';
    document.body.style.overflow = '';
    if (lastFocused instanceof HTMLElement) lastFocused.focus();
    lastFocused = null;
  }

  const closeButton = `<button type="button" class="icon-btn" data-action="close-modal" aria-label="Cerrar">${icon('close')}</button>`;

  function formModal({ title, subtitle, formId, body, submitLabel, wide = false }) {
    return `
      <div class="modal-head">
        <div>
          <h2 id="modal-title">${title}</h2>
          ${subtitle ? `<p>${subtitle}</p>` : ''}
        </div>
        ${closeButton}
      </div>
      <div class="modal-body">
        <form id="${formId}">
          <div class="form-error" role="alert"></div>
          ${body}
        </form>
      </div>
      <div class="modal-foot">
        <button type="button" class="btn btn-ghost" data-action="close-modal">Cancelar</button>
        <button type="submit" form="${formId}" class="btn btn-primary">${submitLabel}</button>
      </div>`;
  }

  function setFormError(form, message) {
    const box = form.querySelector('.form-error');
    if (box) box.textContent = message || '';
  }

  function markFieldErrors(form, errors) {
    form.querySelectorAll('[data-error-for]').forEach((node) => {
      node.textContent = '';
    });
    form.querySelectorAll('[aria-invalid="true"]').forEach((node) => {
      node.removeAttribute('aria-invalid');
    });

    (errors || []).forEach((item) => {
      const target = form.querySelector(`[data-error-for="${CSS.escape(item.field || '')}"]`);
      if (target) target.textContent = item.message || '';
      const input = form.querySelector(`[name="${CSS.escape(item.field || '')}"]`);
      if (input) input.setAttribute('aria-invalid', 'true');
    });
  }

  function submitting(form, busy) {
    const button = document.querySelector(`[form="${form.id}"][type="submit"]`);
    if (button) {
      button.disabled = busy;
      button.dataset.label ??= button.textContent;
      button.textContent = busy ? 'Guardando…' : button.dataset.label;
    }
  }

  /* ------------------------------------------------------------- Carga API */

  async function loadPetTypes() {
    state.petTypes = await api('/pet-types');
  }

  async function loadVets() {
    state.vets = await api('/vets');
    renderVets();
  }

  async function loadOwners() {
    state.loadingOwners = true;
    renderOwners();

    try {
      const params = new URLSearchParams({
        lastName: state.search,
        page: String(state.page),
        size: String(PAGE_SIZE),
      });
      const page = await api(`/owners?${params}`);
      state.owners = page.content || [];
      state.totalPages = page.totalPages || 0;
      state.totalElements = page.totalElements || 0;
      if (page.page && page.page !== state.page) state.page = page.page;
    } catch (error) {
      state.owners = [];
      state.totalPages = 0;
      state.totalElements = 0;
      toast(errorMessage(error), 'error');
    } finally {
      state.loadingOwners = false;
      renderOwners();
      renderPagination();
    }
  }

  async function loadStats() {
    try {
      const [owners, vets] = await Promise.all([
        api(`/owners?lastName=&page=1&size=100`),
        api('/vets'),
      ]);
      const allOwners = owners.content || [];
      const pets = allOwners.reduce((total, owner) => total + (owner.pets?.length || 0), 0);
      const specialties = new Set(
        (vets || []).flatMap((vet) => (vet.specialties || []).map((item) => item.name)),
      );
      els.statOwners.textContent = String(owners.totalElements ?? allOwners.length);
      els.statPets.textContent = String(pets);
      els.statVets.textContent = String((vets || []).length);
      els.statSpecialties.textContent = String(specialties.size);
    } catch {
      /* las estadísticas son decorativas: un fallo no bloquea la vista */
    }
  }

  /* -------------------------------------------------------------- Render */

  function renderOwners() {
    if (state.loadingOwners) {
      els.ownersGrid.innerHTML = Array.from({ length: 3 }, () => '<div class="skeleton-card"></div>').join('');
      return;
    }

    if (!state.owners.length) {
      const filtered = Boolean(state.search.trim());
      els.ownersGrid.innerHTML = `
        <div class="empty-state">
          ${icon('paw')}
          <h3>${filtered ? 'Sin resultados' : 'Todavía no hay propietarios'}</h3>
          <p>${
            filtered
              ? `No encontramos apellidos que empiecen por “${escapeHtml(state.search.trim())}”.`
              : 'Registra al primer propietario para empezar a gestionar sus mascotas.'
          }</p>
          <button type="button" class="btn btn-primary" data-action="new-owner">+ Nuevo propietario</button>
        </div>`;
      return;
    }

    els.ownersGrid.innerHTML = state.owners.map(ownerCard).join('');
  }

  function ownerCard(owner) {
    const pets = owner.pets || [];
    return `
      <article class="owner-card">
        <div class="card-head">
          <div class="avatar" aria-hidden="true">${escapeHtml(initials(owner.firstName, owner.lastName))}</div>
          <div class="card-title">
            <h3>${escapeHtml(owner.firstName)} ${escapeHtml(owner.lastName)}</h3>
            <p>${escapeHtml(owner.city || 'Sin ciudad')}</p>
          </div>
          <button type="button" class="icon-btn" data-action="edit-owner" data-id="${owner.id}"
                  title="Editar propietario" aria-label="Editar a ${escapeHtml(owner.firstName)} ${escapeHtml(owner.lastName)}">
            ${icon('pencil')}
          </button>
        </div>

        <ul class="meta">
          <li>${icon('pin')}<span>${escapeHtml(owner.address || 'Sin dirección')}</span></li>
          <li>${icon('phone')}<span>${escapeHtml(owner.telephone || 'Sin teléfono')}</span></li>
        </ul>

        <div class="pets">
          <div class="pets-head">
            Mascotas <span class="count-pill">${pets.length}</span>
          </div>
          ${
            pets.length
              ? `<ul class="pet-list">${pets.map((pet) => petItem(owner, pet)).join('')}</ul>`
              : '<p class="no-pets">Este propietario aún no tiene mascotas.</p>'
          }
          <button type="button" class="add-pet" data-action="new-pet" data-owner="${owner.id}">
            ${icon('plus')} Añadir mascota
          </button>
        </div>
      </article>`;
  }

  function petItem(owner, pet) {
    const type = pet.type?.name || 'Mascota';
    return `
      <li class="pet-item">
        <span class="pet-emoji" aria-hidden="true">${petEmoji(type)}</span>
        <div class="pet-info">
          <strong>${escapeHtml(pet.name)}</strong>
          <small>${escapeHtml(type)} · ${escapeHtml(petAge(pet.birthDate))}</small>
        </div>
        <div class="pet-actions">
          <button type="button" class="icon-btn" data-action="visits"
                  data-owner="${owner.id}" data-pet="${pet.id}" data-name="${escapeHtml(pet.name)}"
                  title="Ver visitas" aria-label="Ver visitas de ${escapeHtml(pet.name)}">
            ${icon('calendar')}
          </button>
          <button type="button" class="icon-btn" data-action="edit-pet"
                  data-owner="${owner.id}" data-pet="${pet.id}"
                  title="Editar mascota" aria-label="Editar a ${escapeHtml(pet.name)}">
            ${icon('pencil')}
          </button>
        </div>
      </li>`;
  }

  function renderPagination() {
    const visible = state.totalPages > 1;
    els.ownersPagination.hidden = !visible;
    if (!visible) return;

    els.pageInfo.textContent = `Página ${state.page} de ${state.totalPages} · ${state.totalElements} ${
      state.totalElements === 1 ? 'propietario' : 'propietarios'
    }`;
    els.ownersPagination.querySelector('[data-action="prev-page"]').disabled = state.page <= 1;
    els.ownersPagination.querySelector('[data-action="next-page"]').disabled =
      state.page >= state.totalPages;
  }

  function renderVets() {
    const specialties = [...new Set(state.vets.flatMap((vet) => (vet.specialties || []).map((s) => s.name)))].sort(
      (a, b) => a.localeCompare(b, 'es'),
    );

    els.specialtyFilter.innerHTML = [
      `<button type="button" class="filter-chip${state.specialty ? '' : ' is-active'}" data-action="filter-specialty" data-specialty="">Todas</button>`,
      ...specialties.map(
        (name) => `<button type="button" class="filter-chip${
          state.specialty === name ? ' is-active' : ''
        }" data-action="filter-specialty" data-specialty="${escapeHtml(name)}">${escapeHtml(name)}</button>`,
      ),
    ].join('');

    const visible = state.specialty
      ? state.vets.filter((vet) => (vet.specialties || []).some((s) => s.name === state.specialty))
      : state.vets;

    if (!visible.length) {
      els.vetsGrid.innerHTML = `
        <div class="empty-state">
          ${icon('stethoscope')}
          <h3>Sin veterinarios</h3>
          <p>No hay profesionales para la especialidad seleccionada.</p>
        </div>`;
      return;
    }

    els.vetsGrid.innerHTML = visible.map(vetCard).join('');
  }

  function vetCard(vet) {
    const specialties = vet.specialties || [];
    return `
      <article class="vet-card">
        <div class="card-head">
          <div class="avatar avatar--vet" aria-hidden="true">${escapeHtml(initials(vet.firstName, vet.lastName))}</div>
          <div class="card-title">
            <h3>${escapeHtml(vet.firstName)} ${escapeHtml(vet.lastName)}</h3>
            <p>Medicina veterinaria</p>
          </div>
          <button type="button" class="icon-btn" data-action="view-vet" data-id="${vet.id}"
                  title="Ver ficha" aria-label="Ver ficha de ${escapeHtml(vet.firstName)} ${escapeHtml(vet.lastName)}">
            ${icon('stethoscope')}
          </button>
        </div>
        <div class="chips">
          ${
            specialties.length
              ? specialties.map((item) => `<span class="chip">${escapeHtml(item.name)}</span>`).join('')
              : '<span class="chip chip--empty">Sin especialidades</span>'
          }
        </div>
      </article>`;
  }

  /* -------------------------------------------------------------- Formularios */

  function ownerForm(owner = null) {
    const editing = Boolean(owner);
    openModal(
      formModal({
        title: editing ? 'Editar propietario' : 'Nuevo propietario',
        subtitle: editing
          ? 'Actualiza los datos de contacto del propietario.'
          : 'Registra a un propietario y después añade sus mascotas.',
        formId: 'owner-form',
        submitLabel: editing ? 'Guardar cambios' : 'Crear propietario',
        body: `
          <div class="form-grid">
            <div class="field">
              <label for="firstName">Nombre</label>
              <input id="firstName" name="firstName" value="${escapeHtml(owner?.firstName)}" required maxlength="30" autocomplete="given-name">
              <span class="field-error" data-error-for="firstName"></span>
            </div>
            <div class="field">
              <label for="lastName">Apellido</label>
              <input id="lastName" name="lastName" value="${escapeHtml(owner?.lastName)}" required maxlength="30" autocomplete="family-name">
              <span class="field-error" data-error-for="lastName"></span>
            </div>
            <div class="field field--full">
              <label for="address">Dirección</label>
              <input id="address" name="address" value="${escapeHtml(owner?.address)}" required maxlength="255" autocomplete="street-address">
              <span class="field-error" data-error-for="address"></span>
            </div>
            <div class="field">
              <label for="city">Ciudad</label>
              <input id="city" name="city" value="${escapeHtml(owner?.city)}" required maxlength="80" autocomplete="address-level2">
              <span class="field-error" data-error-for="city"></span>
            </div>
            <div class="field">
              <label for="telephone">Teléfono</label>
              <input id="telephone" name="telephone" value="${escapeHtml(owner?.telephone)}" required
                     inputmode="numeric" pattern="[0-9]{10}" maxlength="10" title="Debe tener exactamente 10 dígitos" autocomplete="tel">
              <span class="field-hint">10 dígitos, sin espacios.</span>
              <span class="field-error" data-error-for="telephone"></span>
            </div>
          </div>`,
      }),
    );

    const form = document.getElementById('owner-form');
    form.dataset.id = owner?.id ?? '';
    form.addEventListener('submit', (event) => submitOwner(event, form));
  }

  async function submitOwner(event, form) {
    event.preventDefault();
    if (!form.reportValidity()) return;

    const data = new FormData(form);
    const payload = {
      firstName: String(data.get('firstName') || '').trim(),
      lastName: String(data.get('lastName') || '').trim(),
      address: String(data.get('address') || '').trim(),
      city: String(data.get('city') || '').trim(),
      telephone: String(data.get('telephone') || '').trim(),
    };

    const id = form.dataset.id;
    setFormError(form, '');
    markFieldErrors(form, []);
    submitting(form, true);

    try {
      if (id) {
        await api(`/owners/${id}`, { method: 'PUT', body: payload });
        toast('Propietario actualizado');
      } else {
        state.page = 1;
        await api('/owners', { method: 'POST', body: payload });
        toast('Propietario creado');
      }
      closeModal();
      await Promise.all([loadOwners(), loadStats()]);
    } catch (error) {
      markFieldErrors(form, error.body?.errors);
      setFormError(form, errorMessage(error));
    } finally {
      submitting(form, false);
    }
  }

  function petForm({ ownerId, pet = null }) {
    const editing = Boolean(pet);
    const typeOptions = state.petTypes
      .map(
        (type) =>
          `<option value="${type.id}"${pet?.type?.id === type.id ? ' selected' : ''}>${escapeHtml(type.name)}</option>`,
      )
      .join('');

    openModal(
      formModal({
        title: editing ? 'Editar mascota' : 'Nueva mascota',
        subtitle: editing ? 'Modifica los datos de la mascota.' : 'Añade una mascota al propietario.',
        formId: 'pet-form',
        submitLabel: editing ? 'Guardar cambios' : 'Añadir mascota',
        body: `
          <div class="form-grid">
            <div class="field">
              <label for="petName">Nombre</label>
              <input id="petName" name="name" value="${escapeHtml(pet?.name)}" required maxlength="30">
              <span class="field-error" data-error-for="name"></span>
            </div>
            <div class="field">
              <label for="typeId">Tipo</label>
              <select id="typeId" name="typeId" required>
                <option value="" disabled${pet ? '' : ' selected'}>Selecciona un tipo</option>
                ${typeOptions}
              </select>
              <span class="field-error" data-error-for="typeId"></span>
            </div>
            <div class="field field--full">
              <label for="birthDate">Fecha de nacimiento</label>
              <input id="birthDate" name="birthDate" type="date" value="${escapeHtml(pet?.birthDate)}" max="${today()}" required>
              <span class="field-error" data-error-for="birthDate"></span>
            </div>
          </div>`,
      }),
    );

    const form = document.getElementById('pet-form');
    form.dataset.owner = ownerId;
    form.dataset.pet = pet?.id ?? '';
    form.addEventListener('submit', (event) => submitPet(event, form));
  }

  async function submitPet(event, form) {
    event.preventDefault();
    if (!form.reportValidity()) return;

    const data = new FormData(form);
    const payload = {
      name: String(data.get('name') || '').trim(),
      birthDate: String(data.get('birthDate') || ''),
      typeId: Number(data.get('typeId')),
    };

    const ownerId = form.dataset.owner;
    const petId = form.dataset.pet;
    setFormError(form, '');
    markFieldErrors(form, []);
    submitting(form, true);

    try {
      if (petId) {
        await api(`/owners/${ownerId}/pets/${petId}`, { method: 'PUT', body: payload });
        toast('Mascota actualizada');
      } else {
        await api(`/owners/${ownerId}/pets`, { method: 'POST', body: payload });
        toast('Mascota añadida');
      }
      closeModal();
      await Promise.all([loadOwners(), loadStats()]);
    } catch (error) {
      markFieldErrors(form, error.body?.errors);
      setFormError(form, errorMessage(error));
    } finally {
      submitting(form, false);
    }
  }

  /* --------------------------------------------------------------- Visitas */

  async function visitsModal({ ownerId, petId, petName }) {
    openModal(
      `
      <div class="modal-head">
        <div>
          <h2 id="modal-title">Visitas de ${escapeHtml(petName)}</h2>
          <p>Historial clínico y registro de nuevas visitas.</p>
        </div>
        ${closeButton}
      </div>
      <div class="modal-body">
        <div id="visit-list" class="visit-list" aria-live="polite">
          <div class="skeleton-card" style="height: 80px"></div>
        </div>

        <form id="visit-form" class="visit-form">
          <div class="form-error" role="alert"></div>
          <h3>Registrar visita</h3>
          <div class="form-grid">
            <div class="field field--full">
              <label for="visitDescription">Descripción</label>
              <textarea id="visitDescription" name="description" maxlength="255" required
                        placeholder="Motivo de la visita, diagnóstico, tratamiento…"></textarea>
              <span class="field-error" data-error-for="description"></span>
            </div>
            <div class="field">
              <label for="visitDate">Fecha (opcional)</label>
              <input id="visitDate" name="date" type="date" min="${tomorrow()}">
              <span class="field-hint">Si se omite, se usa mañana.</span>
              <span class="field-error" data-error-for="date"></span>
            </div>
          </div>
        </form>
      </div>
      <div class="modal-foot">
        <button type="button" class="btn btn-ghost" data-action="close-modal">Cerrar</button>
        <button type="submit" form="visit-form" class="btn btn-primary">Registrar visita</button>
      </div>`,
      { wide: true },
    );

    const form = document.getElementById('visit-form');
    form.dataset.owner = ownerId;
    form.dataset.pet = petId;
    form.addEventListener('submit', (event) => submitVisit(event, form));

    await refreshVisits(ownerId, petId);
  }

  async function refreshVisits(ownerId, petId) {
    const list = document.getElementById('visit-list');
    if (!list) return;

    try {
      const visits = await api(`/owners/${ownerId}/pets/${petId}/visits`);
      list.innerHTML = visits.length
        ? visits.map(visitItem).join('')
        : '<p class="no-pets">Aún no hay visitas registradas para esta mascota.</p>';
    } catch (error) {
      list.innerHTML = `<p class="no-pets">${escapeHtml(errorMessage(error))}</p>`;
    }
  }

  function visitItem(visit) {
    const { day, month } = visitDay(visit.date);
    return `
      <li class="visit-item">
        <div class="visit-date" aria-hidden="true"><strong>${escapeHtml(day)}</strong><small>${escapeHtml(month)}</small></div>
        <div class="visit-body">
          <p>${escapeHtml(visit.description)}</p>
          <small>${escapeHtml(formatDate(visit.date))} · visita #${visit.id}</small>
        </div>
      </li>`;
  }

  async function submitVisit(event, form) {
    event.preventDefault();
    if (!form.reportValidity()) return;

    const data = new FormData(form);
    const payload = { description: String(data.get('description') || '').trim() };
    const date = String(data.get('date') || '');
    if (date) payload.date = date;

    setFormError(form, '');
    markFieldErrors(form, []);
    submitting(form, true);

    try {
      await api(`/owners/${form.dataset.owner}/pets/${form.dataset.pet}/visits`, {
        method: 'POST',
        body: payload,
      });
      form.reset();
      toast('Visita registrada');
      await refreshVisits(form.dataset.owner, form.dataset.pet);
    } catch (error) {
      markFieldErrors(form, error.body?.errors);
      setFormError(form, errorMessage(error));
    } finally {
      submitting(form, false);
    }
  }

  /* ------------------------------------------------------------ Vets ficha */

  function vetModal(vet) {
    const specialties = vet.specialties || [];
    openModal(`
      <div class="modal-head">
        <div>
          <h2 id="modal-title">${escapeHtml(vet.firstName)} ${escapeHtml(vet.lastName)}</h2>
          <p>Ficha del profesional · ID ${vet.id}</p>
        </div>
        ${closeButton}
      </div>
      <div class="modal-body">
        <div class="card-head">
          <div class="avatar avatar--vet" aria-hidden="true">${escapeHtml(initials(vet.firstName, vet.lastName))}</div>
          <div class="card-title">
            <h3>Medicina veterinaria</h3>
            <p>Equipo de la clínica PetClinic</p>
          </div>
        </div>
        <div class="chips">
          ${
            specialties.length
              ? specialties.map((item) => `<span class="chip">${escapeHtml(item.name)}</span>`).join('')
              : '<span class="chip chip--empty">Sin especialidades registradas</span>'
          }
        </div>
      </div>
      <div class="modal-foot">
        <button type="button" class="btn btn-primary" data-action="close-modal">Cerrar</button>
      </div>`);
  }

  /* ------------------------------------------------------------- Navegación */

  function setView(view) {
    state.view = view;
    document.querySelectorAll('.nav-btn').forEach((button) => {
      button.classList.toggle('is-active', button.dataset.view === view);
    });
    document.getElementById('view-owners').hidden = view !== 'owners';
    document.getElementById('view-vets').hidden = view !== 'vets';

    // Mostrar/ocultar botón "Nuevo propietario" según la vista
    const btnNewOwner = document.getElementById('btn-new-owner');
    if (btnNewOwner) {
      btnNewOwner.hidden = view !== 'owners';
    }

    if (view === 'vets' && !state.vets.length) {
      loadVets().catch((error) => toast(errorMessage(error), 'error'));
    }

    const hash = view === 'vets' ? '#veterinarios' : '#propietarios';
    if (window.location.hash !== hash) history.replaceState(null, '', hash);
  }

  /* --------------------------------------------------------------- Eventos */

  let searchTimer = null;

  els.search.addEventListener('input', () => {
    state.search = els.search.value;
    els.clearSearch.hidden = !state.search;
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(() => {
      state.page = 1;
      loadOwners();
    }, 280);
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && els.modalRoot.innerHTML) closeModal();
  });

  document.addEventListener('click', (event) => {
    const nav = event.target.closest('.nav-btn');
    if (nav) {
      setView(nav.dataset.view);
      return;
    }

    const trigger = event.target.closest('[data-action]');
    if (!trigger) return;

    const action = trigger.dataset.action;

    if (action === 'close-modal') {
      closeModal();
      return;
    }

    if (action === 'new-owner') {
      ownerForm();
      return;
    }

    if (action === 'edit-owner') {
      const owner = state.owners.find((item) => String(item.id) === trigger.dataset.id);
      if (owner) ownerForm(owner);
      return;
    }

    if (action === 'new-pet') {
      petForm({ ownerId: trigger.dataset.owner });
      return;
    }

    if (action === 'edit-pet') {
      const owner = state.owners.find((item) => String(item.id) === trigger.dataset.owner);
      const pet = owner?.pets?.find((item) => String(item.id) === trigger.dataset.pet);
      if (pet) petForm({ ownerId: trigger.dataset.owner, pet });
      return;
    }

    if (action === 'visits') {
      visitsModal({
        ownerId: trigger.dataset.owner,
        petId: trigger.dataset.pet,
        petName: trigger.dataset.name,
      });
      return;
    }

    if (action === 'view-vet') {
      const vet = state.vets.find((item) => String(item.id) === trigger.dataset.id);
      if (vet) vetModal(vet);
      return;
    }

    if (action === 'filter-specialty') {
      state.specialty = trigger.dataset.specialty || null;
      renderVets();
      return;
    }

    if (action === 'prev-page' && state.page > 1) {
      state.page -= 1;
      loadOwners();
      return;
    }

    if (action === 'next-page' && state.page < state.totalPages) {
      state.page += 1;
      loadOwners();
      return;
    }

    if (action === 'clear-search') {
      els.search.value = '';
      state.search = '';
      els.clearSearch.hidden = true;
      state.page = 1;
      loadOwners();
      els.search.focus();
    }
  });

  /* ----------------------------------------------------------------- Init */

  async function init() {
    const hash = window.location.hash;
    setView(hash === '#veterinarios' ? 'vets' : 'owners');

    try {
      await loadPetTypes();
    } catch (error) {
      toast(errorMessage(error), 'error');
    }

    await Promise.allSettled([loadOwners(), loadVets(), loadStats()]);
  }

  init();
})();

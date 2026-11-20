import { api } from "../api.js";
import {
  bindLightboxClicks,
  calcPetAge,
  closeModal,
  defaultFutureSlot,
  esc,
  formatDate,
  formatDateTime,
  formatTimeRange,
  mountMediaPicker,
  openModal,
  renderAppointmentBadge,
  renderHealthBadge,
  safeImageUrl,
  showToast,
  toInputDateTime
} from "../utils.js";

/* =========================================================================
   1. CUSTOMER DASHBOARD / PET HUB
   ========================================================================= */
export async function renderCustomerDashboardView(container, ctx) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading your Pet Hub...</p></div></div>`;

  const user = ctx.getUser();
  let pets = [], appointments = [], timeline = [];
  try {
    [pets, appointments, timeline] = await Promise.all([
      api.getPets(),
      api.getAppointments(),
      api.getHealthTimeline()
    ]);
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  const activeAppts = appointments.filter((a) => ["PENDING", "RESCHEDULE_PROPOSED", "CONFIRMED"].includes(a.status));
  const negotiatingCount = appointments.filter((a) => a.status === "RESCHEDULE_PROPOSED").length;
  const completedCount = appointments.filter((a) => a.status === "COMPLETED").length;

  const spotlight =
    appointments.find((a) => a.status === "RESCHEDULE_PROPOSED")
    || appointments.find((a) => a.status === "CONFIRMED")
    || appointments.find((a) => a.status === "PENDING")
    || null;

  container.innerHTML = `
    <section class="hero-banner">
      <div>
        <div class="hero-pill">
          <span class="material-symbols-outlined" style="font-size:15px;">waving_hand</span>
          Pet Caregiver Portal
        </div>
        <h1>Welcome back, ${esc(user.fullName.split(" ")[0])}!</h1>
        <p>
          Track your companions' profiles, coordinate appointment schedules, and follow daily
          veterinarian-verified health updates in one synchronized clinic hub.
        </p>
      </div>
      <div class="row-gap">
        <button class="btn btn-white" id="dash-book-btn">
          <span class="material-symbols-outlined" style="font-size:18px;">add_circle</span>
          Book New Appointment
        </button>
        <button class="btn btn-translucent" id="dash-add-pet-btn">
          <span class="material-symbols-outlined" style="font-size:18px;">pets</span>
          Register New Pet
        </button>
      </div>
    </section>

    <section class="kpi-grid">
      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">Registered Pets</span>
          <span class="kpi-icon"><span class="material-symbols-outlined">pets</span></span>
        </div>
        <div class="kpi-value numeric">${pets.length}</div>
        <div class="kpi-meta">${pets.length ? pets.slice(0, 3).map((p) => esc(p.name)).join(" · ") : "Add your first companion"}</div>
      </article>

      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">Active Appointments</span>
          <span class="kpi-icon ${negotiatingCount ? "alert" : ""}">
            <span class="material-symbols-outlined">calendar_month</span>
          </span>
        </div>
        <div class="kpi-value numeric">${activeAppts.length}</div>
        <div class="kpi-meta">
          ${negotiatingCount ? `<strong class="text-primary">${negotiatingCount} reschedule proposal${negotiatingCount > 1 ? "s" : ""} awaiting review</strong>` : "Pending or confirmed visits"}
        </div>
      </article>

      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">Completed Care Visits</span>
          <span class="kpi-icon sage"><span class="material-symbols-outlined">verified</span></span>
        </div>
        <div class="kpi-value numeric">${completedCount}</div>
        <div class="kpi-meta">${timeline.length} clinician health update${timeline.length === 1 ? "" : "s"} logged</div>
      </article>
    </section>

    <!-- My Pets Preview -->
    <section class="stack-md">
      <div class="row-between">
        <div class="row-gap">
          <h2 class="card-title" style="font-size:1.2rem;">My Pets</h2>
          <span class="badge badge-neutral badge-no-dot">${pets.length} Registered</span>
        </div>
        <button class="btn btn-ghost btn-sm" id="dash-manage-pets">
          Manage all profiles
          <span class="material-symbols-outlined" style="font-size:17px;">chevron_right</span>
        </button>
      </div>

      ${pets.length ? `
        <div class="grid-2">
          ${pets.slice(0, 4).map((pet) => renderPetSummaryCard(pet)).join("")}
        </div>
      ` : `
        <div class="card">
          <div class="empty-state">
            <span class="material-symbols-outlined">pets</span>
            <h4>No pets registered yet</h4>
            <p>Register your pet profile to start requesting veterinary examinations and tracking medical records.</p>
            <button class="btn btn-primary btn-sm" id="dash-empty-add-pet">Register First Pet</button>
          </div>
        </div>
      `}
    </section>

    <!-- 2-Column Split: Appointment Spotlight + Recent Health Updates -->
    <section class="split-7-5">
      <div class="stack-md">
        <div class="row-between">
          <h2 class="card-title" style="font-size:1.15rem;">Appointment Spotlight</h2>
          <button class="btn btn-ghost btn-sm" id="dash-all-appts">View All Appointments</button>
        </div>

        ${spotlight ? `
          <div class="card stack-md">
            <div class="row-between">
              <div class="row-gap">
                <span class="kpi-icon sage"><span class="material-symbols-outlined">medical_services</span></span>
                <div>
                  <div class="fw-700" style="font-size:1.02rem;">${esc(spotlight.petName)} · APT-${spotlight.id}</div>
                  <div class="text-xs text-muted">${esc(spotlight.symptoms || "Scheduled clinical consultation")}</div>
                </div>
              </div>
              ${renderAppointmentBadge(spotlight.status)}
            </div>

            <div class="meta-grid">
              <div>
                <span class="pet-metric-label">${spotlight.confirmedStart ? "Confirmed Slot" : "Requested Slot"}</span>
                <span class="pet-metric-val numeric">${esc(formatTimeRange(spotlight.confirmedStart || spotlight.requestedStart, spotlight.confirmedEnd || spotlight.requestedEnd))}</span>
              </div>
              <div>
                <span class="pet-metric-label">Veterinarian</span>
                <span class="pet-metric-val">${esc(spotlight.veterinarianName || "Pending clinic assignment")}</span>
              </div>
            </div>

            <div class="row-between">
              <button class="btn btn-primary btn-sm" data-open-appt="${spotlight.id}">
                <span class="material-symbols-outlined" style="font-size:16px;">${spotlight.status === "RESCHEDULE_PROPOSED" ? "event_repeat" : "visibility"}</span>
                ${spotlight.status === "RESCHEDULE_PROPOSED" ? "Review Proposal & Negotiate" : "Open Details & Timeline"}
              </button>
              <button class="btn btn-ghost btn-sm" id="dash-book-another">+ New Request</button>
            </div>
          </div>
        ` : `
          <div class="card">
            <div class="empty-state">
              <span class="material-symbols-outlined">event_available</span>
              <h4>No active appointments</h4>
              <p>Need a wellness exam or symptom evaluation? Request a slot with our veterinary team.</p>
              <button class="btn btn-primary btn-sm" id="dash-empty-book">Book Appointment</button>
            </div>
          </div>
        `}
      </div>

      <div class="stack-md">
        <div class="row-between">
          <h2 class="card-title" style="font-size:1.15rem;">Recent Health Updates</h2>
          <button class="btn btn-ghost btn-sm" id="dash-view-timeline">View Full Timeline (${timeline.length})</button>
        </div>

        <div class="card stack-md">
          ${timeline.length ? timeline.slice(0, 3).map((item) => renderHealthUpdateCard(item, true)).join("") : `
            <div class="empty-state" style="padding:1.5rem;">
              <span class="material-symbols-outlined">timeline</span>
              <h4>No daily health updates yet</h4>
              <p>Clinician-authored progress logs and recovery photos will appear here after examinations.</p>
            </div>
          `}
        </div>
      </div>
    </section>
  `;

  bindLightboxClicks(container);

  document.getElementById("dash-book-btn")?.addEventListener("click", () => ctx.navigate("book"));
  document.getElementById("dash-book-another")?.addEventListener("click", () => ctx.navigate("book"));
  document.getElementById("dash-empty-book")?.addEventListener("click", () => ctx.navigate("book"));
  document.getElementById("dash-manage-pets")?.addEventListener("click", () => ctx.navigate("pets"));
  document.getElementById("dash-all-appts")?.addEventListener("click", () => ctx.navigate("appointments"));
  document.getElementById("dash-view-timeline")?.addEventListener("click", () => ctx.navigate("appointments"));

  const openNewPet = () => openPetFormModal({ onSaved: () => renderCustomerDashboardView(container, ctx) });
  document.getElementById("dash-add-pet-btn")?.addEventListener("click", openNewPet);
  document.getElementById("dash-empty-add-pet")?.addEventListener("click", openNewPet);

  container.querySelectorAll("[data-open-appt]").forEach((btn) => {
    btn.addEventListener("click", () => ctx.navigate("appointment-detail", { id: Number(btn.getAttribute("data-open-appt")) }));
  });
  container.querySelectorAll("[data-book-pet]").forEach((btn) => {
    btn.addEventListener("click", () => ctx.navigate("book", { petId: Number(btn.getAttribute("data-book-pet")) }));
  });
  container.querySelectorAll("[data-view-pet]").forEach((btn) => {
    btn.addEventListener("click", () => openPetDetailModal(Number(btn.getAttribute("data-view-pet")), ctx, () => renderCustomerDashboardView(container, ctx)));
  });
  container.querySelectorAll("[data-edit-pet]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const pet = pets.find((p) => p.id === Number(btn.getAttribute("data-edit-pet")));
      if (pet) openPetFormModal({ pet, onSaved: () => renderCustomerDashboardView(container, ctx) });
    });
  });
}

/* =========================================================================
   2. MY PETS & PET PROFILE MANAGEMENT (also supports Admin clinic directory)
   ========================================================================= */
export async function renderPetsDirectoryView(container, ctx) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading pet profiles...</p></div></div>`;

  const user = ctx.getUser();
  const isCustomer = user.role === "CUSTOMER";
  const isAdmin = user.role === "ADMIN";
  const canModify = isCustomer || isAdmin;

  let pets = [], customers = [];
  try {
    pets = await api.getPets();
    if (isAdmin) {
      customers = await api.getUsers({ role: "CUSTOMER" });
    }
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  let searchQuery = "";

  function renderGrid() {
    const q = searchQuery.trim().toLowerCase();
    const filtered = !q ? pets : pets.filter((p) =>
      `${p.name} ${p.species} ${p.breed || ""} ${p.ownerName || ""}`.toLowerCase().includes(q)
    );

    const gridSlot = document.getElementById("pets-grid-slot");
    if (!gridSlot) return;

    gridSlot.innerHTML = `
      ${filtered.map((pet) => renderPetSummaryCard(pet, { showOwner: !isCustomer, canModify, canBook: isCustomer })).join("")}
      ${canModify ? `
        <div class="pet-card" style="background:var(--surface-low);border-style:dashed;cursor:pointer;justify-content:center;align-items:center;padding:2rem;text-align:center;" id="card-add-pet">
          <span class="kpi-icon" style="width:52px;height:52px;border-radius:50%;margin-bottom:0.75rem;">
            <span class="material-symbols-outlined" style="font-size:28px;">pets</span>
          </span>
          <h3 style="margin:0 0 0.25rem;font-size:1rem;">Register New Pet</h3>
          <p class="text-sm text-muted" style="margin:0 0 0.75rem;">Add companion details, weight, and profile photos.</p>
          <span class="text-sm fw-700 text-primary">+ Add Companion</span>
        </div>
      ` : ""}
    `;

    if (!filtered.length && !canModify) {
      gridSlot.innerHTML = `<div class="card col-span-full"><div class="empty-state"><span class="material-symbols-outlined">pets</span><h4>No matching pets found</h4></div></div>`;
    }

    const reload = () => renderPetsDirectoryView(container, ctx);

    document.getElementById("card-add-pet")?.addEventListener("click", () => {
      openPetFormModal({ customers: isAdmin ? customers : null, onSaved: reload });
    });

    gridSlot.querySelectorAll("[data-view-pet]").forEach((btn) => {
      btn.addEventListener("click", () => openPetDetailModal(Number(btn.getAttribute("data-view-pet")), ctx, reload));
    });
    gridSlot.querySelectorAll("[data-edit-pet]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const pet = pets.find((p) => p.id === Number(btn.getAttribute("data-edit-pet")));
        if (pet) openPetFormModal({ pet, onSaved: reload });
      });
    });
    gridSlot.querySelectorAll("[data-book-pet]").forEach((btn) => {
      btn.addEventListener("click", () => ctx.navigate("book", { petId: Number(btn.getAttribute("data-book-pet")) }));
    });
  }

  container.innerHTML = `
    <div class="page-header">
      <div>
        <span class="eyebrow">${isCustomer ? "Companion Directory" : "Clinic Patients"}</span>
        <h1>${isCustomer ? "My Pets" : "Clinic Pet Directory"}</h1>
        <p>${isCustomer
          ? "Manage pet profiles, biometrics, photos, and review certified medical records and visit history."
          : "Browse registered clinic patients, owner details, and medical histories."}</p>
      </div>
      <div class="header-actions">
        <div class="input-with-icon" style="width:240px;">
          <span class="material-symbols-outlined leading-icon">search</span>
          <input id="pets-search-input" class="input" placeholder="Search pets, breed, owner...">
        </div>
        ${canModify ? `
          <button class="btn btn-primary" id="btn-register-pet">
            <span class="material-symbols-outlined" style="font-size:18px;">add</span>
            Register New Pet
          </button>
        ` : ""}
      </div>
    </div>

    <div class="grid-3" id="pets-grid-slot"></div>
  `;

  document.getElementById("pets-search-input")?.addEventListener("input", (e) => {
    searchQuery = e.target.value || "";
    renderGrid();
  });

  document.getElementById("btn-register-pet")?.addEventListener("click", () => {
    openPetFormModal({
      customers: isAdmin ? customers : null,
      onSaved: () => renderPetsDirectoryView(container, ctx)
    });
  });

  renderGrid();
}

function renderPetSummaryCard(pet, { showOwner = false, canModify = true, canBook = true } = {}) {
  const thumb = safeImageUrl(pet.images?.[0]?.imageUrl);
  return `
    <article class="pet-card">
      <div class="pet-card-body">
        <div class="row-gap" style="align-items:flex-start;gap:0.85rem;">
          <div class="pet-thumb">
            ${thumb
              ? `<img src="${esc(thumb)}" alt="${esc(pet.name)}">`
              : `<span class="material-symbols-outlined" style="font-size:30px;">pets</span>`}
          </div>
          <div style="flex:1;min-width:0;">
            <div class="row-between" style="gap:0.35rem;">
              <span class="eyebrow" style="color:var(--outline);">${esc(pet.species)}</span>
              ${pet.images?.length ? `<span class="text-xs text-muted">${pet.images.length} photo${pet.images.length === 1 ? "" : "s"}</span>` : ""}
            </div>
            <h3 style="margin:0.1rem 0 0;font-size:1.1rem;color:var(--ink-cypress);">${esc(pet.name)}</h3>
            <div class="text-sm text-muted">${esc(pet.breed || "Mixed / Unspecified")}</div>
            ${showOwner ? `<div class="text-xs text-primary fw-600" style="margin-top:3px;">Owner: ${esc(pet.ownerName || `User #${pet.ownerId}`)}</div>` : ""}
          </div>
        </div>

        <div class="pet-metrics">
          <div>
            <span class="pet-metric-label">Age</span>
            <span class="pet-metric-val numeric">${esc(calcPetAge(pet.dateOfBirth))}</span>
          </div>
          <div>
            <span class="pet-metric-label">Gender</span>
            <span class="pet-metric-val">${esc(pet.gender || "UNKNOWN")}</span>
          </div>
          <div>
            <span class="pet-metric-label">Weight</span>
            <span class="pet-metric-val numeric">${pet.weight != null ? `${pet.weight} kg` : "—"}</span>
          </div>
        </div>

        ${pet.description ? `<p class="text-xs text-muted pre-wrap" style="margin:0;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">${esc(pet.description)}</p>` : ""}
      </div>

      <div class="pet-card-footer">
        <button type="button" class="btn btn-ghost btn-sm" data-view-pet="${pet.id}">View Profile</button>
        <div class="row-gap" style="gap:0.35rem;">
          ${canModify ? `
            <button type="button" class="btn btn-tonal btn-icon" data-edit-pet="${pet.id}" title="Edit Pet">
              <span class="material-symbols-outlined" style="font-size:17px;">edit</span>
            </button>
          ` : ""}
          ${canBook ? `
            <button type="button" class="btn btn-primary btn-sm" data-book-pet="${pet.id}">
              <span class="material-symbols-outlined" style="font-size:15px;">event</span>
              Request Visit
            </button>
          ` : ""}
        </div>
      </div>
    </article>
  `;
}

export function openPetFormModal({ pet = null, customers = null, onSaved = null } = {}) {
  const isEdit = Boolean(pet);
  openModal({
    title: isEdit ? `Edit Pet — ${pet.name}` : "Register New Pet",
    large: true,
    bodyHtml: `
      <form id="pet-modal-form" class="stack-md">
        <div class="form-grid-3">
          ${!isEdit && customers ? `
            <div class="field col-span-full">
              <label for="pet-owner-select">Pet Owner (Customer) <span class="req">*</span></label>
              <select id="pet-owner-select" name="ownerId" class="select" required>
                <option value="">Select a customer...</option>
                ${customers.map((c) => `<option value="${c.id}">${esc(c.fullName)} (${esc(c.email)})</option>`).join("")}
              </select>
            </div>
          ` : ""}

          <div class="field">
            <label for="pet-name">Pet Name <span class="req">*</span></label>
            <input id="pet-name" name="name" class="input" required maxlength="100" value="${esc(pet?.name || "")}" placeholder="Luna">
          </div>

          <div class="field">
            <label for="pet-species">Species <span class="req">*</span></label>
            <input id="pet-species" name="species" class="input" required maxlength="50" list="species-presets" value="${esc(pet?.species || "")}" placeholder="Cat, Dog...">
            <datalist id="species-presets">
              <option value="Cat"></option>
              <option value="Dog"></option>
              <option value="Rabbit"></option>
              <option value="Bird"></option>
            </datalist>
          </div>

          <div class="field">
            <label for="pet-breed">Breed</label>
            <input id="pet-breed" name="breed" class="input" maxlength="100" value="${esc(pet?.breed || "")}" placeholder="Ragdoll, Golden Retriever...">
          </div>

          <div class="field">
            <label for="pet-gender">Gender</label>
            <select id="pet-gender" name="gender" class="select">
              <option value="MALE" ${pet?.gender === "MALE" ? "selected" : ""}>Male</option>
              <option value="FEMALE" ${pet?.gender === "FEMALE" ? "selected" : ""}>Female</option>
              <option value="UNKNOWN" ${!pet?.gender || pet?.gender === "UNKNOWN" ? "selected" : ""}>Unknown</option>
            </select>
          </div>

          <div class="field">
            <label for="pet-dob">Date of Birth</label>
            <input id="pet-dob" name="dateOfBirth" type="date" class="input" value="${esc(pet?.dateOfBirth || "")}">
          </div>

          <div class="field">
            <label for="pet-weight">Weight (kg)</label>
            <input id="pet-weight" name="weight" type="number" step="0.01" min="0" max="999.99" class="input" value="${pet?.weight != null ? esc(pet.weight) : ""}" placeholder="4.20">
          </div>

          <div class="field col-span-full">
            <label for="pet-desc">Caregiver Notes / Allergies / Description</label>
            <textarea id="pet-desc" name="description" class="textarea" placeholder="Coat color, known allergies, temperament, special care notes...">${esc(pet?.description || "")}</textarea>
          </div>
        </div>

        <div id="pet-form-media-picker"></div>

        <div class="row-between" style="padding-top:0.5rem;border-top:1px solid var(--border);">
          <button type="button" class="btn btn-secondary" id="pet-form-cancel">Cancel</button>
          <button type="submit" class="btn btn-primary" id="pet-form-save">
            <span class="material-symbols-outlined" style="font-size:18px;">save</span>
            ${isEdit ? "Save Profile" : "Register Pet"}
          </button>
        </div>
      </form>
    `,
    onMount: () => {
      const picker = mountMediaPicker(document.getElementById("pet-form-media-picker"), {
        label: isEdit ? "Add Additional Pet Photos (Optional)" : "Pet Profile Photos (Optional)"
      });

      document.getElementById("pet-form-cancel")?.addEventListener("click", closeModal);
      document.getElementById("pet-modal-form")?.addEventListener("submit", async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const weightRaw = String(fd.get("weight") || "").trim();
        const payload = {
          name: String(fd.get("name") || "").trim(),
          species: String(fd.get("species") || "").trim(),
          breed: String(fd.get("breed") || "").trim() || null,
          gender: String(fd.get("gender") || "UNKNOWN"),
          dateOfBirth: String(fd.get("dateOfBirth") || "") || null,
          weight: weightRaw ? Number(weightRaw) : null,
          description: String(fd.get("description") || "").trim() || null
        };
        const ownerId = fd.get("ownerId") ? Number(fd.get("ownerId")) : null;
        const btn = document.getElementById("pet-form-save");
        btn.disabled = true;

        try {
          const savedPet = isEdit
            ? await api.updatePet(pet.id, payload)
            : await api.createPet(payload, ownerId);

          for (const url of picker.getUrls()) {
            await api.addPetImage(savedPet.id, url);
          }

          closeModal();
          showToast(isEdit ? "Pet profile updated." : `${savedPet.name} registered successfully!`, "success");
          if (typeof onSaved === "function") onSaved(savedPet);
        } catch (err) {
          showToast(err.message, "error");
          btn.disabled = false;
        }
      });
    }
  });
}

export async function openPetDetailModal(petId, ctx, onChanged = null) {
  const user = ctx.getUser();
  const canModify = user.role === "CUSTOMER" || user.role === "ADMIN";

  let pet, records = [], visits = [];
  try {
    const [petData, recData, apptData] = await Promise.all([
      api.getPetById(petId),
      api.getMedicalRecords({ petId }),
      api.getAppointments()
    ]);
    pet = petData;
    records = recData || [];
    visits = (apptData || []).filter((a) => a.petId === petId);
  } catch (err) {
    showToast(err.message, "error");
    return;
  }

  let activeTab = "medical";

  openModal({
    title: `${pet.name} — Companion Profile`,
    large: true,
    bodyHtml: `
      <div class="callout callout-teal">
        <span class="material-symbols-outlined text-primary" style="font-size:19px;">verified</span>
        <span>Medical records and clinician health updates are certified and read-only for pet parents.</span>
      </div>

      <div class="split-7-5">
        <div class="stack-sm">
          <div class="row-between">
            <span class="eyebrow" style="color:var(--outline);">Pet Gallery (${pet.images?.length || 0})</span>
            ${canModify ? `<button type="button" class="btn btn-ghost btn-sm" id="pet-detail-add-photo">+ Add Photo</button>` : ""}
          </div>
          ${pet.images?.length ? `
            <div class="media-grid">
              ${pet.images.map((img) => `
                <div class="media-thumb" data-lightbox-url="${esc(img.imageUrl)}">
                  <img src="${esc(safeImageUrl(img.imageUrl))}" alt="${esc(pet.name)} photo">
                  ${canModify ? `
                    <button type="button" class="media-remove-btn" data-del-pet-img="${img.id}" title="Delete photo">
                      <span class="material-symbols-outlined" style="font-size:15px;">delete</span>
                    </button>
                  ` : ""}
                </div>
              `).join("")}
            </div>
          ` : `<div class="card-soft text-sm text-muted">No photos uploaded yet.</div>`}
        </div>

        <div class="card-soft stack-sm">
          <div class="row-between">
            <h4 style="margin:0;font-size:1.1rem;">${esc(pet.name)}</h4>
            <span class="badge badge-neutral badge-no-dot">${esc(pet.species)}</span>
          </div>
          <div class="text-sm text-muted">${esc(pet.breed || "Unspecified breed")} · Owner: ${esc(pet.ownerName || `User #${pet.ownerId}`)}</div>
          <div class="pet-metrics" style="background:var(--surface-lowest);">
            <div><span class="pet-metric-label">Age</span><span class="pet-metric-val numeric">${esc(calcPetAge(pet.dateOfBirth))}</span></div>
            <div><span class="pet-metric-label">Gender</span><span class="pet-metric-val">${esc(pet.gender || "UNKNOWN")}</span></div>
            <div><span class="pet-metric-label">Weight</span><span class="pet-metric-val numeric">${pet.weight != null ? `${pet.weight} kg` : "—"}</span></div>
          </div>
          <div class="text-xs text-muted">Birth date: <strong class="numeric">${esc(formatDate(pet.dateOfBirth))}</strong></div>
          ${pet.description ? `<p class="text-sm pre-wrap" style="margin:0.25rem 0 0;">${esc(pet.description)}</p>` : ""}
        </div>
      </div>

      <div class="filter-bar" style="border-bottom:1px solid var(--border);padding-bottom:0.6rem;">
        <button type="button" class="pill-tab active" data-pet-tab="medical">Medical Record History <span class="count">${records.length}</span></button>
        <button type="button" class="pill-tab" data-pet-tab="visits">Appointment Visits <span class="count">${visits.length}</span></button>
      </div>

      <div id="pet-detail-tab-slot" class="stack-sm"></div>

      <div class="row-between" style="padding-top:0.5rem;border-top:1px solid var(--border);">
        <div class="row-gap">
          ${canModify ? `
            <button type="button" class="btn btn-secondary btn-sm" id="pet-detail-edit-btn">
              <span class="material-symbols-outlined" style="font-size:16px;">edit</span> Edit Profile
            </button>
            <button type="button" class="btn btn-danger-ghost btn-sm" id="pet-detail-delete-btn">
              <span class="material-symbols-outlined" style="font-size:16px;">delete</span> Delete Pet
            </button>
          ` : ""}
        </div>
        <div class="row-gap">
          ${user.role === "CUSTOMER" ? `
            <button type="button" class="btn btn-primary btn-sm" id="pet-detail-book-btn">Request Appointment</button>
          ` : ""}
          <button type="button" class="btn btn-tonal btn-sm" id="pet-detail-close-btn">Close</button>
        </div>
      </div>
    `,
    onMount: (modalRoot) => {
      bindLightboxClicks(modalRoot);

      function renderTab() {
        const slot = document.getElementById("pet-detail-tab-slot");
        if (!slot) return;
        if (activeTab === "medical") {
          slot.innerHTML = records.length ? records.map((r) => `
            <div class="card-soft stack-sm">
              <div class="row-between">
                <div class="fw-700">${esc(r.diagnosis || `Medical Record #${r.id}`)}</div>
                <span class="text-xs text-muted numeric">${esc(formatDate(r.createdAt))}</span>
              </div>
              <div class="text-xs text-muted">
                Attending: <strong>${esc(r.veterinarianName || "Clinic Vet")}</strong>
                ${r.weight != null ? ` · Weight: ${r.weight} kg` : ""}
                ${r.temperature != null ? ` · Temp: ${r.temperature}°C` : ""}
              </div>
              ${r.treatment ? `<p class="text-sm pre-wrap" style="margin:0;"><strong>Treatment:</strong> ${esc(r.treatment)}</p>` : ""}
              ${r.advice ? `<p class="text-sm pre-wrap" style="margin:0;"><strong>Advice:</strong> ${esc(r.advice)}</p>` : ""}
            </div>
          `).join("") : `<p class="text-sm text-muted">No medical records saved for ${esc(pet.name)} yet.</p>`;
        } else {
          slot.innerHTML = visits.length ? visits.map((v) => `
            <div class="card-soft row-between">
              <div>
                <div class="row-gap">
                  <span class="fw-700 numeric">APT-${v.id}</span>
                  ${renderAppointmentBadge(v.status)}
                </div>
                <div class="text-xs text-muted numeric" style="margin-top:3px;">
                  ${esc(formatTimeRange(v.confirmedStart || v.requestedStart, v.confirmedEnd || v.requestedEnd))}
                  · ${esc(v.veterinarianName || "Unassigned")}
                </div>
              </div>
              <button type="button" class="btn btn-secondary btn-sm" data-modal-open-appt="${v.id}">Details</button>
            </div>
          `).join("") : `<p class="text-sm text-muted">No appointment visits recorded for ${esc(pet.name)} yet.</p>`;

          slot.querySelectorAll("[data-modal-open-appt]").forEach((btn) => {
            btn.addEventListener("click", () => {
              closeModal();
              ctx.navigate("appointment-detail", { id: Number(btn.getAttribute("data-modal-open-appt")) });
            });
          });
        }
      }

      modalRoot.querySelectorAll("[data-pet-tab]").forEach((btn) => {
        btn.addEventListener("click", () => {
          activeTab = btn.getAttribute("data-pet-tab");
          modalRoot.querySelectorAll("[data-pet-tab]").forEach((b) => b.classList.toggle("active", b === btn));
          renderTab();
        });
      });

      document.getElementById("pet-detail-close-btn")?.addEventListener("click", closeModal);
      document.getElementById("pet-detail-book-btn")?.addEventListener("click", () => {
        closeModal();
        ctx.navigate("book", { petId: pet.id });
      });
      document.getElementById("pet-detail-edit-btn")?.addEventListener("click", () => {
        openPetFormModal({ pet, onSaved: () => { if (onChanged) onChanged(); } });
      });
      document.getElementById("pet-detail-delete-btn")?.addEventListener("click", async () => {
        if (!window.confirm(`Delete ${pet.name}? Pets with existing appointments or medical records cannot be deleted.`)) return;
        try {
          await api.deletePet(pet.id);
          closeModal();
          showToast(`${pet.name} deleted.`, "info");
          if (onChanged) onChanged();
        } catch (err) {
          showToast(err.message, "error");
        }
      });

      document.getElementById("pet-detail-add-photo")?.addEventListener("click", () => {
        openModal({
          title: `Add Photos — ${pet.name}`,
          bodyHtml: `
            <div id="pet-extra-media"></div>
            <div class="row-between">
              <button type="button" class="btn btn-secondary" id="pet-extra-cancel">Back</button>
              <button type="button" class="btn btn-primary" id="pet-extra-save">Attach Photos</button>
            </div>
          `,
          onMount: () => {
            const picker = mountMediaPicker(document.getElementById("pet-extra-media"), { label: "Upload or Paste Photo URL" });
            document.getElementById("pet-extra-cancel")?.addEventListener("click", () => openPetDetailModal(pet.id, ctx, onChanged));
            document.getElementById("pet-extra-save")?.addEventListener("click", async () => {
              const urls = picker.getUrls();
              if (!urls.length) { showToast("Add at least one photo first.", "error"); return; }
              try {
                for (const u of urls) await api.addPetImage(pet.id, u);
                showToast("Photos attached.", "success");
                if (onChanged) onChanged();
                openPetDetailModal(pet.id, ctx, onChanged);
              } catch (err) {
                showToast(err.message, "error");
              }
            });
          }
        });
      });

      modalRoot.querySelectorAll("[data-del-pet-img]").forEach((btn) => {
        btn.addEventListener("click", async (e) => {
          e.stopPropagation();
          const imgId = Number(btn.getAttribute("data-del-pet-img"));
          try {
            await api.deletePetImage(pet.id, imgId);
            showToast("Photo removed.", "info");
            if (onChanged) onChanged();
            openPetDetailModal(pet.id, ctx, onChanged);
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      });

      renderTab();
    }
  });
}

/* =========================================================================
   3. CREATE APPOINTMENT REQUEST (CUSTOMER)
   ========================================================================= */
export async function renderCreateAppointmentView(container, ctx, params = {}) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Preparing appointment request...</p></div></div>`;

  let pets = [], vets = [];
  try {
    [pets, vets] = await Promise.all([api.getPets(), api.getActiveVeterinarians()]);
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  if (!pets.length) {
    container.innerHTML = `
      <div class="page-header">
        <div>
          <span class="eyebrow">Appointment Request</span>
          <h1>Request an Appointment</h1>
        </div>
      </div>
      <div class="card">
        <div class="empty-state">
          <span class="material-symbols-outlined">pets</span>
          <h4>Register a pet first</h4>
          <p>You need at least one pet profile in your account before submitting an appointment request.</p>
          <button class="btn btn-primary" id="book-need-pet-btn">Register New Pet</button>
        </div>
      </div>
    `;
    document.getElementById("book-need-pet-btn")?.addEventListener("click", () => {
      openPetFormModal({ onSaved: () => renderCreateAppointmentView(container, ctx, params) });
    });
    return;
  }

  const slot = defaultFutureSlot(24, 45);
  let selectedPetId = params.petId && pets.some((p) => p.id === Number(params.petId))
    ? Number(params.petId)
    : pets[0].id;

  container.innerHTML = `
    <div class="page-header">
      <div>
        <nav class="breadcrumb">
          <a href="#appointments" id="book-bc-appts">Appointments</a>
          <span class="material-symbols-outlined" style="font-size:15px;">chevron_right</span>
          <span>Request New Appointment</span>
        </nav>
        <h1>Request an Appointment</h1>
        <p>Select your companion, choose a preferred time slot and optional veterinarian, describe symptoms, and attach photos to help our triage team prepare.</p>
      </div>
    </div>

    <div class="split-8-4">
      <form id="create-appt-form" class="stack-lg">
        <!-- Step 1: Select Pet -->
        <section class="card stack-md">
          <div class="row-between">
            <div class="row-gap">
              <span class="avatar" style="width:28px;height:28px;font-size:0.78rem;">1</span>
              <div>
                <h2 class="card-title">Select Your Pet</h2>
                <p class="card-subtitle">Choose which companion needs clinical care</p>
              </div>
            </div>
            <button type="button" class="btn btn-ghost btn-sm" id="book-add-pet-inline">+ Register New Pet</button>
          </div>

          <div class="grid-2" id="book-pet-choices">
            ${pets.map((p) => {
              const thumb = safeImageUrl(p.images?.[0]?.imageUrl);
              return `
                <label class="choice-card ${p.id === selectedPetId ? "selected" : ""}" data-pet-choice="${p.id}">
                  <input type="radio" name="petId" value="${p.id}" ${p.id === selectedPetId ? "checked" : ""} hidden>
                  <div class="pet-thumb" style="width:52px;height:52px;">
                    ${thumb ? `<img src="${esc(thumb)}" alt="${esc(p.name)}">` : `<span class="material-symbols-outlined">pets</span>`}
                  </div>
                  <div style="flex:1;min-width:0;">
                    <div class="fw-700">${esc(p.name)}</div>
                    <div class="text-xs text-muted">${esc(p.species)}${p.breed ? ` · ${esc(p.breed)}` : ""}</div>
                    <div class="text-xs text-muted numeric">${esc(calcPetAge(p.dateOfBirth))}${p.weight != null ? ` · ${p.weight} kg` : ""}</div>
                  </div>
                </label>
              `;
            }).join("")}
          </div>
        </section>

        <!-- Step 2: Time & Veterinarian -->
        <section class="card stack-md">
          <div class="row-gap">
            <span class="avatar" style="width:28px;height:28px;font-size:0.78rem;">2</span>
            <div>
              <h2 class="card-title">Preferred Schedule &amp; Veterinarian</h2>
              <p class="card-subtitle">Propose a future time window; clinic staff can confirm or negotiate an alternative slot.</p>
            </div>
          </div>

          <div class="form-grid">
            <div class="field">
              <label for="book-start">Requested Start <span class="req">*</span></label>
              <input id="book-start" name="requestedStart" type="datetime-local" class="input" required value="${esc(slot.start)}">
            </div>
            <div class="field">
              <label for="book-end">Requested End <span class="req">*</span></label>
              <input id="book-end" name="requestedEnd" type="datetime-local" class="input" required value="${esc(slot.end)}">
            </div>
            <div class="field col-span-full">
              <div class="row-gap">
                <span class="text-xs text-muted">Quick duration:</span>
                <button type="button" class="btn btn-tonal btn-sm" data-dur="30">30 min</button>
                <button type="button" class="btn btn-tonal btn-sm" data-dur="45">45 min</button>
                <button type="button" class="btn btn-tonal btn-sm" data-dur="60">60 min</button>
              </div>
            </div>
            <div class="field col-span-full">
              <label for="book-vet">Preferred Veterinarian <span class="text-muted">(Optional)</span></label>
              <select id="book-vet" name="veterinarianId" class="select">
                <option value="">Let Meocare clinic assign an available veterinarian</option>
                ${vets.map((v) => `<option value="${v.id}">${esc(v.fullName)} (${esc(v.email)})</option>`).join("")}
              </select>
            </div>
          </div>
        </section>

        <!-- Step 3: Symptoms -->
        <section class="card stack-md">
          <div class="row-gap">
            <span class="avatar" style="width:28px;height:28px;font-size:0.78rem;">3</span>
            <div>
              <h2 class="card-title">Clinical Symptoms &amp; Observations</h2>
              <p class="card-subtitle">Share signs, onset duration, appetite or energy changes</p>
            </div>
          </div>

          <div class="field">
            <label for="book-symptoms">Reason for Visit &amp; Observations</label>
            <textarea id="book-symptoms" name="symptoms" class="textarea" rows="4"
                      placeholder="e.g., Persistent ear scratching for 2 days, mild redness behind left ear, normal appetite..."></textarea>
          </div>
        </section>

        <!-- Step 4: Symptom Photos -->
        <section class="card stack-md">
          <div class="row-gap">
            <span class="avatar" style="width:28px;height:28px;font-size:0.78rem;">4</span>
            <div>
              <h2 class="card-title">Symptom Photos &amp; Media</h2>
              <p class="card-subtitle">Attach clear photos to assist veterinary triage</p>
            </div>
          </div>
          <div id="book-media-picker"></div>
        </section>

        <div class="card row-between">
          <button type="button" class="btn btn-secondary" id="book-cancel-btn">Cancel</button>
          <button type="submit" class="btn btn-primary btn-lg" id="book-submit-btn">
            <span>Submit Appointment Request</span>
            <span class="material-symbols-outlined" style="font-size:19px;">arrow_forward</span>
          </button>
        </div>
      </form>

      <!-- Right Sidebar: Summary & Process -->
      <aside class="stack-md">
        <section class="card stack-md" id="book-summary-card">
          <h3 class="card-title">
            <span class="material-symbols-outlined text-primary">receipt_long</span>
            Request Summary
          </h3>
          <div class="card-soft stack-sm" id="book-summary-details"></div>
        </section>

        <section class="card stack-md">
          <h3 class="card-title">
            <span class="material-symbols-outlined text-primary">conversion_path</span>
            How Scheduling Works
          </h3>
          <div class="timeline" style="padding-left:2rem;">
            <div class="timeline-item">
              <div class="timeline-node primary" style="width:24px;height:24px;left:-2rem;font-size:12px;">1</div>
              <div>
                <div class="fw-600 text-sm">Request Submitted</div>
                <div class="text-xs text-muted">Your request enters Meocare's triage queue in Pending status.</div>
              </div>
            </div>
            <div class="timeline-item">
              <div class="timeline-node violet" style="width:24px;height:24px;left:-2rem;font-size:12px;">2</div>
              <div>
                <div class="fw-600 text-sm">Confirm or Counter-Propose</div>
                <div class="text-xs text-muted">Our veterinarian confirms your slot or proposes an alternative time with rationale.</div>
              </div>
            </div>
            <div class="timeline-item">
              <div class="timeline-node secondary" style="width:24px;height:24px;left:-2rem;font-size:12px;">3</div>
              <div>
                <div class="fw-600 text-sm">Visit &amp; Daily Timeline</div>
                <div class="text-xs text-muted">After your exam, certified medical records and daily health updates appear in your portal.</div>
              </div>
            </div>
          </div>
        </section>
      </aside>
    </div>
  `;

  const mediaPicker = mountMediaPicker(document.getElementById("book-media-picker"), {
    label: "Symptom Photos (Optional)"
  });

  const startInput = document.getElementById("book-start");
  const endInput = document.getElementById("book-end");
  const vetSelect = document.getElementById("book-vet");

  function updateSummary() {
    const pet = pets.find((p) => p.id === selectedPetId) || pets[0];
    const vetOpt = vetSelect.options[vetSelect.selectedIndex]?.text || "Clinic Assigned";
    document.getElementById("book-summary-details").innerHTML = `
      <div class="row-between text-sm"><span class="text-muted">Pet</span><strong>${esc(pet.name)} (${esc(pet.species)})</strong></div>
      <div class="row-between text-sm"><span class="text-muted">Slot</span><strong class="numeric">${esc(formatTimeRange(startInput.value, endInput.value))}</strong></div>
      <div class="row-between text-sm"><span class="text-muted">Veterinarian</span><span>${esc(vetSelect.value ? vetOpt.split(" (")[0] : "Clinic Assigned")}</span></div>
      <div class="row-between text-sm"><span class="text-muted">Clinic</span><span>Meocare Veterinary Clinic</span></div>
    `;
  }

  container.querySelectorAll("[data-pet-choice]").forEach((label) => {
    label.addEventListener("click", () => {
      selectedPetId = Number(label.getAttribute("data-pet-choice"));
      container.querySelectorAll("[data-pet-choice]").forEach((l) => {
        const isSelected = Number(l.getAttribute("data-pet-choice")) === selectedPetId;
        l.classList.toggle("selected", isSelected);
        const radio = l.querySelector("input[type=radio]");
        if (radio) radio.checked = isSelected;
      });
      updateSummary();
    });
  });

  container.querySelectorAll("[data-dur]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const mins = Number(btn.getAttribute("data-dur"));
      const s = new Date(startInput.value);
      if (!Number.isNaN(s.getTime())) {
        endInput.value = toInputDateTime(new Date(s.getTime() + mins * 60_000));
        updateSummary();
      }
    });
  });

  startInput?.addEventListener("change", updateSummary);
  endInput?.addEventListener("change", updateSummary);
  vetSelect?.addEventListener("change", updateSummary);
  document.getElementById("book-bc-appts")?.addEventListener("click", (e) => { e.preventDefault(); ctx.navigate("appointments"); });
  document.getElementById("book-cancel-btn")?.addEventListener("click", () => ctx.navigate("appointments"));
  document.getElementById("book-add-pet-inline")?.addEventListener("click", () => {
    openPetFormModal({ onSaved: (created) => renderCreateAppointmentView(container, ctx, { petId: created.id }) });
  });

  document.getElementById("create-appt-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const startVal = startInput.value;
    const endVal = endInput.value;
    if (new Date(startVal) <= new Date()) {
      showToast("Requested start time must be in the future.", "error");
      return;
    }
    if (new Date(endVal) <= new Date(startVal)) {
      showToast("Requested end time must be after start time.", "error");
      return;
    }

    const btn = document.getElementById("book-submit-btn");
    btn.disabled = true;
    try {
      const created = await api.createAppointment({
        petId: selectedPetId,
        veterinarianId: vetSelect.value ? Number(vetSelect.value) : null,
        requestedStart: startVal,
        requestedEnd: endVal,
        symptoms: String(document.getElementById("book-symptoms")?.value || "").trim() || null,
        imageUrls: mediaPicker.getUrls()
      });
      showToast(`Appointment APT-${created.id} submitted!`, "success");
      ctx.navigate("appointment-detail", { id: created.id });
    } catch (err) {
      showToast(err.message, "error");
      btn.disabled = false;
    }
  });

  updateSummary();
}

/* =========================================================================
   4. CUSTOMER APPOINTMENTS & READ-ONLY DAILY HEALTH TIMELINE
   ========================================================================= */
export async function renderCustomerAppointmentsTimelineView(container, ctx) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading appointments &amp; health timelines...</p></div></div>`;

  let appointments = [], timeline = [], pets = [];
  try {
    [appointments, timeline, pets] = await Promise.all([
      api.getAppointments(),
      api.getHealthTimeline(),
      api.getPets()
    ]);
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  let statusFilter = "ALL";
  let timelinePetFilter = "";

  const counts = {
    ALL: appointments.length,
    RESCHEDULE_PROPOSED: appointments.filter((a) => a.status === "RESCHEDULE_PROPOSED").length,
    PENDING: appointments.filter((a) => a.status === "PENDING").length,
    CONFIRMED: appointments.filter((a) => a.status === "CONFIRMED").length,
    COMPLETED: appointments.filter((a) => a.status === "COMPLETED").length,
    CLOSED: appointments.filter((a) => ["CANCELLED", "REJECTED"].includes(a.status)).length
  };

  container.innerHTML = `
    <div class="page-header">
      <div>
        <span class="eyebrow">Care Companion Record</span>
        <h1>Appointments &amp; Health Timelines</h1>
        <p>Manage scheduled visits, respond to veterinarian reschedule proposals, and review daily clinician-verified health updates.</p>
      </div>
      <div class="header-actions">
        ${counts.RESCHEDULE_PROPOSED ? `
          <span class="badge badge-reschedule">${counts.RESCHEDULE_PROPOSED} Proposal${counts.RESCHEDULE_PROPOSED > 1 ? "s" : ""} Need Review</span>
        ` : ""}
        <button class="btn btn-primary" id="appts-book-new">
          <span class="material-symbols-outlined" style="font-size:18px;">add_circle</span>
          Book New Visit
        </button>
      </div>
    </div>

    <div class="split-7-5">
      <!-- Left: Appointment Schedule Hub -->
      <section class="card stack-md">
        <div class="row-between">
          <h2 class="card-title">
            <span class="material-symbols-outlined text-primary">event_available</span>
            Appointments Schedule
          </h2>
        </div>

        <div class="filter-bar" id="appt-status-tabs">
          <button type="button" class="pill-tab active" data-st="ALL">All <span class="count">${counts.ALL}</span></button>
          <button type="button" class="pill-tab" data-st="RESCHEDULE_PROPOSED">Reschedule Proposed <span class="count">${counts.RESCHEDULE_PROPOSED}</span></button>
          <button type="button" class="pill-tab" data-st="CONFIRMED">Confirmed <span class="count">${counts.CONFIRMED}</span></button>
          <button type="button" class="pill-tab" data-st="PENDING">Pending <span class="count">${counts.PENDING}</span></button>
          <button type="button" class="pill-tab" data-st="COMPLETED">Completed <span class="count">${counts.COMPLETED}</span></button>
          <button type="button" class="pill-tab" data-st="CLOSED">Cancelled / Rejected <span class="count">${counts.CLOSED}</span></button>
        </div>

        <div id="customer-appts-list" class="stack-md"></div>
      </section>

      <!-- Right: Clinician-Verified Read-Only Daily Health Timeline -->
      <aside class="stack-md">
        <div class="callout callout-teal">
          <span class="material-symbols-outlined text-primary">verified</span>
          <div>
            <div class="fw-700">Clinician-Verified Daily Health Timeline</div>
            <div class="text-xs">Chronological recovery observations posted directly by Meocare veterinarians. Read-only for pet parents.</div>
          </div>
        </div>

        <section class="card stack-md">
          <div class="row-between">
            <h2 class="card-title">
              <span class="material-symbols-outlined text-primary">vital_signs</span>
              Health Timeline
            </h2>
            <select id="timeline-pet-filter" class="select" style="width:auto;min-width:140px;padding:0.35rem 0.65rem;font-size:0.8rem;">
              <option value="">All My Pets</option>
              ${pets.map((p) => `<option value="${p.id}">${esc(p.name)}</option>`).join("")}
            </select>
          </div>

          <div id="customer-timeline-slot"></div>
        </section>
      </aside>
    </div>
  `;

  function renderAppointmentsList() {
    const slot = document.getElementById("customer-appts-list");
    const list = appointments.filter((a) => {
      if (statusFilter === "ALL") return true;
      if (statusFilter === "CLOSED") return ["CANCELLED", "REJECTED"].includes(a.status);
      return a.status === statusFilter;
    });

    if (!list.length) {
      slot.innerHTML = `
        <div class="empty-state">
          <span class="material-symbols-outlined">event_note</span>
          <h4>No appointments in this filter</h4>
          <p>Switch status filters or submit a new appointment request.</p>
        </div>
      `;
      return;
    }

    slot.innerHTML = list.map((a) => {
      const accent =
        a.status === "RESCHEDULE_PROPOSED" ? "accent-negotiation"
          : a.status === "CONFIRMED" ? "accent-confirmed"
          : a.status === "PENDING" ? "accent-pending" : "";
      const canCancel = ["PENDING", "RESCHEDULE_PROPOSED", "CONFIRMED"].includes(a.status);
      return `
        <article class="appt-card ${accent}">
          <div class="row-between">
            <div>
              <div class="row-gap">
                <span class="fw-700" style="font-size:1rem;">${esc(a.petName || `Pet #${a.petId}`)}</span>
                <span class="text-xs text-muted numeric">APT-${a.id}</span>
              </div>
              <div class="text-xs text-muted" style="margin-top:2px;">${esc(a.symptoms || "Clinical consultation")}</div>
            </div>
            ${renderAppointmentBadge(a.status)}
          </div>

          <div class="meta-grid">
            <div>
              <span class="pet-metric-label">${a.confirmedStart ? "Confirmed Time" : "Requested Time"}</span>
              <span class="pet-metric-val numeric">${esc(formatTimeRange(a.confirmedStart || a.requestedStart, a.confirmedEnd || a.requestedEnd))}</span>
            </div>
            <div>
              <span class="pet-metric-label">Veterinarian</span>
              <span class="pet-metric-val">${esc(a.veterinarianName || "Pending Assignment")}</span>
            </div>
          </div>

          <div class="row-between">
            <div class="row-gap">
              <button type="button" class="btn ${a.status === "RESCHEDULE_PROPOSED" ? "btn-primary" : "btn-secondary"} btn-sm" data-open-appt="${a.id}">
                <span class="material-symbols-outlined" style="font-size:16px;">${a.status === "RESCHEDULE_PROPOSED" ? "event_repeat" : "visibility"}</span>
                ${a.status === "RESCHEDULE_PROPOSED" ? "Review Proposal" : "Details & Negotiation"}
              </button>
              ${a.status === "COMPLETED" ? `
                <button type="button" class="btn btn-tonal btn-sm" data-open-rec-appt="${a.id}">Medical Record</button>
              ` : ""}
            </div>
            ${canCancel ? `
              <button type="button" class="btn btn-danger-ghost btn-sm" data-cancel-appt="${a.id}">Cancel</button>
            ` : ""}
          </div>
        </article>
      `;
    }).join("");

    slot.querySelectorAll("[data-open-appt]").forEach((btn) => {
      btn.addEventListener("click", () => ctx.navigate("appointment-detail", { id: Number(btn.getAttribute("data-open-appt")) }));
    });
    slot.querySelectorAll("[data-open-rec-appt]").forEach((btn) => {
      btn.addEventListener("click", () => ctx.navigate("records", { appointmentId: Number(btn.getAttribute("data-open-rec-appt")) }));
    });
    slot.querySelectorAll("[data-cancel-appt]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = Number(btn.getAttribute("data-cancel-appt"));
        if (!window.confirm(`Cancel appointment APT-${id}?`)) return;
        try {
          await api.cancelAppointment(id);
          showToast("Appointment cancelled.", "info");
          renderCustomerAppointmentsTimelineView(container, ctx);
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });
  }

  function renderTimelineFeed() {
    const slot = document.getElementById("customer-timeline-slot");
    const list = timelinePetFilter
      ? timeline.filter((u) => String(u.petId) === timelinePetFilter)
      : timeline;

    if (!list.length) {
      slot.innerHTML = `
        <div class="empty-state" style="padding:1.5rem;">
          <span class="material-symbols-outlined">timeline</span>
          <h4>No daily health updates</h4>
          <p>Veterinarian daily check-in updates and recovery photos will appear here chronologically.</p>
        </div>
      `;
      return;
    }

    slot.innerHTML = `<div class="timeline">${list.map((item) => renderHealthUpdateTimelineNode(item)).join("")}</div>`;
    bindLightboxClicks(slot);
  }

  document.getElementById("appts-book-new")?.addEventListener("click", () => ctx.navigate("book"));
  document.getElementById("appt-status-tabs")?.querySelectorAll("[data-st]").forEach((btn) => {
    btn.addEventListener("click", () => {
      statusFilter = btn.getAttribute("data-st");
      document.getElementById("appt-status-tabs").querySelectorAll("[data-st]").forEach((b) => b.classList.toggle("active", b === btn));
      renderAppointmentsList();
    });
  });
  document.getElementById("timeline-pet-filter")?.addEventListener("change", (e) => {
    timelinePetFilter = e.target.value;
    renderTimelineFeed();
  });

  renderAppointmentsList();
  renderTimelineFeed();
}

/* =========================================================================
   5. CUSTOMER MEDICAL RECORDS (READ-ONLY)
   ========================================================================= */
export async function renderCustomerRecordsView(container, ctx, params = {}) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading medical records...</p></div></div>`;

  let pets = [], records = [], timeline = [];
  try {
    [pets, records, timeline] = await Promise.all([
      api.getPets(),
      api.getMedicalRecords(),
      api.getHealthTimeline()
    ]);
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  let selectedPetId = params.petId ? String(params.petId) : "";
  const updatesByRecord = new Map();
  timeline.forEach((u) => {
    if (!updatesByRecord.has(u.medicalRecordId)) updatesByRecord.set(u.medicalRecordId, []);
    updatesByRecord.get(u.medicalRecordId).push(u);
  });

  container.innerHTML = `
    <div class="page-header">
      <div>
        <span class="eyebrow">Certified Clinical Ledger</span>
        <h1>Medical Records</h1>
        <p>Read-only examination records, vitals, diagnoses, treatment plans, and linked daily health updates authored by Meocare veterinarians.</p>
      </div>
      <div class="header-actions">
        <select id="cust-rec-pet-filter" class="select" style="width:auto;min-width:180px;">
          <option value="">All My Pets</option>
          ${pets.map((p) => `<option value="${p.id}" ${String(p.id) === selectedPetId ? "selected" : ""}>${esc(p.name)}</option>`).join("")}
        </select>
      </div>
    </div>

    <div id="cust-records-list" class="stack-md"></div>
  `;

  function renderList() {
    const slot = document.getElementById("cust-records-list");
    let list = selectedPetId ? records.filter((r) => String(r.petId) === selectedPetId) : records;
    if (params.appointmentId) {
      const matched = list.filter((r) => r.appointmentId === Number(params.appointmentId));
      if (matched.length) list = matched;
    }

    if (!list.length) {
      slot.innerHTML = `
        <div class="card">
          <div class="empty-state">
            <span class="material-symbols-outlined">clinical_notes</span>
            <h4>No medical records found</h4>
            <p>Certified examination records appear here once your veterinarian completes a clinical evaluation.</p>
          </div>
        </div>
      `;
      return;
    }

    slot.innerHTML = list.map((r) => {
      const updates = updatesByRecord.get(r.id) || [];
      return `
        <article class="card stack-md">
          <div class="row-between">
            <div>
              <div class="row-gap">
                <h2 class="card-title">${esc(r.petName || `Pet #${r.petId}`)} — ${esc(r.diagnosis || "Clinical Examination")}</h2>
                <span class="badge badge-completed badge-no-dot">MED-${r.id}</span>
              </div>
              <div class="text-xs text-muted numeric" style="margin-top:3px;">
                Examined by <strong>${esc(r.veterinarianName || "Clinic Veterinarian")}</strong>
                · ${esc(formatDateTime(r.createdAt))} · APT-${r.appointmentId}
              </div>
            </div>
            <div class="row-gap">
              ${r.weight != null ? `<span class="badge badge-primary badge-no-dot numeric">${r.weight} kg</span>` : ""}
              ${r.temperature != null ? `<span class="badge badge-confirmed badge-no-dot numeric">${r.temperature} °C</span>` : ""}
            </div>
          </div>

          <div class="grid-2">
            ${r.examination ? `<div class="card-soft"><div class="eyebrow" style="color:var(--outline);">Examination Findings</div><p class="text-sm pre-wrap" style="margin:4px 0 0;">${esc(r.examination)}</p></div>` : ""}
            ${r.diagnosis ? `<div class="card-soft"><div class="eyebrow" style="color:var(--outline);">Diagnosis</div><p class="text-sm pre-wrap" style="margin:4px 0 0;">${esc(r.diagnosis)}</p></div>` : ""}
            ${r.treatment ? `<div class="card-soft"><div class="eyebrow" style="color:var(--outline);">Treatment Plan</div><p class="text-sm pre-wrap" style="margin:4px 0 0;">${esc(r.treatment)}</p></div>` : ""}
            ${r.advice ? `<div class="card-soft"><div class="eyebrow" style="color:var(--outline);">Caregiver Advice</div><p class="text-sm pre-wrap" style="margin:4px 0 0;">${esc(r.advice)}</p></div>` : ""}
          </div>

          ${r.notes ? `<div class="card-soft"><div class="eyebrow" style="color:var(--outline);">Additional Clinical Notes</div><p class="text-sm pre-wrap" style="margin:4px 0 0;">${esc(r.notes)}</p></div>` : ""}

          ${r.images?.length ? `
            <div class="stack-sm">
              <span class="eyebrow" style="color:var(--outline);">Diagnostic Media (${r.images.length})</span>
              <div class="media-grid">
                ${r.images.map((img) => `
                  <div class="media-thumb" data-lightbox-url="${esc(img.imageUrl)}">
                    <img src="${esc(safeImageUrl(img.imageUrl))}" alt="Diagnostic image">
                  </div>
                `).join("")}
              </div>
            </div>
          ` : ""}

          ${updates.length ? `
            <div class="stack-sm" style="padding-top:0.75rem;border-top:1px solid var(--border);">
              <span class="eyebrow text-primary">Linked Daily Health Updates (${updates.length})</span>
              <div class="timeline">
                ${updates.map((u) => renderHealthUpdateTimelineNode(u)).join("")}
              </div>
            </div>
          ` : ""}
        </article>
      `;
    }).join("");

    bindLightboxClicks(slot);
  }

  document.getElementById("cust-rec-pet-filter")?.addEventListener("change", (e) => {
    selectedPetId = e.target.value;
    params.appointmentId = null;
    renderList();
  });

  renderList();
}

export function renderHealthUpdateCard(item, compact = false) {
  return `
    <div class="card-soft stack-sm">
      <div class="row-between">
        <div class="row-gap">
          <span class="fw-700">${esc(item.petName || `Pet #${item.petId}`)}</span>
          ${renderHealthBadge(item.healthStatus)}
        </div>
        <span class="text-xs text-muted numeric">${esc(formatDateTime(item.createdAt))}</span>
      </div>
      <div class="text-xs text-muted">
        <span class="material-symbols-outlined text-primary" style="font-size:14px;">stethoscope</span>
        ${esc(item.veterinarianName || "Attending Veterinarian")} · MED-${item.medicalRecordId}
      </div>
      ${item.description ? `<p class="text-sm pre-wrap" style="margin:0;color:var(--ink);">${esc(item.description)}</p>` : ""}
      ${!compact && item.images?.length ? `
        <div class="media-grid" style="margin-top:4px;">
          ${item.images.map((img) => `
            <div class="media-thumb" data-lightbox-url="${esc(img.imageUrl)}">
              <img src="${esc(safeImageUrl(img.imageUrl))}" alt="Health progress photo">
            </div>
          `).join("")}
        </div>
      ` : ""}
    </div>
  `;
}

export function renderHealthUpdateTimelineNode(item) {
  return `
    <div class="timeline-item">
      <div class="timeline-node secondary">
        <span class="material-symbols-outlined" style="font-size:16px;">vital_signs</span>
      </div>
      <div class="timeline-card">
        <div class="row-between">
          <div class="row-gap">
            <span class="fw-700">${esc(item.petName || `Pet #${item.petId}`)}</span>
            ${renderHealthBadge(item.healthStatus)}
          </div>
          <span class="text-xs text-muted numeric">${esc(formatDateTime(item.createdAt))}</span>
        </div>
        <div class="text-xs text-muted">
          Dr. / Clinician: <strong>${esc(item.veterinarianName || "Clinic Veterinarian")}</strong> · Record #MED-${item.medicalRecordId}
        </div>
        ${item.description ? `<p class="text-sm pre-wrap" style="margin:0;color:var(--ink);">${esc(item.description)}</p>` : ""}
        ${item.images?.length ? `
          <div class="media-grid" style="margin-top:4px;">
            ${item.images.map((img) => `
              <div class="media-thumb" data-lightbox-url="${esc(img.imageUrl)}">
                <img src="${esc(safeImageUrl(img.imageUrl))}" alt="Clinical progress attachment">
              </div>
            `).join("")}
          </div>
        ` : ""}
      </div>
    </div>
  `;
}

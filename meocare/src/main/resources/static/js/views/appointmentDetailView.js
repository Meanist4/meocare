import { api } from "../api.js";
import {
  bindLightboxClicks,
  calcPetAge,
  closeModal,
  defaultFutureSlot,
  esc,
  formatDateTime,
  formatTimeRange,
  mountMediaPicker,
  openModal,
  renderAppointmentBadge,
  renderProposalBadge,
  renderRoleBadge,
  safeImageUrl,
  showToast,
  toInputDateTime
} from "../utils.js";

export async function renderAppointmentDetailView(container, ctx, appointmentId) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading appointment details...</p></div></div>`;

  const user = ctx.getUser();
  const isAdmin = user.role === "ADMIN";
  const isVet = user.role === "VETERINARIAN";
  const isCustomer = user.role === "CUSTOMER";

  let appt, proposals, pet = null, vets = [], medicalRecord = null;
  try {
    const [apptData, proposalsData, vetsData] = await Promise.all([
      api.getAppointmentById(appointmentId),
      api.getProposals(appointmentId),
      api.getActiveVeterinarians()
    ]);
    appt = apptData;
    proposals = proposalsData || [];
    vets = vetsData || [];

    const [petRes, recordRes] = await Promise.allSettled([
      appt.petId ? api.getPetById(appt.petId) : Promise.resolve(null),
      api.getMedicalRecordByAppointmentId(appointmentId)
    ]);
    if (petRes.status === "fulfilled") pet = petRes.value;
    if (recordRes.status === "fulfilled") medicalRecord = recordRes.value;
  } catch (err) {
    if (err.status === 403) {
      container.innerHTML = `
        <div class="callout callout-error">
          <span class="material-symbols-outlined">lock</span>
          <div><strong>Access Denied:</strong> ${esc(err.message)}</div>
        </div>
        <div><button class="btn btn-secondary" id="back-appts-403">Back to Appointments</button></div>
      `;
      document.getElementById("back-appts-403")?.addEventListener("click", () => ctx.navigate("appointments"));
      return;
    }
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  const isTerminal = ["REJECTED", "CANCELLED", "COMPLETED"].includes(appt.status);
  const canNegotiate = ["PENDING", "RESCHEDULE_PROPOSED", "CONFIRMED"].includes(appt.status);
  const canConfirmOrReject = (isAdmin || isVet) && ["PENDING", "RESCHEDULE_PROPOSED"].includes(appt.status);
  const canComplete = (isAdmin || isVet) && appt.status === "CONFIRMED";
  const canManagePhotos = isAdmin || (isCustomer && appt.customerId === user.id);
  const canOpenMedicalRecord = (isAdmin || isVet) && ["CONFIRMED", "COMPLETED"].includes(appt.status);

  const pendingProposals = proposals.filter((p) => p.status === "PENDING");
  const latestPending = pendingProposals[0] || null;
  const primaryPetPhoto = safeImageUrl(pet?.images?.[0]?.imageUrl);

  const reload = () => renderAppointmentDetailView(container, ctx, appointmentId);

  container.innerHTML = `
    <div class="row-between">
      <nav class="breadcrumb" aria-label="Breadcrumb">
        <a href="#appointments" id="bc-appointments">Appointments</a>
        <span class="material-symbols-outlined" style="font-size:15px;">chevron_right</span>
        <span class="numeric fw-600" style="color:var(--ink);">APT-${appt.id}</span>
        <span class="material-symbols-outlined" style="font-size:15px;">chevron_right</span>
        <span class="text-primary fw-600">Details &amp; Negotiation</span>
      </nav>
      <div class="row-gap">
        ${renderAppointmentBadge(appt.status)}
        <span class="text-xs text-muted numeric">Updated ${esc(formatDateTime(appt.updatedAt || appt.createdAt))}</span>
      </div>
    </div>

    <div class="split-8-4">
      <!-- Left 8 cols: Active Negotiation, Actions & Audit Trail -->
      <div class="stack-lg">
        ${latestPending ? renderActiveProposalBanner(appt, latestPending, user) : ""}

        <!-- Appointment Overview & Action Bar -->
        <section class="card stack-md">
          <div class="row-between">
            <div>
              <span class="eyebrow">Appointment #APT-${appt.id}</span>
              <h1 style="margin:0.2rem 0 0;font-size:1.35rem;color:var(--ink-cypress);">
                ${esc(appt.petName || `Pet #${appt.petId}`)} — Clinical Consultation
              </h1>
            </div>
            ${renderAppointmentBadge(appt.status)}
          </div>

          <div class="meta-grid">
            <div>
              <span class="pet-metric-label">Requested Slot</span>
              <span class="pet-metric-val numeric">${esc(formatTimeRange(appt.requestedStart, appt.requestedEnd))}</span>
            </div>
            <div>
              <span class="pet-metric-label">Confirmed Slot</span>
              <span class="pet-metric-val numeric" style="color:${appt.confirmedStart ? "var(--secondary)" : "var(--outline)"};">
                ${appt.confirmedStart ? esc(formatTimeRange(appt.confirmedStart, appt.confirmedEnd)) : "Not confirmed yet"}
              </span>
            </div>
            <div>
              <span class="pet-metric-label">Assigned Veterinarian</span>
              <span class="pet-metric-val">${esc(appt.veterinarianName || "Unassigned (Triage Queue)")}</span>
            </div>
          </div>

          <div class="card-soft stack-sm">
            <span class="eyebrow" style="color:var(--outline);">Clinical Symptoms &amp; Owner Notes</span>
            <p class="pre-wrap" style="margin:0;color:var(--ink);">
              ${appt.symptoms ? esc(appt.symptoms) : '<span class="text-muted">No symptoms described.</span>'}
            </p>
          </div>

          <div class="row-between" style="padding-top:0.35rem;border-top:1px solid var(--border);">
            <div class="row-gap">
              ${canConfirmOrReject ? `
                <button class="btn btn-primary" id="btn-confirm-appt">
                  <span class="material-symbols-outlined" style="font-size:18px;">check_circle</span>
                  Confirm Appointment
                </button>
              ` : ""}
              ${canNegotiate ? `
                <button class="btn btn-secondary" id="btn-propose-slot">
                  <span class="material-symbols-outlined" style="font-size:18px;">event_repeat</span>
                  Propose Alternative Slot
                </button>
              ` : ""}
              ${(isAdmin || (isVet && !appt.veterinarianId)) && !isTerminal ? `
                <button class="btn btn-tonal" id="btn-assign-vet">
                  <span class="material-symbols-outlined" style="font-size:18px;">person_add</span>
                  ${appt.veterinarianId ? "Reassign Vet" : (isVet ? "Assign to Me" : "Assign Veterinarian")}
                </button>
              ` : ""}
              ${canComplete ? `
                <button class="btn btn-primary" id="btn-complete-appt">
                  <span class="material-symbols-outlined" style="font-size:18px;">task_alt</span>
                  Mark Completed
                </button>
              ` : ""}
              ${canOpenMedicalRecord ? `
                <button class="btn btn-tonal" id="btn-open-record">
                  <span class="material-symbols-outlined" style="font-size:18px;">clinical_notes</span>
                  ${medicalRecord ? "Open Medical Record" : "Create Medical Record"}
                </button>
              ` : ""}
              ${isCustomer && medicalRecord ? `
                <button class="btn btn-tonal" id="btn-view-customer-record">
                  <span class="material-symbols-outlined" style="font-size:18px;">clinical_notes</span>
                  View Medical Record
                </button>
              ` : ""}
            </div>

            <div class="row-gap">
              ${canConfirmOrReject ? `
                <button class="btn btn-danger-ghost btn-sm" id="btn-reject-appt">
                  <span class="material-symbols-outlined" style="font-size:17px;">block</span>
                  Reject
                </button>
              ` : ""}
              ${!isTerminal ? `
                <button class="btn btn-danger-ghost btn-sm" id="btn-cancel-appt">
                  <span class="material-symbols-outlined" style="font-size:17px;">cancel</span>
                  Cancel Appointment
                </button>
              ` : ""}
            </div>
          </div>
        </section>

        <!-- Negotiation History Timeline -->
        <section class="card stack-md">
          <div class="row-between">
            <h2 class="card-title">
              <span class="material-symbols-outlined text-primary">history</span>
              Negotiation History &amp; Lifecycle
            </h2>
            <span class="badge badge-neutral badge-no-dot">${proposals.length} Proposal${proposals.length === 1 ? "" : "s"}</span>
          </div>

          <div class="timeline">
            ${proposals.map((p) => renderProposalTimelineNode(p, user, canNegotiate)).join("")}

            <div class="timeline-item">
              <div class="timeline-node neutral">
                <span class="material-symbols-outlined" style="font-size:16px;">edit_calendar</span>
              </div>
              <div class="timeline-card">
                <div class="row-between">
                  <div class="row-gap">
                    <span class="fw-700">Appointment Request Created</span>
                    ${renderRoleBadge("CUSTOMER")}
                  </div>
                  <span class="text-xs text-muted numeric">${esc(formatDateTime(appt.createdAt))}</span>
                </div>
                <p style="margin:0;font-size:0.85rem;color:var(--ink-variant);">
                  <strong>${esc(appt.customerName || "Pet Parent")}</strong> requested a visit for
                  <strong>${esc(appt.petName || "Pet")}</strong> on
                  <strong class="numeric">${esc(formatTimeRange(appt.requestedStart, appt.requestedEnd))}</strong>.
                </p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- Right 4 cols: Patient, Caregiver, Symptom Photos, Medical Record -->
      <aside class="stack-md">
        <section class="card stack-sm">
          <div class="row-between">
            <span class="eyebrow" style="color:var(--outline);">Patient Details</span>
            ${pet?.species ? `<span class="badge badge-neutral badge-no-dot">${esc(pet.species)}</span>` : ""}
          </div>
          <div class="row-gap" style="gap:0.85rem;">
            <div class="pet-thumb">
              ${primaryPetPhoto
                ? `<img src="${esc(primaryPetPhoto)}" alt="${esc(appt.petName)}">`
                : `<span class="material-symbols-outlined" style="font-size:30px;">pets</span>`}
            </div>
            <div class="stack-sm" style="gap:2px;">
              <div class="fw-700" style="font-size:1.05rem;">${esc(pet?.name || appt.petName || `Pet #${appt.petId}`)}</div>
              <div class="text-sm text-muted">${esc(pet?.breed || pet?.species || "Clinic Patient")}</div>
              <div class="text-xs text-muted numeric">
                ${pet ? `${esc(calcPetAge(pet.dateOfBirth))}${pet.weight ? ` · ${pet.weight} kg` : ""}${pet.gender ? ` · ${pet.gender}` : ""}` : ""}
              </div>
            </div>
          </div>
          ${pet?.description ? `<p class="text-xs text-muted pre-wrap" style="margin:0.25rem 0 0;">${esc(pet.description)}</p>` : ""}
        </section>

        <section class="card stack-sm">
          <span class="eyebrow" style="color:var(--outline);">Caregiver &amp; Clinician</span>
          <div class="card-soft stack-sm">
            <div class="row-between">
              <span class="text-xs text-muted">Pet Parent</span>
              <span class="text-sm fw-600">${esc(appt.customerName || `Customer #${appt.customerId}`)}</span>
            </div>
            <div class="row-between">
              <span class="text-xs text-muted">Veterinarian</span>
              <span class="text-sm fw-600">${esc(appt.veterinarianName || "Unassigned")}</span>
            </div>
          </div>
        </section>

        <section class="card stack-sm">
          <div class="row-between">
            <span class="eyebrow" style="color:var(--outline);">Submitted Symptom Photos</span>
            ${canManagePhotos ? `
              <button type="button" class="btn btn-ghost btn-sm" id="btn-add-appt-photo">
                <span class="material-symbols-outlined" style="font-size:16px;">add_photo_alternate</span>
                Add Photo
              </button>
            ` : ""}
          </div>
          ${appt.images?.length ? `
            <div class="media-grid">
              ${appt.images.map((img) => `
                <div class="media-thumb" data-lightbox-url="${esc(img.imageUrl)}">
                  <img src="${esc(safeImageUrl(img.imageUrl))}" alt="Symptom attachment">
                  ${canManagePhotos ? `
                    <button type="button" class="media-remove-btn" data-delete-appt-img="${img.id}" title="Remove photo">
                      <span class="material-symbols-outlined" style="font-size:15px;">delete</span>
                    </button>
                  ` : ""}
                </div>
              `).join("")}
            </div>
          ` : `<p class="text-sm text-muted" style="margin:0;">No symptom photos attached to this appointment.</p>`}
        </section>

        ${medicalRecord ? `
          <section class="card stack-sm">
            <div class="row-between">
              <span class="eyebrow text-secondary">Linked Medical Record</span>
              <span class="badge badge-completed badge-no-dot">MED-${medicalRecord.id}</span>
            </div>
            <p class="text-sm" style="margin:0;">
              <strong>Diagnosis:</strong> ${esc(medicalRecord.diagnosis || "Recorded")}
            </p>
            <p class="text-xs text-muted" style="margin:0;">
              Examined by ${esc(medicalRecord.veterinarianName)} on ${esc(formatDateTime(medicalRecord.createdAt))}
            </p>
            <button type="button" class="btn btn-secondary btn-sm" id="sidebar-open-record">
              Open Full Record
            </button>
          </section>
        ` : ""}
      </aside>
    </div>
  `;

  bindLightboxClicks(container);

  document.getElementById("bc-appointments")?.addEventListener("click", (e) => {
    e.preventDefault();
    ctx.navigate("appointments");
  });

  // Accept / Decline proposal buttons (from active callout or timeline nodes)
  container.querySelectorAll("[data-accept-proposal]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const propId = Number(btn.getAttribute("data-accept-proposal"));
      btn.disabled = true;
      try {
        await api.acceptProposal(appt.id, propId);
        showToast("Proposal accepted! Appointment slot is now confirmed.", "success");
        await reload();
      } catch (err) {
        showToast(err.message, "error");
        btn.disabled = false;
      }
    });
  });

  container.querySelectorAll("[data-decline-proposal]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const propId = Number(btn.getAttribute("data-decline-proposal"));
      btn.disabled = true;
      try {
        await api.declineProposal(appt.id, propId);
        showToast("Proposal declined. You may propose another time slot.", "info");
        await reload();
      } catch (err) {
        showToast(err.message, "error");
        btn.disabled = false;
      }
    });
  });

  // Propose alternative slot modal
  const openProposalModal = () => {
    const slot = defaultFutureSlot(48, 45);
    openModal({
      title: `Propose Alternative Slot — APT-${appt.id}`,
      bodyHtml: `
        <form id="proposal-modal-form" class="stack-md">
          <div class="callout callout-info">
            <span class="material-symbols-outlined text-primary" style="font-size:19px;">event_repeat</span>
            <span>Submitting a proposal transitions this appointment to <strong>Reschedule Proposed</strong> until the other party responds.</span>
          </div>
          <div class="form-grid">
            <div class="field">
              <label for="prop-start">Proposed Start <span class="req">*</span></label>
              <input id="prop-start" name="proposedStart" type="datetime-local" class="input" required value="${esc(slot.start)}">
            </div>
            <div class="field">
              <label for="prop-end">Proposed End <span class="req">*</span></label>
              <input id="prop-end" name="proposedEnd" type="datetime-local" class="input" required value="${esc(slot.end)}">
            </div>
          </div>
          <div class="field">
            <label for="prop-note">Reason / Clinical Note</label>
            <textarea id="prop-note" name="note" class="textarea" maxlength="500"
                      placeholder="Explain why this alternative time works better..."></textarea>
          </div>
          <div class="row-between">
            <button type="button" class="btn btn-secondary" id="prop-cancel-btn">Cancel</button>
            <button type="submit" class="btn btn-primary" id="prop-submit-btn">Send Proposal</button>
          </div>
        </form>
      `,
      onMount: () => {
        document.getElementById("prop-cancel-btn")?.addEventListener("click", closeModal);
        document.getElementById("proposal-modal-form")?.addEventListener("submit", async (e) => {
          e.preventDefault();
          const fd = new FormData(e.target);
          const proposedStart = String(fd.get("proposedStart") || "");
          const proposedEnd = String(fd.get("proposedEnd") || "");
          const note = String(fd.get("note") || "").trim();
          const btn = document.getElementById("prop-submit-btn");
          btn.disabled = true;
          try {
            await api.createProposal(appt.id, { proposedStart, proposedEnd, note: note || null });
            closeModal();
            showToast("Reschedule proposal submitted.", "success");
            await reload();
          } catch (err) {
            showToast(err.message, "error");
            btn.disabled = false;
          }
        });
      }
    });
  };

  document.getElementById("btn-propose-slot")?.addEventListener("click", openProposalModal);
  document.getElementById("callout-counter-btn")?.addEventListener("click", openProposalModal);

  // Confirm appointment modal (Admin/Vet)
  document.getElementById("btn-confirm-appt")?.addEventListener("click", () => {
    openModal({
      title: `Confirm Appointment — APT-${appt.id}`,
      bodyHtml: `
        <form id="confirm-appt-form" class="stack-md">
          <div class="form-grid">
            <div class="field">
              <label for="conf-start">Confirmed Start <span class="req">*</span></label>
              <input id="conf-start" name="confirmedStart" type="datetime-local" class="input" required
                     value="${esc(toInputDateTime(appt.confirmedStart || appt.requestedStart))}">
            </div>
            <div class="field">
              <label for="conf-end">Confirmed End <span class="req">*</span></label>
              <input id="conf-end" name="confirmedEnd" type="datetime-local" class="input" required
                     value="${esc(toInputDateTime(appt.confirmedEnd || appt.requestedEnd))}">
            </div>
            ${isAdmin ? `
              <div class="field col-span-full">
                <label for="conf-vet">Assigned Veterinarian</label>
                <select id="conf-vet" name="veterinarianId" class="select">
                  <option value="">Keep current (${esc(appt.veterinarianName || "Unassigned")})</option>
                  ${vets.map((v) => `<option value="${v.id}" ${v.id === appt.veterinarianId ? "selected" : ""}>${esc(v.fullName)} (${esc(v.email)})</option>`).join("")}
                </select>
              </div>
            ` : ""}
          </div>
          <div class="row-between">
            <button type="button" class="btn btn-secondary" id="conf-cancel-btn">Cancel</button>
            <button type="submit" class="btn btn-primary" id="conf-submit-btn">Confirm Slot</button>
          </div>
        </form>
      `,
      onMount: () => {
        document.getElementById("conf-cancel-btn")?.addEventListener("click", closeModal);
        document.getElementById("confirm-appt-form")?.addEventListener("submit", async (e) => {
          e.preventDefault();
          const fd = new FormData(e.target);
          const payload = {
            confirmedStart: String(fd.get("confirmedStart") || ""),
            confirmedEnd: String(fd.get("confirmedEnd") || ""),
            veterinarianId: fd.get("veterinarianId") ? Number(fd.get("veterinarianId")) : null
          };
          const btn = document.getElementById("conf-submit-btn");
          btn.disabled = true;
          try {
            await api.confirmAppointment(appt.id, payload);
            closeModal();
            showToast("Appointment confirmed!", "success");
            await reload();
          } catch (err) {
            showToast(err.message, "error");
            btn.disabled = false;
          }
        });
      }
    });
  });

  // Assign Veterinarian (Admin chooses any active vet; Vet self-assigns if unassigned)
  document.getElementById("btn-assign-vet")?.addEventListener("click", async () => {
    if (isVet && !isAdmin) {
      try {
        await api.assignVeterinarian(appt.id, user.id);
        showToast("Appointment assigned to you.", "success");
        await reload();
      } catch (err) {
        showToast(err.message, "error");
      }
      return;
    }

    openModal({
      title: `Assign Veterinarian — APT-${appt.id}`,
      bodyHtml: `
        <form id="assign-vet-form" class="stack-md">
          <div class="field">
            <label for="assign-vet-select">Select Active Veterinarian <span class="req">*</span></label>
            <select id="assign-vet-select" name="veterinarianId" class="select" required>
              <option value="">Choose a veterinarian...</option>
              ${vets.map((v) => `<option value="${v.id}" ${v.id === appt.veterinarianId ? "selected" : ""}>${esc(v.fullName)} — ${esc(v.email)}</option>`).join("")}
            </select>
          </div>
          <div class="row-between">
            <button type="button" class="btn btn-secondary" id="assign-cancel-btn">Cancel</button>
            <button type="submit" class="btn btn-primary" id="assign-submit-btn">Save Assignment</button>
          </div>
        </form>
      `,
      onMount: () => {
        document.getElementById("assign-cancel-btn")?.addEventListener("click", closeModal);
        document.getElementById("assign-vet-form")?.addEventListener("submit", async (e) => {
          e.preventDefault();
          const vetId = Number(new FormData(e.target).get("veterinarianId"));
          try {
            await api.assignVeterinarian(appt.id, vetId);
            closeModal();
            showToast("Veterinarian assigned.", "success");
            await reload();
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      }
    });
  });

  document.getElementById("btn-complete-appt")?.addEventListener("click", async () => {
    try {
      await api.updateAppointmentStatus(appt.id, "COMPLETED");
      showToast("Appointment marked as completed.", "success");
      await reload();
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  document.getElementById("btn-reject-appt")?.addEventListener("click", async () => {
    if (!window.confirm(`Reject appointment APT-${appt.id}?`)) return;
    try {
      await api.rejectAppointment(appt.id);
      showToast("Appointment rejected.", "info");
      await reload();
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  document.getElementById("btn-cancel-appt")?.addEventListener("click", async () => {
    if (!window.confirm(`Cancel appointment APT-${appt.id}?`)) return;
    try {
      await api.cancelAppointment(appt.id);
      showToast("Appointment cancelled.", "info");
      await reload();
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  const openRecords = () => ctx.navigate("records", { appointmentId: appt.id, recordId: medicalRecord?.id || null });
  document.getElementById("btn-open-record")?.addEventListener("click", openRecords);
  document.getElementById("btn-view-customer-record")?.addEventListener("click", openRecords);
  document.getElementById("sidebar-open-record")?.addEventListener("click", openRecords);

  // Add symptom photos modal
  document.getElementById("btn-add-appt-photo")?.addEventListener("click", () => {
    openModal({
      title: `Add Symptom Photos — APT-${appt.id}`,
      bodyHtml: `
        <div id="appt-photo-picker"></div>
        <div class="row-between" style="margin-top:0.5rem;">
          <button type="button" class="btn btn-secondary" id="appt-photo-cancel">Cancel</button>
          <button type="button" class="btn btn-primary" id="appt-photo-save">Attach Photos</button>
        </div>
      `,
      onMount: () => {
        const picker = mountMediaPicker(document.getElementById("appt-photo-picker"), {
          label: "Select or Upload Symptom Photos"
        });
        document.getElementById("appt-photo-cancel")?.addEventListener("click", closeModal);
        document.getElementById("appt-photo-save")?.addEventListener("click", async () => {
          const urls = picker.getUrls();
          if (!urls.length) {
            showToast("Please upload or add at least one image URL.", "error");
            return;
          }
          try {
            for (const url of urls) {
              await api.addAppointmentImage(appt.id, url);
            }
            closeModal();
            showToast("Symptom photos attached.", "success");
            await reload();
          } catch (err) {
            showToast(err.message, "error");
          }
        });
      }
    });
  });

  container.querySelectorAll("[data-delete-appt-img]").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      e.stopPropagation();
      const imageId = Number(btn.getAttribute("data-delete-appt-img"));
      try {
        await api.deleteAppointmentImage(appt.id, imageId);
        showToast("Photo removed.", "info");
        await reload();
      } catch (err) {
        showToast(err.message, "error");
      }
    });
  });
}

function renderActiveProposalBanner(appt, proposal, currentUser) {
  const isOwnProposal = proposal.proposedById === currentUser.id;
  return `
    <section class="card stack-md" style="border-color:var(--primary-container);box-shadow:var(--shadow-md);">
      <div class="row-between">
        <span class="badge badge-reschedule">Active Counter-Proposal (#PRP-${proposal.id})</span>
        <span class="text-xs text-muted numeric">${esc(formatDateTime(proposal.createdAt))}</span>
      </div>

      <div class="form-grid">
        <div class="card-soft">
          <span class="eyebrow" style="color:var(--outline);">Original Requested Slot</span>
          <div class="numeric fw-600 text-muted" style="text-decoration:line-through;margin-top:4px;">
            ${esc(formatTimeRange(appt.requestedStart, appt.requestedEnd))}
          </div>
        </div>
        <div class="card" style="padding:0.85rem;border-color:var(--primary-container);">
          <span class="eyebrow">Proposed New Slot</span>
          <div class="numeric fw-700 text-primary" style="margin-top:4px;">
            ${esc(formatTimeRange(proposal.proposedStart, proposal.proposedEnd))}
          </div>
        </div>
      </div>

      <div class="card-soft stack-sm">
        <div class="row-gap">
          <span class="fw-700">${esc(proposal.proposedByName || "Clinic Participant")}</span>
          ${renderRoleBadge(proposal.proposedByRole)}
        </div>
        <p class="pre-wrap" style="margin:0;color:var(--ink-variant);font-size:0.86rem;">
          ${proposal.note ? esc(proposal.note) : "Alternative time slot proposed."}
        </p>
      </div>

      ${isOwnProposal ? `
        <div class="callout callout-info">
          <span class="material-symbols-outlined text-primary" style="font-size:18px;">hourglass_top</span>
          <span>Awaiting counterparty response. You cannot accept your own proposal, though you may propose another time or decline it.</span>
        </div>
        <div class="row-gap">
          <button type="button" class="btn btn-secondary btn-sm" id="callout-counter-btn">Propose Different Slot</button>
          <button type="button" class="btn btn-danger-ghost btn-sm" data-decline-proposal="${proposal.id}">Withdraw Proposal</button>
        </div>
      ` : `
        <div class="row-gap">
          <button type="button" class="btn btn-primary" data-accept-proposal="${proposal.id}">
            <span class="material-symbols-outlined" style="font-size:18px;">check_circle</span>
            Accept Proposed Time
          </button>
          <button type="button" class="btn btn-secondary" id="callout-counter-btn">
            <span class="material-symbols-outlined" style="font-size:18px;">sync_alt</span>
            Counter-Propose Slot
          </button>
          <button type="button" class="btn btn-danger-ghost btn-sm" data-decline-proposal="${proposal.id}">
            Decline
          </button>
        </div>
      `}
    </section>
  `;
}

function renderProposalTimelineNode(proposal, currentUser, canNegotiate) {
  const nodeClass =
    proposal.status === "ACCEPTED" ? "secondary"
      : proposal.status === "DECLINED" ? "rose"
      : "violet";
  const nodeIcon =
    proposal.status === "ACCEPTED" ? "check"
      : proposal.status === "DECLINED" ? "close"
      : "schedule";
  const canRespond = canNegotiate && proposal.status === "PENDING" && proposal.proposedById !== currentUser.id;

  return `
    <div class="timeline-item">
      <div class="timeline-node ${nodeClass}">
        <span class="material-symbols-outlined" style="font-size:16px;">${nodeIcon}</span>
      </div>
      <div class="timeline-card ${proposal.status === "PENDING" ? "highlight" : ""}">
        <div class="row-between">
          <div class="row-gap">
            <span class="fw-700">${esc(proposal.proposedByName || `User #${proposal.proposedById}`)}</span>
            ${renderRoleBadge(proposal.proposedByRole)}
            ${renderProposalBadge(proposal.status)}
          </div>
          <span class="text-xs text-muted numeric">${esc(formatDateTime(proposal.createdAt))}</span>
        </div>
        <div class="numeric fw-600 text-primary" style="font-size:0.86rem;">
          Proposed: ${esc(formatTimeRange(proposal.proposedStart, proposal.proposedEnd))}
        </div>
        ${proposal.note ? `<p class="pre-wrap text-sm" style="margin:0;color:var(--ink-variant);">${esc(proposal.note)}</p>` : ""}
        ${canRespond ? `
          <div class="row-gap" style="margin-top:0.25rem;">
            <button type="button" class="btn btn-primary btn-sm" data-accept-proposal="${proposal.id}">Accept</button>
            <button type="button" class="btn btn-secondary btn-sm" data-decline-proposal="${proposal.id}">Decline</button>
          </div>
        ` : ""}
      </div>
    </div>
  `;
}

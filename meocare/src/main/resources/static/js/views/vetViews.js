import { api } from "../api.js";
import {
  bindLightboxClicks,
  esc,
  formatDateTime,
  formatTimeRange,
  mountMediaPicker,
  renderAppointmentBadge,
  renderHealthBadge,
  safeImageUrl,
  showToast
} from "../utils.js";

/* =========================================================================
   1. VETERINARIAN SCHEDULE & TRIAGE QUEUE
   ========================================================================= */
export async function renderVetScheduleView(container, ctx) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading clinical schedule...</p></div></div>`;

  const user = ctx.getUser();
  let appointments = [], records = [];
  try {
    [appointments, records] = await Promise.all([
      api.getAppointments(),
      api.getMedicalRecords({ veterinarianId: user.id })
    ]);
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  const recordByAppt = new Map(records.map((r) => [r.appointmentId, r]));
  let activeFilter = "ALL";
  let searchQuery = "";

  const myAssigned = appointments.filter((a) => a.veterinarianId === user.id);
  const unassigned = appointments.filter((a) => !a.veterinarianId && !["CANCELLED", "REJECTED", "COMPLETED"].includes(a.status));
  const confirmedCount = myAssigned.filter((a) => a.status === "CONFIRMED").length;
  const negotiatingCount = appointments.filter((a) => a.status === "RESCHEDULE_PROPOSED").length;
  const completedCount = myAssigned.filter((a) => a.status === "COMPLETED").length;

  container.innerHTML = `
    <div class="page-header">
      <div>
        <span class="eyebrow">Veterinarian Clinical Suite</span>
        <h1>Appointments &amp; Triage Schedule</h1>
        <p>Manage your assigned consultations, claim unassigned triage requests, negotiate slots, and launch clinical examinations.</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" id="vet-open-records">
          <span class="material-symbols-outlined" style="font-size:18px;">clinical_notes</span>
          Medical Records &amp; Updates
        </button>
      </div>
    </div>

    <section class="kpi-grid">
      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">My Confirmed Visits</span>
          <span class="kpi-icon sage"><span class="material-symbols-outlined">event_available</span></span>
        </div>
        <div class="kpi-value numeric">${confirmedCount}</div>
        <div class="kpi-meta">Ready for clinical examination</div>
      </article>

      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">Unassigned Triage</span>
          <span class="kpi-icon ${unassigned.length ? "alert" : ""}"><span class="material-symbols-outlined">assignment_late</span></span>
        </div>
        <div class="kpi-value numeric">${unassigned.length}</div>
        <div class="kpi-meta">Open requests awaiting a clinician</div>
      </article>

      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">Active Negotiations</span>
          <span class="kpi-icon"><span class="material-symbols-outlined">event_repeat</span></span>
        </div>
        <div class="kpi-value numeric">${negotiatingCount}</div>
        <div class="kpi-meta">Reschedule proposals in progress</div>
      </article>

      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">Completed Exams</span>
          <span class="kpi-icon"><span class="material-symbols-outlined">task_alt</span></span>
        </div>
        <div class="kpi-value numeric">${completedCount}</div>
        <div class="kpi-meta">${records.length} medical record${records.length === 1 ? "" : "s"} authored</div>
      </article>
    </section>

    <section class="card stack-md">
      <div class="row-between">
        <div class="filter-bar" id="vet-filter-tabs">
          <button type="button" class="pill-tab active" data-vf="ALL">All <span class="count">${appointments.length}</span></button>
          <button type="button" class="pill-tab" data-vf="MINE">My Assigned <span class="count">${myAssigned.length}</span></button>
          <button type="button" class="pill-tab" data-vf="UNASSIGNED">Unassigned Triage <span class="count">${unassigned.length}</span></button>
          <button type="button" class="pill-tab" data-vf="PENDING">Pending</button>
          <button type="button" class="pill-tab" data-vf="RESCHEDULE_PROPOSED">Negotiating</button>
          <button type="button" class="pill-tab" data-vf="CONFIRMED">Confirmed</button>
          <button type="button" class="pill-tab" data-vf="COMPLETED">Completed</button>
        </div>

        <div class="input-with-icon" style="width:240px;">
          <span class="material-symbols-outlined leading-icon">search</span>
          <input id="vet-appt-search" class="input" placeholder="Search pet, owner, symptoms...">
        </div>
      </div>

      <div id="vet-appt-table-slot"></div>
    </section>
  `;

  function renderTable() {
    const slot = document.getElementById("vet-appt-table-slot");
    const q = searchQuery.trim().toLowerCase();
    const list = appointments.filter((a) => {
      if (activeFilter === "MINE" && a.veterinarianId !== user.id) return false;
      if (activeFilter === "UNASSIGNED" && (a.veterinarianId || ["CANCELLED", "REJECTED", "COMPLETED"].includes(a.status))) return false;
      if (["PENDING", "RESCHEDULE_PROPOSED", "CONFIRMED", "COMPLETED"].includes(activeFilter) && a.status !== activeFilter) return false;
      if (q && !`apt-${a.id} ${a.petName || ""} ${a.customerName || ""} ${a.symptoms || ""}`.toLowerCase().includes(q)) return false;
      return true;
    });

    if (!list.length) {
      slot.innerHTML = `<div class="empty-state"><span class="material-symbols-outlined">calendar_today</span><h4>No appointments match this filter</h4></div>`;
      return;
    }

    slot.innerHTML = `
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Patient &amp; Owner</th>
              <th>Schedule Slot</th>
              <th>Assignment</th>
              <th>Status</th>
              <th style="text-align:right;">Clinical Actions</th>
            </tr>
          </thead>
          <tbody>
            ${list.map((a) => {
              const hasRecord = recordByAppt.has(a.id);
              const canConfirm = ["PENDING", "RESCHEDULE_PROPOSED"].includes(a.status);
              const canComplete = a.status === "CONFIRMED";
              const canExamine = ["CONFIRMED", "COMPLETED"].includes(a.status);
              return `
                <tr>
                  <td class="numeric fw-700 text-primary">APT-${a.id}</td>
                  <td>
                    <div class="fw-700">${esc(a.petName || `Pet #${a.petId}`)}</div>
                    <div class="text-xs text-muted">${esc(a.customerName || `Customer #${a.customerId}`)}</div>
                    ${a.symptoms ? `<div class="text-xs text-muted" style="max-width:240px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(a.symptoms)}</div>` : ""}
                  </td>
                  <td class="numeric text-sm">
                    <div class="fw-600">${esc(formatTimeRange(a.confirmedStart || a.requestedStart, a.confirmedEnd || a.requestedEnd))}</div>
                    <div class="text-xs text-muted">${a.confirmedStart ? "Confirmed slot" : "Requested slot"}</div>
                  </td>
                  <td>
                    ${a.veterinarianName
                      ? `<span class="text-sm fw-600">${esc(a.veterinarianName)}</span>`
                      : `<span class="badge badge-pending">Unassigned</span>`}
                  </td>
                  <td>${renderAppointmentBadge(a.status)}</td>
                  <td style="text-align:right;">
                    <div class="row-gap" style="justify-content:flex-end;">
                      ${!a.veterinarianId && !["CANCELLED", "REJECTED", "COMPLETED"].includes(a.status) ? `
                        <button class="btn btn-tonal btn-sm" data-vet-claim="${a.id}">Claim</button>
                      ` : ""}
                      ${canConfirm ? `
                        <button class="btn btn-primary btn-sm" data-vet-confirm="${a.id}">Confirm</button>
                      ` : ""}
                      ${canComplete ? `
                        <button class="btn btn-primary btn-sm" data-vet-complete="${a.id}">Complete</button>
                      ` : ""}
                      ${canExamine ? `
                        <button class="btn btn-secondary btn-sm" data-vet-exam="${a.id}">
                          <span class="material-symbols-outlined" style="font-size:15px;">clinical_notes</span>
                          ${hasRecord ? "Record" : "Examine"}
                        </button>
                      ` : ""}
                      <button class="btn btn-ghost btn-sm" data-vet-detail="${a.id}">Negotiate / Details</button>
                    </div>
                  </td>
                </tr>
              `;
            }).join("")}
          </tbody>
        </table>
      </div>
    `;

    const reload = () => renderVetScheduleView(container, ctx);

    slot.querySelectorAll("[data-vet-claim]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        try {
          await api.assignVeterinarian(Number(btn.getAttribute("data-vet-claim")), user.id);
          showToast("Appointment assigned to you.", "success");
          await reload();
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });

    slot.querySelectorAll("[data-vet-confirm]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        try {
          await api.confirmAppointment(Number(btn.getAttribute("data-vet-confirm")));
          showToast("Appointment confirmed.", "success");
          await reload();
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });

    slot.querySelectorAll("[data-vet-complete]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        try {
          await api.updateAppointmentStatus(Number(btn.getAttribute("data-vet-complete")), "COMPLETED");
          showToast("Appointment marked as completed.", "success");
          await reload();
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });

    slot.querySelectorAll("[data-vet-exam]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const apptId = Number(btn.getAttribute("data-vet-exam"));
        ctx.navigate("records", { appointmentId: apptId, recordId: recordByAppt.get(apptId)?.id || null });
      });
    });

    slot.querySelectorAll("[data-vet-detail]").forEach((btn) => {
      btn.addEventListener("click", () => {
        ctx.navigate("appointment-detail", { id: Number(btn.getAttribute("data-vet-detail")) });
      });
    });
  }

  document.getElementById("vet-open-records")?.addEventListener("click", () => ctx.navigate("records"));
  document.getElementById("vet-filter-tabs")?.querySelectorAll("[data-vf]").forEach((btn) => {
    btn.addEventListener("click", () => {
      activeFilter = btn.getAttribute("data-vf");
      document.getElementById("vet-filter-tabs").querySelectorAll("[data-vf]").forEach((b) => b.classList.toggle("active", b === btn));
      renderTable();
    });
  });
  document.getElementById("vet-appt-search")?.addEventListener("input", (e) => {
    searchQuery = e.target.value || "";
    renderTable();
  });

  renderTable();
}

/* =========================================================================
   2. CLINICAL EXAMINATION, MEDICAL RECORDS & DAILY HEALTH TIMELINE PUBLISHER
      (Shared by VETERINARIAN & ADMIN)
   ========================================================================= */
export async function renderClinicalRecordsWorkspace(container, ctx, params = {}) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading clinical records workspace...</p></div></div>`;

  let records = [], appointments = [], pets = [];
  try {
    [records, appointments, pets] = await Promise.all([
      api.getMedicalRecords(),
      api.getAppointments(),
      api.getPets()
    ]);
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  const recordByAppointmentId = new Map(records.map((r) => [r.appointmentId, r]));
  const eligibleNewAppts = appointments.filter(
    (a) => ["CONFIRMED", "COMPLETED"].includes(a.status) && !recordByAppointmentId.has(a.id)
  );

  // Determine initial mode: existing record OR creating for a specific appointment
  let mode = "existing";
  let activeRecordId = null;
  let createForApptId = null;

  if (params.recordId && records.some((r) => r.id === Number(params.recordId))) {
    activeRecordId = Number(params.recordId);
  } else if (params.appointmentId) {
    const existing = recordByAppointmentId.get(Number(params.appointmentId));
    if (existing) {
      activeRecordId = existing.id;
    } else if (eligibleNewAppts.some((a) => a.id === Number(params.appointmentId))) {
      mode = "create";
      createForApptId = Number(params.appointmentId);
    }
  } else if (records.length) {
    activeRecordId = records[0].id;
  } else if (eligibleNewAppts.length) {
    mode = "create";
    createForApptId = eligibleNewAppts[0].id;
  }

  let petFilter = "";

  container.innerHTML = `
    <div class="page-header">
      <div>
        <span class="eyebrow">Clinical Examination &amp; EMR</span>
        <h1>Medical Records &amp; Daily Health Timeline</h1>
        <p>Document physiological vitals, examination findings, diagnoses, treatments, diagnostic media, and publish daily patient recovery updates.</p>
      </div>
      <div class="header-actions">
        <select id="clin-pet-filter" class="select" style="width:auto;min-width:170px;">
          <option value="">All Clinic Pets</option>
          ${pets.map((p) => `<option value="${p.id}">${esc(p.name)} (${esc(p.species)})</option>`).join("")}
        </select>
        ${eligibleNewAppts.length ? `
          <button class="btn btn-primary" id="clin-new-rec-btn">
            <span class="material-symbols-outlined" style="font-size:18px;">note_add</span>
            New Medical Record (${eligibleNewAppts.length} Ready)
          </button>
        ` : ""}
      </div>
    </div>

    <div class="split-8-4">
      <div id="clin-editor-slot" class="stack-lg"></div>
      <aside class="stack-md">
        <section class="card stack-sm">
          <div class="row-between">
            <h2 class="card-title">Clinic Records</h2>
            <span class="badge badge-neutral badge-no-dot">${records.length}</span>
          </div>
          <div id="clin-records-sidebar" class="stack-sm" style="max-height:420px;overflow-y:auto;"></div>
        </section>

        ${eligibleNewAppts.length ? `
          <section class="card stack-sm">
            <div class="row-between">
              <h2 class="card-title">Ready for Exam</h2>
              <span class="badge badge-confirmed badge-no-dot">${eligibleNewAppts.length}</span>
            </div>
            <p class="text-xs text-muted" style="margin:0;">Confirmed or completed visits without a medical record yet.</p>
            <div class="stack-sm">
              ${eligibleNewAppts.map((a) => `
                <button type="button" class="card-soft row-between" style="text-align:left;cursor:pointer;border:1px solid var(--border);" data-create-for-appt="${a.id}">
                  <div>
                    <div class="fw-700 text-sm">${esc(a.petName)} · APT-${a.id}</div>
                    <div class="text-xs text-muted">${esc(a.customerName)} · ${esc(a.veterinarianName || "Unassigned")}</div>
                  </div>
                  <span class="material-symbols-outlined text-primary">add_circle</span>
                </button>
              `).join("")}
            </div>
          </section>
        ` : ""}
      </aside>
    </div>
  `;

  function renderSidebar() {
    const slot = document.getElementById("clin-records-sidebar");
    const filtered = petFilter ? records.filter((r) => String(r.petId) === petFilter) : records;
    if (!filtered.length) {
      slot.innerHTML = `<p class="text-sm text-muted">No medical records yet.</p>`;
      return;
    }
    slot.innerHTML = filtered.map((r) => {
      const selected = mode === "existing" && r.id === activeRecordId;
      return `
        <button type="button" class="card-soft stack-sm" data-select-rec="${r.id}"
                style="text-align:left;cursor:pointer;border:1.5px solid ${selected ? "var(--primary)" : "var(--surface-high)"};background:${selected ? "rgba(0,104,95,0.06)" : "var(--surface-low)"};">
          <div class="row-between">
            <span class="fw-700 text-sm">${esc(r.petName || `Pet #${r.petId}`)}</span>
            <span class="text-xs text-muted numeric">MED-${r.id}</span>
          </div>
          <div class="text-xs" style="color:var(--ink-variant);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
            ${esc(r.diagnosis || "Examination record")}
          </div>
          <div class="text-xs text-muted numeric">${esc(r.veterinarianName)} · ${esc(formatDateTime(r.createdAt))}</div>
        </button>
      `;
    }).join("");

    slot.querySelectorAll("[data-select-rec]").forEach((btn) => {
      btn.addEventListener("click", () => {
        mode = "existing";
        activeRecordId = Number(btn.getAttribute("data-select-rec"));
        renderSidebar();
        renderEditor();
      });
    });
  }

  async function renderEditor() {
    const slot = document.getElementById("clin-editor-slot");

    if (mode === "create") {
      const targetAppt = eligibleNewAppts.find((a) => a.id === createForApptId) || eligibleNewAppts[0];
      if (!targetAppt) {
        slot.innerHTML = `
          <div class="card">
            <div class="empty-state">
              <span class="material-symbols-outlined">clinical_notes</span>
              <h4>No confirmed appointments awaiting a record</h4>
              <p>Confirm an appointment first to document a clinical examination.</p>
            </div>
          </div>
        `;
        return;
      }

      slot.innerHTML = `
        <section class="card stack-md">
          <div class="row-between">
            <div>
              <span class="eyebrow">New Clinical Examination</span>
              <h2 class="card-title" style="font-size:1.2rem;margin-top:3px;">
                Create Medical Record — ${esc(targetAppt.petName)} (APT-${targetAppt.id})
              </h2>
              <p class="card-subtitle">
                Owner: ${esc(targetAppt.customerName)} · Vet: ${esc(targetAppt.veterinarianName || "Will be assigned to you")}
              </p>
            </div>
            ${renderAppointmentBadge(targetAppt.status)}
          </div>

          <form id="create-record-form" class="stack-md">
            <div class="field">
              <label for="new-rec-appt">Appointment <span class="req">*</span></label>
              <select id="new-rec-appt" name="appointmentId" class="select" required>
                ${eligibleNewAppts.map((a) => `
                  <option value="${a.id}" ${a.id === targetAppt.id ? "selected" : ""}>
                    APT-${a.id} — ${esc(a.petName)} (${esc(a.customerName)}) [${a.status}]
                  </option>
                `).join("")}
              </select>
            </div>

            ${renderRecordFormFields(null)}
            <div id="create-record-media"></div>

            <div class="row-between" style="padding-top:0.5rem;border-top:1px solid var(--border);">
              <span class="text-xs text-muted">Record will be certified under the examining veterinarian.</span>
              <button type="submit" class="btn btn-primary" id="save-new-record-btn">
                <span class="material-symbols-outlined" style="font-size:18px;">draw</span>
                Save Medical Record
              </button>
            </div>
          </form>
        </section>
      `;

      const mediaPicker = mountMediaPicker(document.getElementById("create-record-media"), {
        label: "Clinical Diagnostic Media & Attachments"
      });

      document.getElementById("create-record-form")?.addEventListener("submit", async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const btn = document.getElementById("save-new-record-btn");
        btn.disabled = true;
        try {
          const created = await api.createMedicalRecord({
            appointmentId: Number(fd.get("appointmentId")),
            weight: parseOptionalNum(fd.get("weight")),
            temperature: parseOptionalNum(fd.get("temperature")),
            examination: trimOrNull(fd.get("examination")),
            diagnosis: trimOrNull(fd.get("diagnosis")),
            treatment: trimOrNull(fd.get("treatment")),
            advice: trimOrNull(fd.get("advice")),
            notes: trimOrNull(fd.get("notes")),
            imageUrls: mediaPicker.getUrls()
          });
          showToast(`Medical record MED-${created.id} saved!`, "success");
          await renderClinicalRecordsWorkspace(container, ctx, { recordId: created.id });
        } catch (err) {
          showToast(err.message, "error");
          btn.disabled = false;
        }
      });
      return;
    }

    const record = records.find((r) => r.id === activeRecordId);
    if (!record) {
      slot.innerHTML = `
        <div class="card">
          <div class="empty-state">
            <span class="material-symbols-outlined">clinical_notes</span>
            <h4>Select a medical record</h4>
            <p>Choose a record from the right sidebar or create a new one from a confirmed appointment.</p>
          </div>
        </div>
      `;
      return;
    }

    let healthUpdates = [];
    try {
      healthUpdates = await api.getHealthUpdatesByRecord(record.id);
    } catch {
      healthUpdates = [];
    }

    slot.innerHTML = `
      <!-- Medical Record Editor -->
      <section class="card stack-md">
        <div class="row-between">
          <div>
            <div class="row-gap">
              <span class="eyebrow">Active Record #MED-${record.id}</span>
              <span class="badge badge-completed badge-no-dot">APT-${record.appointmentId}</span>
            </div>
            <h2 class="card-title" style="font-size:1.25rem;margin-top:3px;">
              ${esc(record.petName || `Pet #${record.petId}`)} — Clinical Examination
            </h2>
            <p class="card-subtitle numeric">
              Clinician: <strong>${esc(record.veterinarianName || "Veterinarian")}</strong> · Recorded ${esc(formatDateTime(record.createdAt))}
            </p>
          </div>
          <button type="button" class="btn btn-secondary btn-sm" id="rec-open-appt">View Appointment</button>
        </div>

        <form id="update-record-form" class="stack-md">
          ${renderRecordFormFields(record)}
          <div class="row-between">
            <span class="text-xs text-muted numeric">Last updated ${esc(formatDateTime(record.updatedAt || record.createdAt))}</span>
            <button type="submit" class="btn btn-primary" id="update-record-btn">
              <span class="material-symbols-outlined" style="font-size:18px;">save</span>
              Update Medical Record
            </button>
          </div>
        </form>

        <!-- Diagnostic Media Gallery -->
        <div class="stack-sm" style="padding-top:0.85rem;border-top:1px solid var(--border);">
          <span class="eyebrow" style="color:var(--outline);">Diagnostic Media (${record.images?.length || 0})</span>
          ${record.images?.length ? `
            <div class="media-grid">
              ${record.images.map((img) => `
                <div class="media-thumb" data-lightbox-url="${esc(img.imageUrl)}">
                  <img src="${esc(safeImageUrl(img.imageUrl))}" alt="Diagnostic attachment">
                  <button type="button" class="media-remove-btn" data-del-rec-img="${img.id}" title="Delete attachment">
                    <span class="material-symbols-outlined" style="font-size:15px;">delete</span>
                  </button>
                </div>
              `).join("")}
            </div>
          ` : `<p class="text-xs text-muted" style="margin:0;">No diagnostic images attached yet.</p>`}
          <div id="existing-record-media-picker"></div>
          <div>
            <button type="button" class="btn btn-secondary btn-sm" id="attach-rec-images-btn">
              <span class="material-symbols-outlined" style="font-size:16px;">add_photo_alternate</span>
              Attach Selected Photos to Record
            </button>
          </div>
        </div>
      </section>

      <!-- Daily Health Timeline Publisher & Feed -->
      <section class="card stack-md">
        <div class="row-between">
          <div>
            <span class="eyebrow text-secondary">Daily Health Timeline Publisher</span>
            <h2 class="card-title">Post Progress Update for ${esc(record.petName)}</h2>
            <p class="card-subtitle">Published updates appear immediately in the pet parent's read-only Daily Health Timeline.</p>
          </div>
          <span class="badge badge-confirmed badge-no-dot">${healthUpdates.length} Update${healthUpdates.length === 1 ? "" : "s"}</span>
        </div>

        <form id="publish-health-form" class="card-soft stack-md">
          <div class="field">
            <label for="hu-status">Health Status <span class="req">*</span></label>
            <input id="hu-status" name="healthStatus" class="input" required maxlength="30" placeholder="e.g., STABLE, IMPROVING, OBSERVATION" value="STABLE">
            <div class="row-gap" style="margin-top:4px;">
              ${["STABLE", "IMPROVING", "RECOVERING", "OBSERVATION", "CRITICAL"].map((st) => `
                <button type="button" class="btn btn-tonal btn-sm" data-hu-preset="${st}">${st}</button>
              `).join("")}
            </div>
          </div>

          <div class="field">
            <label for="hu-desc">Clinical Observation &amp; Care Update <span class="req">*</span></label>
            <textarea id="hu-desc" name="description" class="textarea" required rows="3"
                      placeholder="Describe appetite, energy, wound healing, temperature stability, or care progress..."></textarea>
          </div>

          <div id="hu-media-picker"></div>

          <div class="row-between">
            <span class="text-xs text-muted">Attributed to your authenticated clinician account.</span>
            <button type="submit" class="btn btn-primary" id="hu-submit-btn">
              <span class="material-symbols-outlined" style="font-size:18px;">publish</span>
              Publish Health Update
            </button>
          </div>
        </form>

        <div class="stack-sm">
          <span class="eyebrow" style="color:var(--outline);">Posted Timeline Entries (${healthUpdates.length})</span>
          ${healthUpdates.length ? `
            <div class="timeline">
              ${healthUpdates.map((u) => `
                <div class="timeline-item">
                  <div class="timeline-node secondary">
                    <span class="material-symbols-outlined" style="font-size:16px;">vital_signs</span>
                  </div>
                  <div class="timeline-card">
                    <div class="row-between">
                      <div class="row-gap">
                        ${renderHealthBadge(u.healthStatus)}
                        <span class="text-xs text-muted">by <strong>${esc(u.veterinarianName || "Veterinarian")}</strong></span>
                      </div>
                      <span class="text-xs text-muted numeric">${esc(formatDateTime(u.createdAt))}</span>
                    </div>
                    <p class="text-sm pre-wrap" style="margin:0;">${esc(u.description || "")}</p>
                    ${u.images?.length ? `
                      <div class="media-grid" style="margin-top:4px;">
                        ${u.images.map((img) => `
                          <div class="media-thumb" data-lightbox-url="${esc(img.imageUrl)}">
                            <img src="${esc(safeImageUrl(img.imageUrl))}" alt="Progress photo">
                            <button type="button" class="media-remove-btn" data-del-hu-img="${u.id}:${img.id}" title="Delete photo">
                              <span class="material-symbols-outlined" style="font-size:15px;">delete</span>
                            </button>
                          </div>
                        `).join("")}
                      </div>
                    ` : ""}
                  </div>
                </div>
              `).join("")}
            </div>
          ` : `<p class="text-sm text-muted">No daily health updates posted for this record yet.</p>`}
        </div>
      </section>
    `;

    bindLightboxClicks(slot);

    const recMediaPicker = mountMediaPicker(document.getElementById("existing-record-media-picker"), {
      label: "Upload or Paste Additional Diagnostic Photos"
    });
    const huMediaPicker = mountMediaPicker(document.getElementById("hu-media-picker"), {
      label: "Progress Photos (Optional)"
    });

    document.getElementById("rec-open-appt")?.addEventListener("click", () => {
      ctx.navigate("appointment-detail", { id: record.appointmentId });
    });

    document.getElementById("update-record-form")?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      const btn = document.getElementById("update-record-btn");
      btn.disabled = true;
      try {
        const updated = await api.updateMedicalRecord(record.id, {
          weight: parseOptionalNum(fd.get("weight")),
          temperature: parseOptionalNum(fd.get("temperature")),
          examination: trimOrNull(fd.get("examination")),
          diagnosis: trimOrNull(fd.get("diagnosis")),
          treatment: trimOrNull(fd.get("treatment")),
          advice: trimOrNull(fd.get("advice")),
          notes: trimOrNull(fd.get("notes"))
        });
        const idx = records.findIndex((r) => r.id === updated.id);
        if (idx >= 0) records[idx] = updated;
        showToast("Medical record updated.", "success");
        renderSidebar();
        await renderEditor();
      } catch (err) {
        showToast(err.message, "error");
        btn.disabled = false;
      }
    });

    document.getElementById("attach-rec-images-btn")?.addEventListener("click", async () => {
      const urls = recMediaPicker.getUrls();
      if (!urls.length) {
        showToast("Upload or add at least one image URL first.", "error");
        return;
      }
      try {
        await api.addMedicalRecordImages(record.id, urls);
        showToast("Diagnostic photos attached.", "success");
        await renderClinicalRecordsWorkspace(container, ctx, { recordId: record.id });
      } catch (err) {
        showToast(err.message, "error");
      }
    });

    slot.querySelectorAll("[data-del-rec-img]").forEach((btn) => {
      btn.addEventListener("click", async (e) => {
        e.stopPropagation();
        const imgId = Number(btn.getAttribute("data-del-rec-img"));
        try {
          await api.deleteMedicalRecordImage(record.id, imgId);
          showToast("Diagnostic photo removed.", "info");
          await renderClinicalRecordsWorkspace(container, ctx, { recordId: record.id });
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });

    slot.querySelectorAll("[data-hu-preset]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const input = document.getElementById("hu-status");
        if (input) input.value = btn.getAttribute("data-hu-preset");
      });
    });

    document.getElementById("publish-health-form")?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      const btn = document.getElementById("hu-submit-btn");
      btn.disabled = true;
      try {
        await api.createHealthUpdate(record.id, {
          healthStatus: String(fd.get("healthStatus") || "").trim(),
          description: String(fd.get("description") || "").trim(),
          imageUrls: huMediaPicker.getUrls()
        });
        showToast("Daily health update published to timeline!", "success");
        await renderEditor();
      } catch (err) {
        showToast(err.message, "error");
        btn.disabled = false;
      }
    });

    slot.querySelectorAll("[data-del-hu-img]").forEach((btn) => {
      btn.addEventListener("click", async (e) => {
        e.stopPropagation();
        const [huId, imgId] = btn.getAttribute("data-del-hu-img").split(":").map(Number);
        try {
          await api.deleteHealthUpdateImage(huId, imgId);
          showToast("Progress photo removed.", "info");
          await renderEditor();
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });
  }

  document.getElementById("clin-pet-filter")?.addEventListener("change", (e) => {
    petFilter = e.target.value;
    renderSidebar();
  });

  document.getElementById("clin-new-rec-btn")?.addEventListener("click", () => {
    mode = "create";
    createForApptId = eligibleNewAppts[0]?.id || null;
    renderSidebar();
    renderEditor();
  });

  container.querySelectorAll("[data-create-for-appt]").forEach((btn) => {
    btn.addEventListener("click", () => {
      mode = "create";
      createForApptId = Number(btn.getAttribute("data-create-for-appt"));
      renderSidebar();
      renderEditor();
    });
  });

  renderSidebar();
  await renderEditor();
}

function renderRecordFormFields(record) {
  return `
    <div class="vitals-grid">
      <div class="vital-card">
        <label for="rec-weight" class="kpi-label">Weight (kg)</label>
        <input id="rec-weight" name="weight" type="number" step="0.01" min="0" max="999.99" class="input numeric"
               placeholder="e.g., 31.40" value="${record?.weight != null ? esc(record.weight) : ""}">
      </div>
      <div class="vital-card">
        <label for="rec-temp" class="kpi-label">Body Temperature (°C)</label>
        <input id="rec-temp" name="temperature" type="number" step="0.1" min="30.0" max="45.0" class="input numeric"
               placeholder="30.0 – 45.0" value="${record?.temperature != null ? esc(record.temperature) : ""}">
      </div>
    </div>

    <div class="form-grid">
      <div class="field col-span-full">
        <label for="rec-exam">Primary Examination Findings</label>
        <textarea id="rec-exam" name="examination" class="textarea" rows="3"
                  placeholder="Alert, auscultation clear, coat and ear inspection...">${esc(record?.examination || "")}</textarea>
      </div>
      <div class="field">
        <label for="rec-diag">Diagnosis &amp; Clinical Impression</label>
        <textarea id="rec-diag" name="diagnosis" class="textarea" rows="3"
                  placeholder="Primary clinical diagnosis...">${esc(record?.diagnosis || "")}</textarea>
      </div>
      <div class="field">
        <label for="rec-treat">Treatment &amp; Medical Plan</label>
        <textarea id="rec-treat" name="treatment" class="textarea" rows="3"
                  placeholder="Procedures performed and care regimen...">${esc(record?.treatment || "")}</textarea>
      </div>
      <div class="field">
        <label for="rec-advice">Caregiver Advice</label>
        <textarea id="rec-advice" name="advice" class="textarea" rows="2"
                  placeholder="Home care instructions and follow-up guidance...">${esc(record?.advice || "")}</textarea>
      </div>
      <div class="field">
        <label for="rec-notes">Additional Clinical Notes</label>
        <textarea id="rec-notes" name="notes" class="textarea" rows="2"
                  placeholder="Internal observations or follow-up reminders...">${esc(record?.notes || "")}</textarea>
      </div>
    </div>
  `;
}

function parseOptionalNum(raw) {
  const s = String(raw ?? "").trim();
  return s === "" ? null : Number(s);
}

function trimOrNull(raw) {
  const s = String(raw ?? "").trim();
  return s === "" ? null : s;
}

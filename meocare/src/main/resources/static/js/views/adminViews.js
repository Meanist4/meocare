import { api } from "../api.js";
import {
  closeModal,
  esc,
  formatDate,
  formatTimeRange,
  getInitials,
  openModal,
  renderAppointmentBadge,
  renderRoleBadge,
  renderUserStatusBadge,
  safeImageUrl,
  showToast
} from "../utils.js";

/* =========================================================================
   1. ADMIN CLINIC OPERATIONS & STAFF DISPATCH DASHBOARD
   ========================================================================= */
export async function renderAdminDashboardView(container, ctx) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading Clinic Operations Dashboard...</p></div></div>`;

  let appointments = [], pets = [], vets = [], users = [];
  try {
    [appointments, pets, vets, users] = await Promise.all([
      api.getAppointments(),
      api.getPets(),
      api.getActiveVeterinarians(),
      api.getUsers()
    ]);
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  let statusFilter = "ALL";
  let vetFilter = "";
  let searchQuery = "";

  const pendingCount = appointments.filter((a) => a.status === "PENDING").length;
  const negotiatingCount = appointments.filter((a) => a.status === "RESCHEDULE_PROPOSED").length;
  const confirmedCount = appointments.filter((a) => a.status === "CONFIRMED").length;
  const completedCount = appointments.filter((a) => a.status === "COMPLETED").length;
  const unassignedCount = appointments.filter((a) => !a.veterinarianId && ["PENDING", "RESCHEDULE_PROPOSED"].includes(a.status)).length;
  const urgentCount = unassignedCount + negotiatingCount;
  const customersCount = users.filter((u) => u.role === "CUSTOMER").length;

  container.innerHTML = `
    <div class="page-header">
      <div>
        <span class="eyebrow">Operations Dispatch • Single Clinic Control</span>
        <h1>Clinic Operations &amp; Staff Dispatch</h1>
        <p>Coordinate appointment assignments, balance veterinarian caseloads, arbitrate schedule negotiations, and oversee clinic staff.</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-secondary" id="admin-manage-staff-btn">
          <span class="material-symbols-outlined" style="font-size:18px;">group</span>
          Staff &amp; Users (${users.length})
        </button>
        <button class="btn btn-primary" id="admin-add-vet-btn">
          <span class="material-symbols-outlined" style="font-size:18px;">person_add</span>
          Add Staff Account
        </button>
      </div>
    </div>

    <!-- KPI Ribbon -->
    <section class="kpi-grid">
      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">Registered Patients</span>
          <span class="kpi-icon"><span class="material-symbols-outlined">pets</span></span>
        </div>
        <div class="kpi-value numeric">${pets.length}</div>
        <div class="kpi-meta">Across ${customersCount} pet parent account${customersCount === 1 ? "" : "s"}</div>
      </article>

      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">Clinic Appointments</span>
          <span class="kpi-icon sage"><span class="material-symbols-outlined">calendar_month</span></span>
        </div>
        <div class="kpi-value numeric">${appointments.length}</div>
        <div class="kpi-meta numeric">${confirmedCount} confirmed · ${pendingCount} pending · ${completedCount} done</div>
      </article>

      <article class="kpi-card" style="${urgentCount ? "border-left:4px solid var(--error);" : ""}">
        <div class="kpi-top">
          <span class="kpi-label" style="${urgentCount ? "color:var(--error);" : ""}">Dispatch Attention</span>
          <span class="kpi-icon ${urgentCount ? "alert" : ""}"><span class="material-symbols-outlined">priority_high</span></span>
        </div>
        <div class="kpi-value numeric">${urgentCount}</div>
        <div class="kpi-meta">${unassignedCount} unassigned · ${negotiatingCount} open negotiation${negotiatingCount === 1 ? "" : "s"}</div>
      </article>

      <article class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">Active Veterinarians</span>
          <span class="kpi-icon sage"><span class="material-symbols-outlined">stethoscope</span></span>
        </div>
        <div class="kpi-value numeric">${vets.length}</div>
        <div class="kpi-meta">Available for clinical assignment</div>
      </article>
    </section>

    <div class="split-8-4">
      <!-- Left 8 cols: Live Appointment Dispatch Table -->
      <section class="card stack-md">
        <div class="row-between">
          <div>
            <h2 class="card-title">Live Appointment Dispatch</h2>
            <p class="card-subtitle">Assign veterinarians, confirm or reject requests, and inspect negotiation trails.</p>
          </div>
          <div class="filter-bar" id="admin-appt-tabs">
            <button type="button" class="pill-tab active" data-af="ALL">All <span class="count">${appointments.length}</span></button>
            <button type="button" class="pill-tab" data-af="UNASSIGNED">Unassigned <span class="count">${unassignedCount}</span></button>
            <button type="button" class="pill-tab" data-af="PENDING">Pending <span class="count">${pendingCount}</span></button>
            <button type="button" class="pill-tab" data-af="RESCHEDULE_PROPOSED">Negotiating <span class="count">${negotiatingCount}</span></button>
            <button type="button" class="pill-tab" data-af="CONFIRMED">Confirmed <span class="count">${confirmedCount}</span></button>
            <button type="button" class="pill-tab" data-af="COMPLETED">Completed <span class="count">${completedCount}</span></button>
          </div>
        </div>

        <div class="row-gap">
          <div class="input-with-icon" style="flex:1;min-width:200px;">
            <span class="material-symbols-outlined leading-icon">search</span>
            <input id="admin-appt-search" class="input" placeholder="Filter by Pet, Owner, ID, or Veterinarian...">
          </div>
          <select id="admin-vet-filter" class="select" style="width:auto;min-width:190px;">
            <option value="">All Veterinarians</option>
            ${vets.map((v) => `<option value="${v.id}">${esc(v.fullName)}</option>`).join("")}
          </select>
        </div>

        <div id="admin-dispatch-table"></div>
      </section>

      <!-- Right 4 cols: Veterinarian Caseload -->
      <aside class="stack-md">
        <section class="card stack-md">
          <div class="row-between">
            <div>
              <h2 class="card-title">Veterinarian Caseload</h2>
              <p class="card-subtitle">Active open &amp; confirmed workload</p>
            </div>
            <span class="badge badge-confirmed badge-no-dot">${vets.length} Active</span>
          </div>

          <div class="stack-sm">
            ${vets.length ? vets.map((v) => {
              const activeLoad = appointments.filter(
                (a) => a.veterinarianId === v.id && ["PENDING", "RESCHEDULE_PROPOSED", "CONFIRMED"].includes(a.status)
              ).length;
              const pct = Math.min(100, Math.round((activeLoad / 8) * 100));
              const fillCls = pct >= 80 ? "high" : pct <= 40 ? "low" : "";
              const avatarUrl = safeImageUrl(v.avatarUrl);
              return `
                <div class="card-soft stack-sm">
                  <div class="row-between">
                    <div class="row-gap">
                      <div class="avatar">
                        ${avatarUrl ? `<img src="${esc(avatarUrl)}" alt="${esc(v.fullName)}">` : esc(getInitials(v.fullName))}
                      </div>
                      <div>
                        <div class="fw-700 text-sm">${esc(v.fullName)}</div>
                        <div class="text-xs text-muted">${esc(v.email)}</div>
                      </div>
                    </div>
                    <span class="numeric fw-700 text-sm">${activeLoad} active</span>
                  </div>
                  <div class="progress-bar"><div class="progress-fill ${fillCls}" style="width:${Math.max(pct, activeLoad ? 12 : 0)}%;"></div></div>
                </div>
              `;
            }).join("") : `
              <div class="callout callout-warn">
                <span class="material-symbols-outlined">warning</span>
                <span>No active veterinarians yet. Provision a veterinarian account below.</span>
              </div>
            `}
          </div>

          <button type="button" class="btn btn-tonal btn-block" id="sidebar-add-vet-btn">
            <span class="material-symbols-outlined" style="font-size:18px;">person_add</span>
            Provision New Veterinarian
          </button>
        </section>
      </aside>
    </div>
  `;

  function renderDispatchTable() {
    const slot = document.getElementById("admin-dispatch-table");
    const q = searchQuery.trim().toLowerCase();
    const list = appointments.filter((a) => {
      if (statusFilter === "UNASSIGNED" && (a.veterinarianId || !["PENDING", "RESCHEDULE_PROPOSED"].includes(a.status))) return false;
      if (["PENDING", "RESCHEDULE_PROPOSED", "CONFIRMED", "COMPLETED"].includes(statusFilter) && a.status !== statusFilter) return false;
      if (vetFilter && String(a.veterinarianId) !== vetFilter) return false;
      if (q && !`apt-${a.id} ${a.petName || ""} ${a.customerName || ""} ${a.veterinarianName || ""}`.toLowerCase().includes(q)) return false;
      return true;
    });

    if (!list.length) {
      slot.innerHTML = `<div class="empty-state"><span class="material-symbols-outlined">event_busy</span><h4>No matching appointments</h4></div>`;
      return;
    }

    slot.innerHTML = `
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Pet &amp; Guardian</th>
              <th>Schedule Slot</th>
              <th>Assigned Vet</th>
              <th>Status</th>
              <th style="text-align:right;">Dispatch Actions</th>
            </tr>
          </thead>
          <tbody>
            ${list.map((a) => {
              const isOpen = ["PENDING", "RESCHEDULE_PROPOSED", "CONFIRMED"].includes(a.status);
              return `
                <tr>
                  <td class="numeric fw-700 text-primary">APT-${a.id}</td>
                  <td>
                    <div class="fw-700">${esc(a.petName || `Pet #${a.petId}`)}</div>
                    <div class="text-xs text-muted">${esc(a.customerName || `Customer #${a.customerId}`)}</div>
                  </td>
                  <td class="numeric text-sm">
                    <div class="fw-600">${esc(formatTimeRange(a.confirmedStart || a.requestedStart, a.confirmedEnd || a.requestedEnd))}</div>
                    <div class="text-xs text-muted">${a.confirmedStart ? "Confirmed" : "Requested"}</div>
                  </td>
                  <td>
                    ${a.veterinarianName
                      ? `<span class="text-sm fw-600">${esc(a.veterinarianName)}</span>`
                      : `<span class="badge badge-rejected">Unassigned</span>`}
                  </td>
                  <td>${renderAppointmentBadge(a.status)}</td>
                  <td style="text-align:right;">
                    <div class="row-gap" style="justify-content:flex-end;">
                      ${isOpen ? `
                        <button class="btn btn-tonal btn-sm" data-admin-assign="${a.id}">
                          ${a.veterinarianId ? "Reassign" : "Assign Vet"}
                        </button>
                      ` : ""}
                      <button class="btn btn-secondary btn-sm" data-admin-detail="${a.id}">
                        ${a.status === "RESCHEDULE_PROPOSED" ? "Negotiation" : "Details"}
                      </button>
                    </div>
                  </td>
                </tr>
              `;
            }).join("")}
          </tbody>
        </table>
      </div>
    `;

    slot.querySelectorAll("[data-admin-detail]").forEach((btn) => {
      btn.addEventListener("click", () => ctx.navigate("appointment-detail", { id: Number(btn.getAttribute("data-admin-detail")) }));
    });

    slot.querySelectorAll("[data-admin-assign]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const apptId = Number(btn.getAttribute("data-admin-assign"));
        const appt = appointments.find((a) => a.id === apptId);
        openModal({
          title: `Assign Veterinarian — APT-${apptId}`,
          bodyHtml: `
            <form id="quick-assign-form" class="stack-md">
              <div class="field">
                <label for="quick-assign-vet">Select Active Veterinarian <span class="req">*</span></label>
                <select id="quick-assign-vet" name="veterinarianId" class="select" required>
                  <option value="">Choose a veterinarian...</option>
                  ${vets.map((v) => `<option value="${v.id}" ${v.id === appt?.veterinarianId ? "selected" : ""}>${esc(v.fullName)} (${esc(v.email)})</option>`).join("")}
                </select>
              </div>
              <div class="row-between">
                <button type="button" class="btn btn-secondary" id="qa-cancel">Cancel</button>
                <button type="submit" class="btn btn-primary">Assign Veterinarian</button>
              </div>
            </form>
          `,
          onMount: () => {
            document.getElementById("qa-cancel")?.addEventListener("click", closeModal);
            document.getElementById("quick-assign-form")?.addEventListener("submit", async (e) => {
              e.preventDefault();
              const vetId = Number(new FormData(e.target).get("veterinarianId"));
              try {
                await api.assignVeterinarian(apptId, vetId);
                closeModal();
                showToast("Veterinarian assigned.", "success");
                renderAdminDashboardView(container, ctx);
              } catch (err) {
                showToast(err.message, "error");
              }
            });
          }
        });
      });
    });
  }

  const openProvisionModal = () => openStaffProvisionModal(() => renderAdminDashboardView(container, ctx));
  document.getElementById("admin-add-vet-btn")?.addEventListener("click", openProvisionModal);
  document.getElementById("sidebar-add-vet-btn")?.addEventListener("click", openProvisionModal);
  document.getElementById("admin-manage-staff-btn")?.addEventListener("click", () => ctx.navigate("users"));

  document.getElementById("admin-appt-tabs")?.querySelectorAll("[data-af]").forEach((btn) => {
    btn.addEventListener("click", () => {
      statusFilter = btn.getAttribute("data-af");
      document.getElementById("admin-appt-tabs").querySelectorAll("[data-af]").forEach((b) => b.classList.toggle("active", b === btn));
      renderDispatchTable();
    });
  });

  document.getElementById("admin-vet-filter")?.addEventListener("change", (e) => {
    vetFilter = e.target.value;
    renderDispatchTable();
  });

  document.getElementById("admin-appt-search")?.addEventListener("input", (e) => {
    searchQuery = e.target.value || "";
    renderDispatchTable();
  });

  renderDispatchTable();
}

/* =========================================================================
   2. ADMIN USER & STAFF MANAGEMENT
   ========================================================================= */
export async function renderAdminUsersView(container, ctx) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading clinic accounts...</p></div></div>`;

  const currentUser = ctx.getUser();
  let users = [];
  try {
    users = await api.getUsers();
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  let roleFilter = "ALL";
  let statusFilter = "ALL";
  let searchQuery = "";

  const counts = {
    ALL: users.length,
    VETERINARIAN: users.filter((u) => u.role === "VETERINARIAN").length,
    CUSTOMER: users.filter((u) => u.role === "CUSTOMER").length,
    ADMIN: users.filter((u) => u.role === "ADMIN").length
  };

  container.innerHTML = `
    <div class="page-header">
      <div>
        <span class="eyebrow">Access Governance</span>
        <h1>Users &amp; Staff Management</h1>
        <p>Provision veterinarian and administrator accounts, monitor pet parent registrations, and control account status (Active, Inactive, Locked).</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" id="users-add-staff-btn">
          <span class="material-symbols-outlined" style="font-size:18px;">person_add</span>
          Provision Staff Account
        </button>
      </div>
    </div>

    <section class="card stack-md">
      <div class="row-between">
        <div class="filter-bar" id="users-role-tabs">
          <button type="button" class="pill-tab active" data-rf="ALL">All Users <span class="count">${counts.ALL}</span></button>
          <button type="button" class="pill-tab" data-rf="VETERINARIAN">Veterinarians <span class="count">${counts.VETERINARIAN}</span></button>
          <button type="button" class="pill-tab" data-rf="CUSTOMER">Customers <span class="count">${counts.CUSTOMER}</span></button>
          <button type="button" class="pill-tab" data-rf="ADMIN">Administrators <span class="count">${counts.ADMIN}</span></button>
        </div>

        <div class="row-gap">
          <select id="users-status-filter" class="select" style="width:auto;min-width:140px;">
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="LOCKED">Locked</option>
          </select>
          <div class="input-with-icon" style="width:220px;">
            <span class="material-symbols-outlined leading-icon">search</span>
            <input id="users-search-input" class="input" placeholder="Search name, email...">
          </div>
        </div>
      </div>

      <div id="users-table-slot"></div>
    </section>
  `;

  function renderTable() {
    const slot = document.getElementById("users-table-slot");
    const q = searchQuery.trim().toLowerCase();
    const list = users.filter((u) => {
      if (roleFilter !== "ALL" && u.role !== roleFilter) return false;
      if (statusFilter !== "ALL" && u.status !== statusFilter) return false;
      if (q && !`${u.fullName} ${u.email} ${u.phone || ""}`.toLowerCase().includes(q)) return false;
      return true;
    });

    if (!list.length) {
      slot.innerHTML = `<div class="empty-state"><span class="material-symbols-outlined">group_off</span><h4>No matching accounts</h4></div>`;
      return;
    }

    slot.innerHTML = `
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Contact</th>
              <th>Role</th>
              <th>Status</th>
              <th>Joined</th>
              <th style="text-align:right;">Account Controls</th>
            </tr>
          </thead>
          <tbody>
            ${list.map((u) => {
              const isSelf = u.id === currentUser.id;
              const avatarUrl = safeImageUrl(u.avatarUrl);
              return `
                <tr>
                  <td>
                    <div class="row-gap">
                      <div class="avatar">
                        ${avatarUrl ? `<img src="${esc(avatarUrl)}" alt="${esc(u.fullName)}">` : esc(getInitials(u.fullName))}
                      </div>
                      <div>
                        <div class="fw-700">${esc(u.fullName)} ${isSelf ? '<span class="text-xs text-primary">(You)</span>' : ""}</div>
                        <div class="text-xs text-muted numeric">ID #${u.id}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div class="text-sm">${esc(u.email)}</div>
                    <div class="text-xs text-muted numeric">${esc(u.phone || "No phone")}</div>
                  </td>
                  <td>${renderRoleBadge(u.role)}</td>
                  <td>${renderUserStatusBadge(u.status)}</td>
                  <td class="numeric text-xs text-muted">${esc(formatDate(u.createdAt))}</td>
                  <td style="text-align:right;">
                    ${isSelf ? `<span class="text-xs text-muted">Current Session</span>` : `
                      <div class="row-gap" style="justify-content:flex-end;">
                        ${u.status !== "ACTIVE" ? `<button class="btn btn-primary btn-sm" data-set-status="${u.id}:ACTIVE">Activate</button>` : ""}
                        ${u.status !== "INACTIVE" ? `<button class="btn btn-secondary btn-sm" data-set-status="${u.id}:INACTIVE">Deactivate</button>` : ""}
                        ${u.status !== "LOCKED" ? `<button class="btn btn-danger-ghost btn-sm" data-set-status="${u.id}:LOCKED">Lock</button>` : ""}
                      </div>
                    `}
                  </td>
                </tr>
              `;
            }).join("")}
          </tbody>
        </table>
      </div>
    `;

    slot.querySelectorAll("[data-set-status]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const [idStr, newStatus] = btn.getAttribute("data-set-status").split(":");
        const id = Number(idStr);
        btn.disabled = true;
        try {
          await api.updateUserStatus(id, newStatus);
          showToast(`Account status updated to ${newStatus}.`, "success");
          await renderAdminUsersView(container, ctx);
        } catch (err) {
          showToast(err.message, "error");
          btn.disabled = false;
        }
      });
    });
  }

  document.getElementById("users-add-staff-btn")?.addEventListener("click", () => {
    openStaffProvisionModal(() => renderAdminUsersView(container, ctx));
  });

  document.getElementById("users-role-tabs")?.querySelectorAll("[data-rf]").forEach((btn) => {
    btn.addEventListener("click", () => {
      roleFilter = btn.getAttribute("data-rf");
      document.getElementById("users-role-tabs").querySelectorAll("[data-rf]").forEach((b) => b.classList.toggle("active", b === btn));
      renderTable();
    });
  });

  document.getElementById("users-status-filter")?.addEventListener("change", (e) => {
    statusFilter = e.target.value;
    renderTable();
  });

  document.getElementById("users-search-input")?.addEventListener("input", (e) => {
    searchQuery = e.target.value || "";
    renderTable();
  });

  renderTable();
}

export function openStaffProvisionModal(onCreated = null) {
  openModal({
    title: "Provision Clinic Staff Account",
    bodyHtml: `
      <form id="provision-staff-form" class="stack-md">
        <div class="callout callout-info">
          <span class="material-symbols-outlined text-primary" style="font-size:18px;">verified_user</span>
          <span>Creates an immediately active <strong>Veterinarian</strong> or <strong>Administrator</strong> account. Pet parents self-register via the public portal.</span>
        </div>

        <div class="form-grid">
          <div class="field col-span-full">
            <label for="st-role">Staff Role <span class="req">*</span></label>
            <select id="st-role" name="role" class="select" required>
              <option value="VETERINARIAN" selected>Veterinarian (Clinical Practitioner)</option>
              <option value="ADMIN">Clinic Administrator</option>
            </select>
          </div>

          <div class="field">
            <label for="st-name">Full Name <span class="req">*</span></label>
            <input id="st-name" name="fullName" class="input" required maxlength="100" placeholder="Dr. Emily Vance">
          </div>

          <div class="field">
            <label for="st-phone">Phone Number</label>
            <input id="st-phone" name="phone" class="input" maxlength="20" placeholder="+1 (555) 200-3400">
          </div>

          <div class="field">
            <label for="st-email">Clinic Email <span class="req">*</span></label>
            <input id="st-email" name="email" type="email" class="input" required maxlength="150" placeholder="dr.vance@meocare.clinic">
          </div>

          <div class="field">
            <label for="st-pwd">Initial Password <span class="req">*</span></label>
            <input id="st-pwd" name="password" type="password" class="input" required minlength="6" maxlength="100" placeholder="At least 6 characters">
          </div>
        </div>

        <div class="row-between">
          <button type="button" class="btn btn-secondary" id="st-cancel">Cancel</button>
          <button type="submit" class="btn btn-primary" id="st-submit">Create Staff Account</button>
        </div>
      </form>
    `,
    onMount: () => {
      document.getElementById("st-cancel")?.addEventListener("click", closeModal);
      document.getElementById("provision-staff-form")?.addEventListener("submit", async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const btn = document.getElementById("st-submit");
        btn.disabled = true;
        try {
          const created = await api.createStaffUser({
            fullName: String(fd.get("fullName") || "").trim(),
            email: String(fd.get("email") || "").trim(),
            password: String(fd.get("password") || ""),
            phone: String(fd.get("phone") || "").trim() || null,
            role: String(fd.get("role"))
          });
          closeModal();
          showToast(`${created.fullName} (${created.role}) provisioned!`, "success");
          if (typeof onCreated === "function") onCreated(created);
        } catch (err) {
          showToast(err.message, "error");
          btn.disabled = false;
        }
      });
    }
  });
}

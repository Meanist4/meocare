import { api } from "./api.js";

export function esc(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function safeImageUrl(url) {
  if (!url || typeof url !== "string") return "";
  const trimmed = url.trim();
  if (trimmed.startsWith("/uploads/") || trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
    return trimmed;
  }
  return "";
}

export function getInitials(name) {
  if (!name) return "MC";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function formatDateTime(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

export function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric"
  }).format(date);
}

export function formatTimeRange(start, end) {
  if (!start) return "—";
  const s = new Date(start);
  if (Number.isNaN(s.getTime())) return String(start);
  const datePart = new Intl.DateTimeFormat("en-US", {
    weekday: "short",
    month: "short",
    day: "2-digit",
    year: "numeric"
  }).format(s);
  const startTime = new Intl.DateTimeFormat("en-US", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(s);
  if (!end) return `${datePart} · ${startTime}`;
  const e = new Date(end);
  if (Number.isNaN(e.getTime())) return `${datePart} · ${startTime}`;
  const endTime = new Intl.DateTimeFormat("en-US", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(e);
  return `${datePart} · ${startTime} – ${endTime}`;
}

export function toInputDateTime(dateOrString) {
  const date = dateOrString instanceof Date ? dateOrString : new Date(dateOrString);
  if (Number.isNaN(date.getTime())) return "";
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function defaultFutureSlot(hoursAhead = 24, durationMinutes = 45) {
  const start = new Date();
  start.setHours(start.getHours() + hoursAhead, 0, 0, 0);
  const end = new Date(start.getTime() + durationMinutes * 60_000);
  return {
    start: toInputDateTime(start),
    end: toInputDateTime(end)
  };
}

export function calcPetAge(dob) {
  if (!dob) return "Unknown age";
  const birth = new Date(dob);
  if (Number.isNaN(birth.getTime())) return "Unknown age";
  const now = new Date();
  let months = (now.getFullYear() - birth.getFullYear()) * 12 + (now.getMonth() - birth.getMonth());
  if (months < 0) return "Newborn";
  const years = Math.floor(months / 12);
  const remMonths = months % 12;
  if (years === 0) return `${remMonths} mo${remMonths === 1 ? "" : "s"}`;
  if (remMonths === 0) return `${years} yr${years === 1 ? "" : "s"}`;
  return `${years}y ${remMonths}m`;
}

export function roleLabel(role) {
  switch (role) {
    case "ADMIN": return "Clinic Administrator";
    case "VETERINARIAN": return "Veterinarian";
    case "CUSTOMER": return "Pet Parent";
    default: return role || "User";
  }
}

export function renderAppointmentBadge(status) {
  const map = {
    PENDING: ["badge-pending", "Pending"],
    RESCHEDULE_PROPOSED: ["badge-reschedule", "Reschedule Proposed"],
    CONFIRMED: ["badge-confirmed", "Confirmed"],
    REJECTED: ["badge-rejected", "Rejected"],
    CANCELLED: ["badge-cancelled", "Cancelled"],
    COMPLETED: ["badge-completed", "Completed"]
  };
  const [cls, label] = map[status] || ["badge-neutral", status || "Unknown"];
  return `<span class="badge ${cls}">${esc(label)}</span>`;
}

export function renderProposalBadge(status) {
  const map = {
    PENDING: ["badge-reschedule", "Pending Response"],
    ACCEPTED: ["badge-accepted", "Accepted"],
    DECLINED: ["badge-declined", "Declined"]
  };
  const [cls, label] = map[status] || ["badge-neutral", status || "Unknown"];
  return `<span class="badge ${cls}">${esc(label)}</span>`;
}

export function renderUserStatusBadge(status) {
  const map = {
    ACTIVE: ["badge-active", "Active"],
    INACTIVE: ["badge-inactive", "Inactive"],
    LOCKED: ["badge-locked", "Locked"]
  };
  const [cls, label] = map[status] || ["badge-neutral", status || "Unknown"];
  return `<span class="badge ${cls}">${esc(label)}</span>`;
}

export function renderRoleBadge(role) {
  const map = {
    ADMIN: ["badge-primary badge-no-dot", "Admin"],
    VETERINARIAN: ["badge-confirmed badge-no-dot", "Veterinarian"],
    CUSTOMER: ["badge-neutral badge-no-dot", "Customer"]
  };
  const [cls, label] = map[role] || ["badge-neutral badge-no-dot", role || "User"];
  return `<span class="badge ${cls}">${esc(label)}</span>`;
}

export function renderHealthBadge(status) {
  const upper = String(status || "").toUpperCase();
  let cls = "badge-primary";
  if (upper.includes("STABLE") || upper.includes("HEALTHY") || upper.includes("NORMAL") || upper.includes("RECOVER")) {
    cls = "badge-confirmed";
  } else if (upper.includes("IMPROV") || upper.includes("GOOD")) {
    cls = "badge-completed";
  } else if (upper.includes("OBSERV") || upper.includes("MONITOR") || upper.includes("MILD")) {
    cls = "badge-pending";
  } else if (upper.includes("CRITIC") || upper.includes("URGENT") || upper.includes("ALERT") || upper.includes("WORSE")) {
    cls = "badge-rejected";
  }
  return `<span class="badge ${cls}">${esc(status || "Update")}</span>`;
}

export function showToast(message, type = "info") {
  const root = document.getElementById("toast-root");
  if (!root) return;
  const el = document.createElement("div");
  el.className = `toast toast-${type}`;
  const icon = type === "error" ? "error" : type === "success" ? "check_circle" : "info";
  el.innerHTML = `
    <span class="material-symbols-outlined" style="font-size:19px;">${icon}</span>
    <span>${esc(message)}</span>
  `;
  root.appendChild(el);
  window.setTimeout(() => {
    el.remove();
  }, 4600);
}

export function openModal({ title, bodyHtml, footerHtml = "", large = false, onMount = null }) {
  const root = document.getElementById("modal-root");
  if (!root) return;
  root.innerHTML = `
    <div class="modal-backdrop" id="active-modal-backdrop">
      <div class="modal-panel ${large ? "modal-lg" : ""}" role="dialog" aria-modal="true">
        <div class="modal-header">
          <h3>${esc(title)}</h3>
          <button type="button" class="btn btn-ghost btn-icon" id="modal-close-btn" aria-label="Close modal">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="modal-body" id="modal-body-slot">${bodyHtml}</div>
        ${footerHtml ? `<div class="modal-footer">${footerHtml}</div>` : ""}
      </div>
    </div>
  `;
  const backdrop = document.getElementById("active-modal-backdrop");
  const closeBtn = document.getElementById("modal-close-btn");
  closeBtn?.addEventListener("click", closeModal);
  backdrop?.addEventListener("click", (e) => {
    if (e.target === backdrop) closeModal();
  });
  if (typeof onMount === "function") {
    onMount(root);
  }
}

export function closeModal() {
  const root = document.getElementById("modal-root");
  if (root) root.innerHTML = "";
}

export function openLightbox(url) {
  const safe = safeImageUrl(url);
  if (!safe) return;
  const root = document.getElementById("lightbox-root");
  if (!root) return;
  root.innerHTML = `
    <div class="lightbox-backdrop" id="active-lightbox">
      <button type="button" class="lightbox-close" id="lightbox-close-btn" aria-label="Close image preview">
        <span class="material-symbols-outlined">close</span>
      </button>
      <img src="${esc(safe)}" alt="Clinical image preview">
    </div>
  `;
  const close = () => { root.innerHTML = ""; };
  document.getElementById("lightbox-close-btn")?.addEventListener("click", close);
  document.getElementById("active-lightbox")?.addEventListener("click", (e) => {
    if (e.target.id === "active-lightbox") close();
  });
}

export function bindLightboxClicks(container) {
  container.querySelectorAll("[data-lightbox-url]").forEach((el) => {
    el.addEventListener("click", (e) => {
      if (e.target.closest("button")) return;
      openLightbox(el.getAttribute("data-lightbox-url"));
    });
  });
}

/**
 * Reusable attachment manager supporting both local multipart upload (POST /api/uploads/images)
 * and direct external URL entry.
 */
export function mountMediaPicker(container, { initialUrls = [], label = "Photos & Media" } = {}) {
  const urls = [...initialUrls];

  function render() {
    container.innerHTML = `
      <div class="stack-sm">
        <div class="row-between">
          <label class="text-sm fw-600" style="color:var(--ink-cypress)">${esc(label)}</label>
          <span class="text-xs text-muted">${urls.length} attached</span>
        </div>
        <div class="dropzone" data-dropzone>
          <span class="material-symbols-outlined text-primary" style="font-size:28px;">cloud_upload</span>
          <span class="text-sm fw-600">Drop image files here or click to upload</span>
          <span class="text-xs text-muted">JPEG, PNG, WebP, or GIF up to 10 MB</span>
          <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" multiple hidden data-file-input>
        </div>
        <div class="row-gap">
          <input type="url" class="input" style="flex:1" placeholder="Or paste an image URL (https://...)" data-url-input>
          <button type="button" class="btn btn-secondary" data-add-url-btn>
            <span class="material-symbols-outlined" style="font-size:17px;">add_link</span>
            Add URL
          </button>
        </div>
        ${urls.length ? `
          <div class="media-grid">
            ${urls.map((u, idx) => `
              <div class="media-thumb" data-lightbox-url="${esc(u)}">
                <img src="${esc(safeImageUrl(u))}" alt="Uploaded attachment ${idx + 1}">
                <button type="button" class="media-remove-btn" data-remove-idx="${idx}" title="Remove photo">
                  <span class="material-symbols-outlined" style="font-size:15px;">close</span>
                </button>
              </div>
            `).join("")}
          </div>
        ` : ""}
      </div>
    `;

    const dropzone = container.querySelector("[data-dropzone]");
    const fileInput = container.querySelector("[data-file-input]");
    const urlInput = container.querySelector("[data-url-input]");
    const addUrlBtn = container.querySelector("[data-add-url-btn]");

    const handleFiles = async (fileList) => {
      for (const file of Array.from(fileList || [])) {
        try {
          dropzone.classList.add("dragover");
          const uploaded = await api.uploadImage(file);
          if (uploaded?.url) {
            urls.push(uploaded.url);
            showToast(`Uploaded ${file.name}`, "success");
          }
        } catch (err) {
          showToast(err.message || `Failed to upload ${file.name}`, "error");
        } finally {
          dropzone.classList.remove("dragover");
        }
      }
      render();
    };

    dropzone?.addEventListener("click", () => fileInput?.click());
    dropzone?.addEventListener("dragover", (e) => {
      e.preventDefault();
      dropzone.classList.add("dragover");
    });
    dropzone?.addEventListener("dragleave", () => dropzone.classList.remove("dragover"));
    dropzone?.addEventListener("drop", (e) => {
      e.preventDefault();
      dropzone.classList.remove("dragover");
      handleFiles(e.dataTransfer?.files);
    });
    fileInput?.addEventListener("change", () => handleFiles(fileInput.files));

    addUrlBtn?.addEventListener("click", () => {
      const val = (urlInput?.value || "").trim();
      if (!val) return;
      if (!safeImageUrl(val)) {
        showToast("Please enter a valid http(s):// or /uploads/ image URL.", "error");
        return;
      }
      urls.push(val);
      render();
    });

    container.querySelectorAll("[data-remove-idx]").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        e.stopPropagation();
        const idx = Number(btn.getAttribute("data-remove-idx"));
        urls.splice(idx, 1);
        render();
      });
    });

    bindLightboxClicks(container);
  }

  render();

  return {
    getUrls: () => [...urls],
    clear: () => {
      urls.length = 0;
      render();
    }
  };
}

import { api, clearToken, getToken, setUnauthorizedHandler } from "./js/api.js";
import { closeModal, esc, getInitials, roleLabel, safeImageUrl, showToast } from "./js/utils.js";
import { renderLoginView, renderRegisterView } from "./js/views/authViews.js";
import { renderProfileView } from "./js/views/profileView.js";
import { renderAppointmentDetailView } from "./js/views/appointmentDetailView.js";
import {
  renderCreateAppointmentView,
  renderCustomerAppointmentsTimelineView,
  renderCustomerDashboardView,
  renderCustomerRecordsView,
  renderPetsDirectoryView
} from "./js/views/customerViews.js";
import {
  renderClinicalRecordsWorkspace,
  renderVetScheduleView
} from "./js/views/vetViews.js";
import {
  renderAdminDashboardView,
  renderAdminUsersView
} from "./js/views/adminViews.js";

const appRoot = document.getElementById("app");

const appState = {
  user: null,
  route: "login",
  params: {},
  shellMountedRole: null
};

function getNavConfig(role) {
  if (role === "ADMIN") {
    return {
      portalTitle: "Clinic Operations Portal",
      sectionTitle: "Admin Workspace",
      items: [
        { route: "dashboard", label: "Operations & Dispatch", icon: "dashboard" },
        { route: "users", label: "Users & Staff", icon: "group" },
        { route: "pets", label: "Clinic Pets", icon: "pets" },
        { route: "records", label: "Medical Records", icon: "vital_signs" }
      ]
    };
  }
  if (role === "VETERINARIAN") {
    return {
      portalTitle: "Veterinarian Clinical Suite",
      sectionTitle: "Clinical Practice",
      items: [
        { route: "dashboard", label: "Appointments & Queue", icon: "calendar_month" },
        { route: "records", label: "Exam & Medical Records", icon: "clinical_notes" },
        { route: "pets", label: "Clinic Patients", icon: "pets" }
      ]
    };
  }
  return {
    portalTitle: "Pet Caregiver Portal",
    sectionTitle: "Pet Care",
    items: [
      { route: "dashboard", label: "Dashboard", icon: "grid_view" },
      { route: "pets", label: "My Pets", icon: "pets" },
      { route: "book", label: "Request Appointment", icon: "add_circle" },
      { route: "appointments", label: "Appointments & Timeline", icon: "event_available" },
      { route: "records", label: "Medical Records", icon: "vital_signs" }
    ]
  };
}

function parseHash() {
  const raw = (window.location.hash || "").replace(/^#\/?/, "");
  if (!raw) return { route: "", params: {} };
  const [pathPart, queryPart] = raw.split("?");
  const params = {};
  if (queryPart) {
    const sp = new URLSearchParams(queryPart);
    sp.forEach((v, k) => { params[k] = v; });
  }
  const segments = pathPart.split("/").filter(Boolean);
  if (segments[0] === "appointments" && segments[1] && /^\d+$/.test(segments[1])) {
    return { route: "appointment-detail", params: { ...params, id: Number(segments[1]) } };
  }
  return { route: segments[0] || "", params };
}

function buildHash(route, params = {}) {
  if (route === "appointment-detail" && params.id) {
    return `#appointments/${params.id}`;
  }
  const qs = new URLSearchParams();
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "" && k !== "noticeMessage" && k !== "prefillEmail") {
      qs.set(k, String(v));
    }
  });
  const qStr = qs.toString();
  return `#${route}${qStr ? `?${qStr}` : ""}`;
}

function navigate(route, params = {}, { replace = false } = {}) {
  closeModal();
  appState.route = route;
  appState.params = params || {};
  const targetHash = buildHash(route, params);
  if (window.location.hash !== targetHash) {
    if (replace) {
      window.history.replaceState(null, "", targetHash);
    } else {
      window.location.hash = targetHash;
      return; // hashchange listener will invoke renderCurrentRoute
    }
  }
  renderCurrentRoute();
}

function refreshShellUser() {
  if (!appState.user) return;
  const nameEl = document.getElementById("shell-user-name");
  const roleEl = document.getElementById("shell-user-role");
  const avatarEl = document.getElementById("shell-user-avatar");
  if (nameEl) nameEl.textContent = appState.user.fullName;
  if (roleEl) roleEl.textContent = roleLabel(appState.user.role);
  if (avatarEl) {
    const safeUrl = safeImageUrl(appState.user.avatarUrl);
    avatarEl.innerHTML = safeUrl
      ? `<img src="${esc(safeUrl)}" alt="${esc(appState.user.fullName)}">`
      : esc(getInitials(appState.user.fullName));
  }
}

function ensureAppShell() {
  const user = appState.user;
  if (!user) return null;

  if (appState.shellMountedRole === user.role && document.getElementById("view-container")) {
    refreshShellUser();
    updateActiveNav();
    return document.getElementById("view-container");
  }

  const nav = getNavConfig(user.role);
  const safeAvatar = safeImageUrl(user.avatarUrl);

  appRoot.innerHTML = `
    <header class="shell-topbar">
      <div class="topbar-left">
        <button type="button" class="mobile-menu-btn" id="mobile-menu-btn" aria-label="Toggle navigation">
          <span class="material-symbols-outlined">menu</span>
        </button>
        <a href="#dashboard" class="row-gap" id="shell-brand-link" style="gap:0.6rem;text-decoration:none;">
          <span class="brand-mark brand-mark-sm" aria-hidden="true">m</span>
          <span class="brand-title" style="font-size:1.08rem;">Meocare</span>
        </a>
        <span class="topbar-divider"></span>
        <span class="topbar-portal-label">Meocare Veterinary Clinic · ${esc(nav.portalTitle)}</span>
      </div>

      <div class="topbar-right">
        <button type="button" class="user-chip" id="topbar-profile-chip" title="Open My Profile">
          <span class="avatar" id="shell-user-avatar">
            ${safeAvatar ? `<img src="${esc(safeAvatar)}" alt="${esc(user.fullName)}">` : esc(getInitials(user.fullName))}
          </span>
          <span class="user-chip-info">
            <span class="user-chip-name" id="shell-user-name">${esc(user.fullName)}</span>
            <span class="user-chip-role" id="shell-user-role">${esc(roleLabel(user.role))}</span>
          </span>
        </button>
        <button type="button" class="btn btn-secondary btn-sm" id="topbar-logout-btn" title="Sign out">
          <span class="material-symbols-outlined" style="font-size:17px;">logout</span>
          <span>Sign Out</span>
        </button>
      </div>
    </header>

    <div class="sidebar-backdrop" id="sidebar-backdrop"></div>

    <aside class="shell-sidebar" id="shell-sidebar" aria-label="Primary navigation">
      <div>
        <div class="nav-section">
          <div class="nav-section-title">${esc(nav.sectionTitle)}</div>
          ${nav.items.map((item) => `
            <button type="button" class="nav-item" data-nav-route="${esc(item.route)}">
              <span class="material-symbols-outlined" style="font-size:20px;">${esc(item.icon)}</span>
              <span>${esc(item.label)}</span>
            </button>
          `).join("")}
        </div>

        <div class="nav-section">
          <div class="nav-section-title">Account</div>
          <button type="button" class="nav-item" data-nav-route="profile">
            <span class="material-symbols-outlined" style="font-size:20px;">account_circle</span>
            <span>My Profile</span>
          </button>
          <button type="button" class="nav-item" id="sidebar-logout-btn">
            <span class="material-symbols-outlined" style="font-size:20px;">logout</span>
            <span>Sign Out</span>
          </button>
        </div>
      </div>

      <div class="stack-sm">
        <div class="sidebar-card">
          <div class="sidebar-card-title">
            <span class="material-symbols-outlined" style="font-size:16px;">local_hospital</span>
            Single Clinic EMR
          </div>
          <div class="text-xs text-muted">
            Meocare Veterinary Clinic · Role-based scheduling, clinical exams, and daily health timelines.
          </div>
        </div>
      </div>
    </aside>

    <main class="shell-main">
      <div class="page-container" id="view-container"></div>
    </main>
  `;

  appState.shellMountedRole = user.role;

  const sidebar = document.getElementById("shell-sidebar");
  const backdrop = document.getElementById("sidebar-backdrop");
  const closeDrawer = () => {
    sidebar?.classList.remove("open");
    backdrop?.classList.remove("open");
  };

  document.getElementById("mobile-menu-btn")?.addEventListener("click", () => {
    sidebar?.classList.toggle("open");
    backdrop?.classList.toggle("open");
  });
  backdrop?.addEventListener("click", closeDrawer);

  document.getElementById("shell-brand-link")?.addEventListener("click", (e) => {
    e.preventDefault();
    closeDrawer();
    navigate("dashboard");
  });
  document.getElementById("topbar-profile-chip")?.addEventListener("click", () => {
    closeDrawer();
    navigate("profile");
  });

  appRoot.querySelectorAll("[data-nav-route]").forEach((btn) => {
    btn.addEventListener("click", () => {
      closeDrawer();
      navigate(btn.getAttribute("data-nav-route"));
    });
  });

  const handleLogout = () => {
    clearToken();
    appState.user = null;
    appState.shellMountedRole = null;
    showToast("You have been signed out.", "info");
    navigate("login", {}, { replace: true });
  };

  document.getElementById("topbar-logout-btn")?.addEventListener("click", handleLogout);
  document.getElementById("sidebar-logout-btn")?.addEventListener("click", handleLogout);

  updateActiveNav();
  return document.getElementById("view-container");
}

function updateActiveNav() {
  const effectiveRoute =
    appState.route === "appointment-detail"
      ? (appState.user?.role === "CUSTOMER" ? "appointments" : "dashboard")
      : (appState.route === "appointments" && appState.user?.role !== "CUSTOMER" ? "dashboard" : appState.route);

  document.querySelectorAll("[data-nav-route]").forEach((btn) => {
    btn.classList.toggle("active", btn.getAttribute("data-nav-route") === effectiveRoute);
  });
}

function renderAccessDenied(container) {
  container.innerHTML = `
    <div class="card">
      <div class="empty-state">
        <span class="material-symbols-outlined text-error" style="font-size:44px;">gpp_bad</span>
        <h4>Access Denied (403)</h4>
        <p>Your account role (<strong>${esc(roleLabel(appState.user?.role))}</strong>) does not have permission to view this section.</p>
        <button class="btn btn-primary" id="return-home-403">Return to My Dashboard</button>
      </div>
    </div>
  `;
  document.getElementById("return-home-403")?.addEventListener("click", () => navigate("dashboard"));
}

const ctx = {
  getUser: () => appState.user,
  setUser: (u) => { appState.user = u; },
  refreshShellUser,
  navigate
};

async function renderCurrentRoute() {
  const user = appState.user;

  if (!user) {
    appState.shellMountedRole = null;
    if (appState.route === "register") {
      renderRegisterView(appRoot, { navigate });
    } else {
      renderLoginView(appRoot, {
        navigate,
        noticeMessage: appState.params.noticeMessage || "",
        prefillEmail: appState.params.prefillEmail || "",
        onAuthSuccess: (loggedInUser) => {
          appState.user = loggedInUser;
          navigate("dashboard", {}, { replace: true });
        }
      });
    }
    return;
  }

  // Authenticated user hitting login/register -> redirect to dashboard
  if (appState.route === "login" || appState.route === "register" || !appState.route) {
    navigate("dashboard", {}, { replace: true });
    return;
  }

  const viewContainer = ensureAppShell();
  if (!viewContainer) return;
  updateActiveNav();

  const role = user.role;
  const route = appState.route;
  const params = appState.params || {};

  if (route === "profile") {
    await renderProfileView(viewContainer, ctx);
    return;
  }

  if (route === "appointment-detail" && params.id) {
    await renderAppointmentDetailView(viewContainer, ctx, Number(params.id));
    return;
  }

  // CUSTOMER routes
  if (role === "CUSTOMER") {
    if (route === "dashboard") return renderCustomerDashboardView(viewContainer, ctx);
    if (route === "pets") return renderPetsDirectoryView(viewContainer, ctx);
    if (route === "book") return renderCreateAppointmentView(viewContainer, ctx, params);
    if (route === "appointments") return renderCustomerAppointmentsTimelineView(viewContainer, ctx);
    if (route === "records") return renderCustomerRecordsView(viewContainer, ctx, params);
    if (route === "users") return renderAccessDenied(viewContainer);
    return renderCustomerDashboardView(viewContainer, ctx);
  }

  // VETERINARIAN routes
  if (role === "VETERINARIAN") {
    if (route === "dashboard" || route === "appointments") return renderVetScheduleView(viewContainer, ctx);
    if (route === "records") return renderClinicalRecordsWorkspace(viewContainer, ctx, params);
    if (route === "pets") return renderPetsDirectoryView(viewContainer, ctx);
    if (route === "users" || route === "book") return renderAccessDenied(viewContainer);
    return renderVetScheduleView(viewContainer, ctx);
  }

  // ADMIN routes
  if (role === "ADMIN") {
    if (route === "dashboard" || route === "appointments") return renderAdminDashboardView(viewContainer, ctx);
    if (route === "users") return renderAdminUsersView(viewContainer, ctx);
    if (route === "pets") return renderPetsDirectoryView(viewContainer, ctx);
    if (route === "records") return renderClinicalRecordsWorkspace(viewContainer, ctx, params);
    if (route === "book") return renderAccessDenied(viewContainer);
    return renderAdminDashboardView(viewContainer, ctx);
  }

  renderAccessDenied(viewContainer);
}

async function bootstrap() {
  setUnauthorizedHandler((msg) => {
    appState.user = null;
    appState.shellMountedRole = null;
    showToast(msg || "Session expired. Please sign in again.", "error");
    navigate("login", { noticeMessage: "Your session expired. Please sign in again." }, { replace: true });
  });

  window.addEventListener("hashchange", () => {
    const parsed = parseHash();
    appState.route = parsed.route || (appState.user ? "dashboard" : "login");
    appState.params = parsed.params || {};
    renderCurrentRoute();
  });

  const initial = parseHash();
  const token = getToken();

  if (token) {
    try {
      const me = await api.getMe();
      appState.user = me;
      appState.route = initial.route && initial.route !== "login" && initial.route !== "register"
        ? initial.route
        : "dashboard";
      appState.params = initial.params || {};
      navigate(appState.route, appState.params, { replace: true });
      return;
    } catch {
      clearToken();
      appState.user = null;
    }
  }

  appState.route = initial.route === "register" ? "register" : "login";
  appState.params = {};
  navigate(appState.route, {}, { replace: true });
}

bootstrap();

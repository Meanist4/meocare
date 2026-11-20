import { api, setToken } from "../api.js";
import { esc, showToast } from "../utils.js";

export function renderLoginView(root, { onAuthSuccess, navigate, noticeMessage = "", prefillEmail = "" }) {
  root.innerHTML = `
    <div class="auth-page">
      <header class="auth-header">
        <span class="brand-mark" aria-hidden="true">m</span>
        <span class="brand-title">Meocare Veterinary Clinic</span>
      </header>

      <main class="auth-main">
        <div class="auth-grid">
          <section class="auth-hero">
            <div class="row-between">
              <span class="badge badge-confirmed">Single Clinic Portal</span>
              <span class="text-xs text-muted fw-600">Meocare EMR</span>
            </div>

            <div class="auth-hero-copy">
              <span class="eyebrow">Compassionate Precision</span>
              <h1>Compassionate Veterinary Care &amp; Seamless Health Management</h1>
              <p>
                Welcome to the centralized Meocare clinic portal. Manage companion profiles,
                coordinate appointment negotiations, inspect certified medical records, and follow
                daily clinician-verified health timelines under one calm roof.
              </p>
            </div>

            <div class="auth-clinic-card">
              <div class="row-gap">
                <span class="material-symbols-outlined icon-filled" style="color:var(--primary-fixed);">medical_services</span>
                <h3>Unified Role-Based Clinic Workspace</h3>
              </div>
              <p>
                Sign in with your clinic credentials to automatically open your Pet Parent Hub,
                Veterinarian Clinical Suite, or Clinic Operations &amp; Dispatch Dashboard.
              </p>
            </div>

            <div class="auth-pillars">
              <div class="pillar-card">
                <div class="pillar-title">
                  <span class="material-symbols-outlined text-primary" style="font-size:17px;">event_repeat</span>
                  Negotiated Slots
                </div>
                <p>Transparent counter-proposals with real-time veterinarian conflict protection.</p>
              </div>
              <div class="pillar-card">
                <div class="pillar-title">
                  <span class="material-symbols-outlined text-secondary" style="font-size:17px;">clinical_notes</span>
                  Verified Records
                </div>
                <p>Structured vitals, examination notes, diagnosis, treatment plans, and diagnostic media.</p>
              </div>
              <div class="pillar-card">
                <div class="pillar-title">
                  <span class="material-symbols-outlined text-primary" style="font-size:17px;">timeline</span>
                  Daily Timeline
                </div>
                <p>Chronological recovery updates and progress photos authored directly by clinic vets.</p>
              </div>
            </div>
          </section>

          <section class="auth-card">
            <div>
              <div class="row-between">
                <span class="eyebrow">Secure Access</span>
                <span class="text-xs text-muted row-gap" style="gap:4px;">
                  <span class="material-symbols-outlined text-secondary" style="font-size:15px;">lock</span>
                  JWT Protected
                </span>
              </div>
              <h2>Sign in to Portal</h2>
              <p class="subtitle">Your role workspace opens automatically upon authentication.</p>
            </div>

            ${noticeMessage ? `
              <div class="callout callout-teal">
                <span class="material-symbols-outlined text-primary" style="font-size:19px;">check_circle</span>
                <span>${esc(noticeMessage)}</span>
              </div>
            ` : ""}

            <div id="login-error-box" class="callout callout-error" hidden></div>

            <form id="login-form" class="stack-md">
              <div class="field">
                <label for="login-email">Email Address <span class="req">*</span></label>
                <div class="input-with-icon">
                  <span class="material-symbols-outlined leading-icon">mail</span>
                  <input id="login-email" name="email" type="email" class="input" required
                         autocomplete="email" placeholder="you@example.com" value="${esc(prefillEmail)}">
                </div>
              </div>

              <div class="field">
                <label for="login-password">Password <span class="req">*</span></label>
                <div class="input-with-icon">
                  <span class="material-symbols-outlined leading-icon">key</span>
                  <input id="login-password" name="password" type="password" class="input" required
                         autocomplete="current-password" placeholder="Enter your password" style="padding-right:2.5rem;">
                  <button type="button" class="trailing-btn" id="toggle-login-pwd" aria-label="Toggle password visibility">
                    <span class="material-symbols-outlined" style="font-size:19px;">visibility</span>
                  </button>
                </div>
              </div>

              <button type="submit" class="btn btn-primary btn-lg btn-block" id="login-submit-btn">
                <span>Sign In to Meocare</span>
                <span class="material-symbols-outlined" style="font-size:19px;">arrow_forward</span>
              </button>
            </form>

            <div class="callout callout-info">
              <span class="material-symbols-outlined text-primary" style="font-size:19px;">info</span>
              <div>
                <div class="fw-600" style="color:var(--ink);">New pet parent?</div>
                <p style="margin:2px 0 0;">
                  <a href="#register" id="go-register-link" class="fw-600">Create Customer Account</a>
                  to register your pets, request visits, and follow daily health timelines.
                </p>
              </div>
            </div>

            <p class="text-xs text-muted" style="text-align:center;font-style:italic;margin:0;">
              Veterinarian &amp; Clinic Staff accounts are provisioned exclusively by Clinic Administration.
            </p>
          </section>
        </div>
      </main>

      <footer class="auth-footer">
        Meocare Veterinary Clinic • Dedicated Compassionate Animal Care
      </footer>
    </div>
  `;

  const pwdInput = document.getElementById("login-password");
  document.getElementById("toggle-login-pwd")?.addEventListener("click", (e) => {
    const icon = e.currentTarget.querySelector(".material-symbols-outlined");
    if (pwdInput.type === "password") {
      pwdInput.type = "text";
      icon.textContent = "visibility_off";
    } else {
      pwdInput.type = "password";
      icon.textContent = "visibility";
    }
  });

  document.getElementById("go-register-link")?.addEventListener("click", (e) => {
    e.preventDefault();
    navigate("register");
  });

  const form = document.getElementById("login-form");
  const errorBox = document.getElementById("login-error-box");
  const submitBtn = document.getElementById("login-submit-btn");

  form?.addEventListener("submit", async (e) => {
    e.preventDefault();
    errorBox.hidden = true;
    const data = new FormData(form);
    const email = String(data.get("email") || "").trim();
    const password = String(data.get("password") || "");

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="spinner spinner-sm"></span><span>Signing in...</span>`;

    try {
      const res = await api.login({ email, password });
      setToken(res.token);
      showToast(`Welcome back, ${res.user.fullName}!`, "success");
      onAuthSuccess(res.user);
    } catch (err) {
      errorBox.innerHTML = `<span class="material-symbols-outlined" style="font-size:19px;">error</span><span>${esc(err.message)}</span>`;
      errorBox.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = `<span>Sign In to Meocare</span><span class="material-symbols-outlined" style="font-size:19px;">arrow_forward</span>`;
    }
  });
}

export function renderRegisterView(root, { navigate }) {
  root.innerHTML = `
    <div class="auth-page">
      <header class="auth-header">
        <span class="brand-mark" aria-hidden="true">m</span>
        <span class="brand-title">Meocare Veterinary Clinic</span>
      </header>

      <main class="auth-main">
        <div class="auth-grid register-grid">
          <section class="auth-hero">
            <div class="stack-md">
              <span class="badge badge-confirmed" style="align-self:flex-start;">Pet Parent Portal</span>
              <div class="auth-hero-copy">
                <h1>Compassionate care starts with a single profile.</h1>
                <p>
                  Create your pet parent account to maintain companion profiles, request checkups
                  with symptom photos, negotiate convenient visit slots, and read verified clinic records.
                </p>
              </div>
            </div>

            <div class="stack-sm">
              <div class="card" style="padding:1rem;">
                <div class="row-gap">
                  <span class="material-symbols-outlined icon-filled text-primary">verified_user</span>
                  <div>
                    <div class="fw-700">Certified Medical Records</div>
                    <div class="text-xs text-muted">
                      Every examination summary and daily recovery update is authored and locked by Meocare veterinarians.
                    </div>
                  </div>
                </div>
              </div>
              <div class="card" style="padding:1rem;">
                <div class="row-gap">
                  <span class="material-symbols-outlined icon-filled text-secondary">pets</span>
                  <div>
                    <div class="fw-700">Multi-Pet Household Ready</div>
                    <div class="text-xs text-muted">
                      Manage all of your dogs, cats, and companions with individual photo galleries and visit histories.
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section class="auth-card">
            <div>
              <span class="badge badge-primary">New Pet Parent Registration</span>
              <h2>Create Your Household Account</h2>
              <p class="subtitle">Register your details to schedule examinations and follow care timelines.</p>
            </div>

            <div class="callout callout-info">
              <span class="material-symbols-outlined text-primary" style="font-size:19px;">info</span>
              <div>
                <strong style="color:var(--primary);">Notice of Scope:</strong>
                Public registration strictly enrolls you as a <strong>Pet Parent (Customer)</strong>.
                Veterinarian and clinic administrator credentials are provisioned directly by Clinic Administration.
              </div>
            </div>

            <div id="register-error-box" class="callout callout-error" hidden></div>

            <form id="register-form" class="stack-md">
              <div class="field">
                <label for="reg-name">Full Name <span class="req">*</span></label>
                <div class="input-with-icon">
                  <span class="material-symbols-outlined leading-icon">person</span>
                  <input id="reg-name" name="fullName" type="text" class="input" required maxlength="100"
                         placeholder="Sarah Jenkins">
                </div>
              </div>

              <div class="form-grid">
                <div class="field">
                  <label for="reg-email">Email Address <span class="req">*</span></label>
                  <div class="input-with-icon">
                    <span class="material-symbols-outlined leading-icon">mail</span>
                    <input id="reg-email" name="email" type="email" class="input" required maxlength="150"
                           placeholder="sarah@oakmail.com">
                  </div>
                </div>

                <div class="field">
                  <label for="reg-phone">Phone Number</label>
                  <div class="input-with-icon">
                    <span class="material-symbols-outlined leading-icon">smartphone</span>
                    <input id="reg-phone" name="phone" type="tel" class="input" maxlength="20"
                           placeholder="+1 (555) 234-8901">
                  </div>
                </div>
              </div>

              <div class="form-grid">
                <div class="field">
                  <label for="reg-password">Account Password <span class="req">*</span></label>
                  <div class="input-with-icon">
                    <span class="material-symbols-outlined leading-icon">key</span>
                    <input id="reg-password" name="password" type="password" class="input" required minlength="6" maxlength="100"
                           placeholder="At least 6 characters" style="padding-right:2.5rem;">
                    <button type="button" class="trailing-btn" data-toggle-pwd="reg-password">
                      <span class="material-symbols-outlined" style="font-size:19px;">visibility</span>
                    </button>
                  </div>
                  <div class="row-between text-xs text-muted" style="margin-top:4px;">
                    <span>Password strength</span>
                    <span id="pwd-strength-label" class="fw-600">Enter password</span>
                  </div>
                  <div class="strength-bars">
                    <div class="strength-bar" id="sbar-1"></div>
                    <div class="strength-bar" id="sbar-2"></div>
                    <div class="strength-bar" id="sbar-3"></div>
                    <div class="strength-bar" id="sbar-4"></div>
                  </div>
                </div>

                <div class="field">
                  <label for="reg-confirm">Confirm Password <span class="req">*</span></label>
                  <div class="input-with-icon">
                    <span class="material-symbols-outlined leading-icon">verified</span>
                    <input id="reg-confirm" name="confirmPassword" type="password" class="input" required minlength="6" maxlength="100"
                           placeholder="Re-enter password" style="padding-right:2.5rem;">
                    <button type="button" class="trailing-btn" data-toggle-pwd="reg-confirm">
                      <span class="material-symbols-outlined" style="font-size:19px;">visibility</span>
                    </button>
                  </div>
                  <span id="pwd-match-label" class="hint">Must match your account password.</span>
                </div>
              </div>

              <label class="row-gap text-sm" style="align-items:flex-start;cursor:pointer;">
                <input type="checkbox" required style="margin-top:3px;accent-color:var(--primary);">
                <span class="text-muted">
                  I agree to Meocare Veterinary Clinic's clinical record privacy and appointment policies.
                </span>
              </label>

              <button type="submit" class="btn btn-primary btn-lg btn-block" id="reg-submit-btn">
                <span>Create Pet Parent Account</span>
                <span class="material-symbols-outlined" style="font-size:19px;">arrow_forward</span>
              </button>
            </form>

            <div class="row-between text-sm">
              <span class="text-muted">Already registered with Meocare?</span>
              <a href="#login" id="back-to-login" class="fw-600 row-gap" style="gap:4px;">
                Sign In to Portal
                <span class="material-symbols-outlined" style="font-size:17px;">login</span>
              </a>
            </div>
          </section>
        </div>
      </main>

      <footer class="auth-footer">
        Meocare Veterinary Clinic • Dedicated Compassionate Animal Care
      </footer>
    </div>
  `;

  root.querySelectorAll("[data-toggle-pwd]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const input = document.getElementById(btn.getAttribute("data-toggle-pwd"));
      const icon = btn.querySelector(".material-symbols-outlined");
      if (!input) return;
      input.type = input.type === "password" ? "text" : "password";
      icon.textContent = input.type === "password" ? "visibility" : "visibility_off";
    });
  });

  const pwdInput = document.getElementById("reg-password");
  const confirmInput = document.getElementById("reg-confirm");
  const strengthLabel = document.getElementById("pwd-strength-label");
  const matchLabel = document.getElementById("pwd-match-label");

  function updateStrength() {
    const val = pwdInput.value || "";
    const bars = [1, 2, 3, 4].map((i) => document.getElementById(`sbar-${i}`));
    bars.forEach((b) => { b.className = "strength-bar"; });
    if (!val) {
      strengthLabel.textContent = "Enter password";
      return;
    }
    let score = 0;
    if (val.length >= 6) score++;
    if (val.length >= 10 || /[A-Z]/.test(val)) score++;
    if (/[0-9]/.test(val)) score++;
    if (/[^A-Za-z0-9]/.test(val)) score++;

    const styles = ["s-weak", "s-fair", "s-good", "s-strong"];
    const labels = ["Weak", "Fair", "Good", "Strong"];
    const idx = Math.max(0, Math.min(3, score - 1));
    for (let i = 0; i <= idx; i++) bars[i].classList.add(styles[idx]);
    strengthLabel.textContent = labels[idx];
    updateMatch();
  }

  function updateMatch() {
    if (!confirmInput.value) {
      matchLabel.textContent = "Must match your account password.";
      matchLabel.className = "hint";
      return;
    }
    if (pwdInput.value === confirmInput.value) {
      matchLabel.textContent = "Passwords match.";
      matchLabel.className = "hint text-secondary fw-600";
    } else {
      matchLabel.textContent = "Passwords do not match.";
      matchLabel.className = "hint text-error fw-600";
    }
  }

  pwdInput?.addEventListener("input", updateStrength);
  confirmInput?.addEventListener("input", updateMatch);

  document.getElementById("back-to-login")?.addEventListener("click", (e) => {
    e.preventDefault();
    navigate("login");
  });

  const form = document.getElementById("register-form");
  const errorBox = document.getElementById("register-error-box");
  const submitBtn = document.getElementById("reg-submit-btn");

  form?.addEventListener("submit", async (e) => {
    e.preventDefault();
    errorBox.hidden = true;
    const data = new FormData(form);
    const fullName = String(data.get("fullName") || "").trim();
    const email = String(data.get("email") || "").trim();
    const phone = String(data.get("phone") || "").trim();
    const password = String(data.get("password") || "");
    const confirmPassword = String(data.get("confirmPassword") || "");

    if (password !== confirmPassword) {
      errorBox.innerHTML = `<span class="material-symbols-outlined" style="font-size:19px;">error</span><span>Passwords do not match.</span>`;
      errorBox.hidden = false;
      return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="spinner spinner-sm"></span><span>Creating account...</span>`;

    try {
      await api.register({ fullName, email, phone: phone || null, password });
      showToast("Account created! Please sign in with your new credentials.", "success");
      navigate("login", {
        noticeMessage: "Your pet parent account is ready. Sign in below to access your Pet Hub.",
        prefillEmail: email
      });
    } catch (err) {
      errorBox.innerHTML = `<span class="material-symbols-outlined" style="font-size:19px;">error</span><span>${esc(err.message)}</span>`;
      errorBox.hidden = false;
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = `<span>Create Pet Parent Account</span><span class="material-symbols-outlined" style="font-size:19px;">arrow_forward</span>`;
    }
  });
}

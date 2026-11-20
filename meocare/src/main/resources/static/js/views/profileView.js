import { api } from "../api.js";
import {
  esc,
  formatDate,
  getInitials,
  renderRoleBadge,
  renderUserStatusBadge,
  safeImageUrl,
  showToast
} from "../utils.js";

export async function renderProfileView(container, ctx) {
  container.innerHTML = `<div class="card"><div class="empty-state"><div class="spinner"></div><p>Loading profile...</p></div></div>`;

  let profile;
  try {
    profile = await api.getMe();
    ctx.setUser(profile);
  } catch (err) {
    container.innerHTML = `<div class="callout callout-error">${esc(err.message)}</div>`;
    return;
  }

  const safeAvatar = safeImageUrl(profile.avatarUrl);

  container.innerHTML = `
    <div class="page-header">
      <div>
        <span class="eyebrow">Account Settings</span>
        <h1>My Profile</h1>
        <p>Manage your personal contact information, profile avatar, and account security password.</p>
      </div>
    </div>

    <div class="split-7-5">
      <section class="card stack-md">
        <div class="card-header" style="margin-bottom:0;">
          <div>
            <h2 class="card-title">
              <span class="material-symbols-outlined text-primary">account_circle</span>
              Profile Information
            </h2>
            <p class="card-subtitle">Email address, role, and status are managed by clinic administration.</p>
          </div>
        </div>

        <form id="profile-info-form" class="stack-md">
          <div class="row-gap" style="gap:1rem;">
            <div class="avatar avatar-lg" id="profile-avatar-preview">
              ${safeAvatar ? `<img src="${esc(safeAvatar)}" alt="Profile avatar">` : esc(getInitials(profile.fullName))}
            </div>
            <div class="stack-sm" style="flex:1;">
              <div class="row-gap">
                <label class="btn btn-secondary btn-sm" style="cursor:pointer;">
                  <span class="material-symbols-outlined" style="font-size:16px;">upload</span>
                  Upload Avatar
                  <input type="file" id="profile-avatar-file" accept="image/jpeg,image/png,image/webp,image/gif" hidden>
                </label>
                <button type="button" class="btn btn-ghost btn-sm" id="profile-clear-avatar">Remove</button>
              </div>
              <span class="text-xs text-muted">Upload a local image or paste an image URL below.</span>
            </div>
          </div>

          <div class="form-grid">
            <div class="field">
              <label for="prof-full-name">Full Name <span class="req">*</span></label>
              <input id="prof-full-name" name="fullName" class="input" required maxlength="100" value="${esc(profile.fullName)}">
            </div>

            <div class="field">
              <label for="prof-phone">Phone Number</label>
              <input id="prof-phone" name="phone" class="input" maxlength="20" value="${esc(profile.phone || "")}" placeholder="+1 (555) 000-0000">
            </div>

            <div class="field col-span-full">
              <label for="prof-avatar-url">Avatar Image URL</label>
              <input id="prof-avatar-url" name="avatarUrl" class="input" maxlength="500" value="${esc(profile.avatarUrl || "")}" placeholder="/uploads/... or https://...">
            </div>
          </div>

          <div class="row-between">
            <span class="text-xs text-muted">Member since <strong class="numeric">${esc(formatDate(profile.createdAt))}</strong></span>
            <button type="submit" class="btn btn-primary" id="save-profile-btn">
              <span class="material-symbols-outlined" style="font-size:18px;">save</span>
              Save Changes
            </button>
          </div>
        </form>
      </section>

      <div class="stack-md">
        <section class="card stack-sm">
          <h2 class="card-title">
            <span class="material-symbols-outlined text-primary">badge</span>
            Account Summary
          </h2>
          <div class="card-soft stack-sm">
            <div class="row-between">
              <span class="text-sm text-muted">Email</span>
              <span class="text-sm fw-600">${esc(profile.email)}</span>
            </div>
            <div class="row-between">
              <span class="text-sm text-muted">Role</span>
              <span>${renderRoleBadge(profile.role)}</span>
            </div>
            <div class="row-between">
              <span class="text-sm text-muted">Status</span>
              <span>${renderUserStatusBadge(profile.status)}</span>
            </div>
          </div>
        </section>

        <section class="card stack-md">
          <h2 class="card-title">
            <span class="material-symbols-outlined text-primary">lock_reset</span>
            Change Password
          </h2>
          <form id="profile-password-form" class="stack-sm">
            <div class="field">
              <label for="prof-cur-pwd">Current Password <span class="req">*</span></label>
              <input id="prof-cur-pwd" name="currentPassword" type="password" class="input" required>
            </div>
            <div class="field">
              <label for="prof-new-pwd">New Password <span class="req">*</span></label>
              <input id="prof-new-pwd" name="newPassword" type="password" class="input" required minlength="6" maxlength="100">
            </div>
            <div class="field">
              <label for="prof-confirm-pwd">Confirm New Password <span class="req">*</span></label>
              <input id="prof-confirm-pwd" name="confirmNewPassword" type="password" class="input" required minlength="6" maxlength="100">
            </div>
            <button type="submit" class="btn btn-secondary" id="change-pwd-btn">
              <span class="material-symbols-outlined" style="font-size:18px;">key</span>
              Update Password
            </button>
          </form>
        </section>
      </div>
    </div>
  `;

  const avatarInput = document.getElementById("prof-avatar-url");
  const avatarPreview = document.getElementById("profile-avatar-preview");

  function refreshAvatarPreview(url) {
    const safe = safeImageUrl(url);
    avatarPreview.innerHTML = safe
      ? `<img src="${esc(safe)}" alt="Profile avatar">`
      : esc(getInitials(document.getElementById("prof-full-name")?.value || profile.fullName));
  }

  avatarInput?.addEventListener("input", () => refreshAvatarPreview(avatarInput.value));
  document.getElementById("profile-clear-avatar")?.addEventListener("click", () => {
    avatarInput.value = "";
    refreshAvatarPreview("");
  });

  document.getElementById("profile-avatar-file")?.addEventListener("change", async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const uploaded = await api.uploadImage(file);
      avatarInput.value = uploaded.url;
      refreshAvatarPreview(uploaded.url);
      showToast("Avatar image uploaded. Click Save Changes to apply.", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  document.getElementById("profile-info-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const btn = document.getElementById("save-profile-btn");
    const fd = new FormData(e.target);
    btn.disabled = true;
    try {
      const updated = await api.updateMe({
        fullName: String(fd.get("fullName") || "").trim(),
        phone: String(fd.get("phone") || "").trim(),
        avatarUrl: String(fd.get("avatarUrl") || "").trim()
      });
      ctx.setUser(updated);
      ctx.refreshShellUser();
      showToast("Profile updated successfully.", "success");
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      btn.disabled = false;
    }
  });

  document.getElementById("profile-password-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const btn = document.getElementById("change-pwd-btn");
    const fd = new FormData(e.target);
    const currentPassword = String(fd.get("currentPassword") || "");
    const newPassword = String(fd.get("newPassword") || "");
    const confirm = String(fd.get("confirmNewPassword") || "");
    if (newPassword !== confirm) {
      showToast("New passwords do not match.", "error");
      return;
    }
    btn.disabled = true;
    try {
      await api.updateMe({ currentPassword, newPassword });
      e.target.reset();
      showToast("Password updated successfully.", "success");
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      btn.disabled = false;
    }
  });
}

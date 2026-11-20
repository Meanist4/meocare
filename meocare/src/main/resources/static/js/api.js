const API_BASE = "/api";
const TOKEN_KEY = "meocare_jwt";

let unauthorizedHandler = null;

export class ApiError extends Error {
  constructor(message, status, fieldErrors = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn;
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

function buildQuery(params = {}) {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      qs.set(key, String(value));
    }
  });
  const str = qs.toString();
  return str ? `?${str}` : "";
}

export async function apiRequest(path, options = {}) {
  const token = getToken();
  const isFormData = options.body instanceof FormData;
  const headers = {
    ...(token && !options.skipAuth ? { Authorization: `Bearer ${token}` } : {}),
    ...(!isFormData && options.body ? { "Content-Type": "application/json" } : {}),
    ...(options.headers || {})
  };

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers
    });
  } catch {
    throw new ApiError("Unable to reach the Meocare server. Please check your connection.", 0);
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    let fieldErrors = null;
    const raw = await response.text();
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        message = parsed.message || parsed.detail || parsed.error || message;
        if (parsed.fieldErrors && typeof parsed.fieldErrors === "object" && !Array.isArray(parsed.fieldErrors)) {
          fieldErrors = parsed.fieldErrors;
          const details = Object.values(parsed.fieldErrors).filter(Boolean).join("; ");
          if (details) {
            message = details;
          }
        }
      } catch {
        message = raw;
      }
    }

    if (response.status === 401 && !options.skipUnauthorizedHook && typeof unauthorizedHandler === "function") {
      clearToken();
      unauthorizedHandler(message);
    }

    throw new ApiError(message, response.status, fieldErrors);
  }

  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  // Auth & Profile
  login(payload) {
    return apiRequest("/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
      skipAuth: true,
      skipUnauthorizedHook: true
    });
  },
  register(payload) {
    return apiRequest("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
      skipAuth: true,
      skipUnauthorizedHook: true
    });
  },
  getMe() {
    return apiRequest("/auth/me");
  },
  updateMe(payload) {
    return apiRequest("/auth/me", {
      method: "PUT",
      body: JSON.stringify(payload)
    });
  },

  // Users & Staff
  getUsers(params = {}) {
    return apiRequest(`/users${buildQuery(params)}`);
  },
  getActiveVeterinarians() {
    return apiRequest("/users/veterinarians");
  },
  createStaffUser(payload) {
    return apiRequest("/users", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },
  updateUserStatus(id, status) {
    return apiRequest(`/users/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status })
    });
  },

  // Pets & Pet Images
  getPets(params = {}) {
    return apiRequest(`/pets${buildQuery(params)}`);
  },
  getPetById(id) {
    return apiRequest(`/pets/${id}`);
  },
  createPet(payload, ownerId = null) {
    return apiRequest(`/pets${buildQuery({ ownerId })}`, {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },
  updatePet(id, payload) {
    return apiRequest(`/pets/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
  },
  deletePet(id) {
    return apiRequest(`/pets/${id}`, { method: "DELETE" });
  },
  addPetImage(petId, imageUrl) {
    return apiRequest(`/pets/${petId}/images`, {
      method: "POST",
      body: JSON.stringify({ imageUrl })
    });
  },
  deletePetImage(petId, imageId) {
    return apiRequest(`/pets/${petId}/images/${imageId}`, { method: "DELETE" });
  },

  // Appointments & Images
  getAppointments(params = {}) {
    return apiRequest(`/appointments${buildQuery(params)}`);
  },
  getAppointmentById(id) {
    return apiRequest(`/appointments/${id}`);
  },
  createAppointment(payload) {
    return apiRequest("/appointments", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },
  assignVeterinarian(id, veterinarianId) {
    return apiRequest(`/appointments/${id}/assign`, {
      method: "PATCH",
      body: JSON.stringify({ veterinarianId })
    });
  },
  confirmAppointment(id, payload = null) {
    return apiRequest(`/appointments/${id}/confirm`, {
      method: "PATCH",
      ...(payload ? { body: JSON.stringify(payload) } : {})
    });
  },
  rejectAppointment(id) {
    return apiRequest(`/appointments/${id}/reject`, { method: "PATCH" });
  },
  cancelAppointment(id) {
    return apiRequest(`/appointments/${id}/cancel`, { method: "PATCH" });
  },
  updateAppointmentStatus(id, status) {
    return apiRequest(`/appointments/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status })
    });
  },
  addAppointmentImage(appointmentId, imageUrl) {
    return apiRequest(`/appointments/${appointmentId}/images`, {
      method: "POST",
      body: JSON.stringify({ imageUrl })
    });
  },
  deleteAppointmentImage(appointmentId, imageId) {
    return apiRequest(`/appointments/${appointmentId}/images/${imageId}`, {
      method: "DELETE"
    });
  },

  // Appointment Proposals (Negotiation History)
  getProposals(appointmentId) {
    return apiRequest(`/appointments/${appointmentId}/proposals`);
  },
  createProposal(appointmentId, payload) {
    return apiRequest(`/appointments/${appointmentId}/proposals`, {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },
  acceptProposal(appointmentId, proposalId) {
    return apiRequest(`/appointments/${appointmentId}/proposals/${proposalId}/accept`, {
      method: "PATCH"
    });
  },
  declineProposal(appointmentId, proposalId) {
    return apiRequest(`/appointments/${appointmentId}/proposals/${proposalId}/decline`, {
      method: "PATCH"
    });
  },

  // Medical Records
  getMedicalRecords(params = {}) {
    return apiRequest(`/medical-records${buildQuery(params)}`);
  },
  getMedicalRecordById(id) {
    return apiRequest(`/medical-records/${id}`);
  },
  getMedicalRecordByAppointmentId(appointmentId) {
    return apiRequest(`/medical-records/by-appointment/${appointmentId}`);
  },
  createMedicalRecord(payload) {
    return apiRequest("/medical-records", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },
  updateMedicalRecord(id, payload) {
    return apiRequest(`/medical-records/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
  },
  addMedicalRecordImages(id, imageUrls) {
    return apiRequest(`/medical-records/${id}/images`, {
      method: "POST",
      body: JSON.stringify({ imageUrls })
    });
  },
  deleteMedicalRecordImage(id, imageId) {
    return apiRequest(`/medical-records/${id}/images/${imageId}`, { method: "DELETE" });
  },

  // Daily Health Timeline
  getHealthTimeline(params = {}) {
    return apiRequest(`/health-updates${buildQuery(params)}`);
  },
  getHealthUpdatesByRecord(medicalRecordId) {
    return apiRequest(`/medical-records/${medicalRecordId}/health-updates`);
  },
  createHealthUpdate(medicalRecordId, payload) {
    return apiRequest(`/medical-records/${medicalRecordId}/health-updates`, {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },
  addHealthUpdateImages(id, imageUrls) {
    return apiRequest(`/health-updates/${id}/images`, {
      method: "POST",
      body: JSON.stringify({ imageUrls })
    });
  },
  deleteHealthUpdateImage(id, imageId) {
    return apiRequest(`/health-updates/${id}/images/${imageId}`, { method: "DELETE" });
  },

  // Local Image Upload
  uploadImage(file) {
    const formData = new FormData();
    formData.append("file", file);
    return apiRequest("/uploads/images", {
      method: "POST",
      body: formData
    });
  }
};

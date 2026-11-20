const API_BASE = "/api";
const state = {
  pets: [],
  appointments: [],
  records: [],
  toastTimer: null
};

const elements = {
  appointmentForm: document.querySelector("#appointment-form"),
  petSelect: document.querySelector("#pet-select"),
  vetSelect: document.querySelector("#vet-select"),
  startInput: document.querySelector("#requested-start"),
  endInput: document.querySelector("#requested-end"),
  appointmentFilter: document.querySelector("#appointment-filter"),
  appointmentList: document.querySelector("#appointment-list"),
  recordPetSelect: document.querySelector("#records-pet-select"),
  recordList: document.querySelector("#record-list"),
  toast: document.querySelector("#toast"),
  total: document.querySelector("#stat-total"),
  pending: document.querySelector("#stat-pending"),
  confirmed: document.querySelector("#stat-confirmed")
};

const STATUS_LABELS = {
  PENDING: "Chờ xác nhận",
  RESCHEDULE_PROPOSED: "Đề xuất đổi lịch",
  CONFIRMED: "Đã xác nhận",
  REJECTED: "Đã từ chối",
  CANCELLED: "Đã hủy",
  COMPLETED: "Đã hoàn thành"
};

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...options.headers
      }
    });
  } catch {
    throw new Error("Không thể kết nối máy chủ Meocare. Hãy kiểm tra ứng dụng backend trên cổng 8080.");
  }

  if (!response.ok) {
    const body = await response.text();
    let message = body;
    try {
      const error = JSON.parse(body);
      message = error.message || error.detail || error.title || body;
      if (Array.isArray(error.fieldErrors) && error.fieldErrors.length) {
        message = error.fieldErrors.map(item => item.message || item.defaultMessage).filter(Boolean).join("; ") || message;
      }
    } catch {
      // The response may be plain text rather than the API's JSON error format.
    }
    throw new Error(message || `Máy chủ trả về lỗi ${response.status}.`);
  }

  if (response.status === 204) return null;
  return response.json();
}

function showToast(message, isError = false) {
  window.clearTimeout(state.toastTimer);
  elements.toast.textContent = message;
  elements.toast.classList.toggle("is-error", isError);
  elements.toast.hidden = false;
  state.toastTimer = window.setTimeout(() => { elements.toast.hidden = true; }, 5000);
}

function localDateTimeValue(date) {
  const pad = value => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function setDefaultAppointmentTimes() {
  const now = new Date();
  now.setMinutes(now.getMinutes() + 60 - now.getMinutes() % 30, 0, 0);
  const end = new Date(now.getTime() + 30 * 60 * 1000);
  elements.startInput.min = localDateTimeValue(now);
  elements.endInput.min = localDateTimeValue(now);
  elements.startInput.value = localDateTimeValue(now);
  elements.endInput.value = localDateTimeValue(end);
}

function displayDate(value, includeTime = true) {
  if (!value) return "Chưa có thời gian";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", includeTime
    ? { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" }
    : { day: "2-digit", month: "long", year: "numeric" }).format(date);
}

function appendOption(select, value, label) {
  const option = document.createElement("option");
  option.value = value;
  option.textContent = label;
  select.append(option);
}

function fillPetOptions() {
  const selects = [elements.petSelect, elements.appointmentFilter, elements.recordPetSelect];
  for (const select of selects) {
    const initialLabel = select === elements.petSelect ? "Chọn bé cưng" : select === elements.recordPetSelect ? "Chọn bé cưng" : "Tất cả bé";
    select.replaceChildren();
    appendOption(select, "", initialLabel);
    for (const pet of state.pets) appendOption(select, pet.id, pet.name || `Bé #${pet.id}`);
  }
}

function renderStats() {
  elements.total.textContent = String(state.appointments.length);
  elements.pending.textContent = String(state.appointments.filter(item => item.status === "PENDING").length);
  elements.confirmed.textContent = String(state.appointments.filter(item => item.status === "CONFIRMED").length);
}

function createAppointmentCard(appointment) {
  const card = document.createElement("article");
  card.className = "appointment-card";

  const avatar = document.createElement("div");
  avatar.className = "pet-avatar";
  avatar.setAttribute("aria-hidden", "true");
  avatar.textContent = "🐾";

  const detail = document.createElement("div");
  detail.className = "appointment-detail";
  const name = document.createElement("p");
  name.className = "appointment-name";
  name.textContent = appointment.petName || `Bé #${appointment.petId}`;
  const meta = document.createElement("p");
  meta.className = "appointment-meta";
  const vet = document.createElement("span");
  vet.textContent = appointment.veterinarianName || "Phòng khám sắp xếp bác sĩ";
  const time = document.createElement("span");
  time.textContent = displayDate(appointment.confirmedStart || appointment.requestedStart);
  meta.append(vet, time);
  detail.append(name, meta);

  const side = document.createElement("div");
  side.className = "appointment-side";
  const status = document.createElement("span");
  status.className = `status-pill status-${String(appointment.status || "").toLowerCase()}`;
  status.textContent = STATUS_LABELS[appointment.status] || appointment.status || "Chưa rõ";
  const date = document.createElement("span");
  date.className = "appointment-date";
  date.textContent = appointment.status === "CONFIRMED" ? "Lịch đã chốt" : "Ngày mong muốn";
  side.append(status, date);
  card.append(avatar, detail, side);
  return card;
}

function renderAppointments() {
  renderStats();
  const petId = elements.appointmentFilter.value;
  const appointments = state.appointments
    .filter(item => !petId || String(item.petId) === petId)
    .slice()
    .sort((a, b) => new Date(a.confirmedStart || a.requestedStart) - new Date(b.confirmedStart || b.requestedStart));
  elements.appointmentList.replaceChildren();

  if (!appointments.length) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.innerHTML = '<span class="empty-icon" aria-hidden="true">✿</span>Chưa có lịch khám phù hợp.<br>Hãy đặt lịch cho bé ở biểu mẫu bên cạnh nhé.';
    elements.appointmentList.append(empty);
    return;
  }
  for (const appointment of appointments) elements.appointmentList.append(createAppointmentCard(appointment));
}

function appendRecordSection(card, label, value) {
  if (!value) return;
  const section = document.createElement("section");
  section.className = "record-section";
  const heading = document.createElement("h3");
  heading.textContent = label;
  const text = document.createElement("p");
  text.textContent = value;
  section.append(heading, text);
  card.append(section);
}

function createRecordCard(record) {
  const card = document.createElement("article");
  card.className = "record-card";
  const top = document.createElement("div");
  top.className = "record-card-top";
  const titleGroup = document.createElement("div");
  const name = document.createElement("p");
  name.className = "record-pet-name";
  name.textContent = record.petName || `Hồ sơ #${record.id}`;
  const date = document.createElement("span");
  date.className = "record-date";
  date.textContent = displayDate(record.createdAt, false);
  titleGroup.append(name, date);
  top.append(titleGroup);

  if (record.weight !== null && record.weight !== undefined) {
    const weight = document.createElement("span");
    weight.className = "record-weight";
    weight.textContent = `${record.weight} kg`;
    top.append(weight);
  }
  card.append(top);
  appendRecordSection(card, "Chẩn đoán", record.diagnosis);
  appendRecordSection(card, "Khám lâm sàng", record.examination);
  appendRecordSection(card, "Điều trị", record.treatment);
  appendRecordSection(card, "Lời dặn", record.advice);
  appendRecordSection(card, "Ghi chú", record.notes);

  const vitals = [];
  if (record.temperature !== null && record.temperature !== undefined) vitals.push(["Nhiệt độ", `${record.temperature}°C`]);
  if (record.veterinarianName) vitals.push(["Bác sĩ", record.veterinarianName]);
  if (vitals.length) {
    const row = document.createElement("div");
    row.className = "record-vitals";
    for (const [label, value] of vitals) {
      const item = document.createElement("span");
      item.append(`${label}: `);
      const strong = document.createElement("strong");
      strong.textContent = value;
      item.append(strong);
      row.append(item);
    }
    card.append(row);
  }
  return card;
}

function renderRecords() {
  elements.recordList.replaceChildren();
  if (!elements.recordPetSelect.value) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.textContent = "Chọn bé cưng để xem hồ sơ y tế.";
    elements.recordList.append(empty);
    return;
  }
  if (!state.records.length) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.innerHTML = '<span class="empty-icon" aria-hidden="true">♡</span>Bé chưa có hồ sơ thăm khám nào được lưu.';
    elements.recordList.append(empty);
    return;
  }
  for (const record of state.records) elements.recordList.append(createRecordCard(record));
}

async function loadAppointments() {
  state.appointments = await request("/appointments");
  renderAppointments();
}

async function loadRecords(petId) {
  if (!petId) {
    state.records = [];
    renderRecords();
    return;
  }
  elements.recordList.innerHTML = '<div class="loading-state"><span class="loader"></span> Đang tải hồ sơ...</div>';
  try {
    state.records = await request(`/medical-records?petId=${encodeURIComponent(petId)}`);
    renderRecords();
  } catch (error) {
    elements.recordList.replaceChildren();
    const message = document.createElement("div");
    message.className = "empty-state";
    message.textContent = `Không tải được hồ sơ: ${error.message}`;
    elements.recordList.append(message);
    showToast(`Không tải được hồ sơ y tế: ${error.message}`, true);
  }
}

async function loadInitialData() {
  const results = await Promise.allSettled([
    request("/pets"),
    request("/users"),
    request("/appointments")
  ]);
  const labels = ["danh sách thú cưng", "danh sách bác sĩ", "lịch khám"];

  results.forEach((result, index) => {
    if (result.status === "rejected") {
      showToast(`Không tải được ${labels[index]}: ${result.reason.message}`, true);
      return;
    }
    if (index === 0) state.pets = result.value;
    if (index === 1) {
      const vets = result.value.filter(user => user.role === "VETERINARIAN" && user.status === "ACTIVE");
      elements.vetSelect.replaceChildren();
      appendOption(elements.vetSelect, "", "Để phòng khám sắp xếp");
      for (const vet of vets) appendOption(elements.vetSelect, vet.id, vet.fullName || `Bác sĩ #${vet.id}`);
    }
    if (index === 2) state.appointments = result.value;
  });

  fillPetOptions();
  renderAppointments();
  setDefaultAppointmentTimes();
  if (state.pets.length) {
    elements.recordPetSelect.value = String(state.pets[0].id);
    await loadRecords(elements.recordPetSelect.value);
  } else {
    renderRecords();
  }
  if (!state.pets.length && results[0].status === "fulfilled") {
    showToast("Chưa có thú cưng trong hệ thống; cần thêm hồ sơ thú cưng trước khi đặt lịch.", true);
  }
}

elements.startInput.addEventListener("change", () => {
  elements.endInput.min = elements.startInput.value || localDateTimeValue(new Date());
  if (elements.endInput.value && elements.endInput.value <= elements.startInput.value) {
    const end = new Date(elements.startInput.value);
    end.setMinutes(end.getMinutes() + 30);
    elements.endInput.value = localDateTimeValue(end);
  }
});

elements.appointmentFilter.addEventListener("change", renderAppointments);
elements.recordPetSelect.addEventListener("change", () => {
  loadRecords(elements.recordPetSelect.value);
});

elements.appointmentForm.addEventListener("submit", async event => {
  event.preventDefault();
  const form = new FormData(elements.appointmentForm);
  const start = form.get("requestedStart");
  const end = form.get("requestedEnd");
  if (new Date(start) <= new Date()) {
    showToast("Thời gian bắt đầu phải ở trong tương lai.", true);
    return;
  }
  if (new Date(end) <= new Date(start)) {
    showToast("Thời gian kết thúc phải sau thời gian bắt đầu.", true);
    return;
  }

  const button = elements.appointmentForm.querySelector('button[type="submit"]');
  button.disabled = true;
  try {
    await request("/appointments", {
      method: "POST",
      body: JSON.stringify({
        petId: Number(form.get("petId")),
        veterinarianId: form.get("veterinarianId") ? Number(form.get("veterinarianId")) : null,
        requestedStart: start,
        requestedEnd: end,
        symptoms: form.get("symptoms").trim(),
        imageUrls: []
      })
    });
    showToast("Đã gửi yêu cầu đặt lịch. Phòng khám sẽ xác nhận với bạn.");
    elements.appointmentForm.reset();
    setDefaultAppointmentTimes();
    elements.petSelect.value = String(state.pets[0]?.id || "");
    await loadAppointments();
  } catch (error) {
    showToast(`Không đặt được lịch: ${error.message}`, true);
  } finally {
    button.disabled = false;
  }
});

document.querySelectorAll("[data-view]").forEach(button => {
  button.addEventListener("click", () => {
    const view = button.dataset.view;
    document.querySelectorAll("[data-view]").forEach(item => item.classList.toggle("is-active", item === button));
    document.querySelectorAll(".view").forEach(section => {
      const visible = section.id === `${view}-view`;
      section.classList.toggle("is-visible", visible);
      section.hidden = !visible;
    });
    document.querySelector("#breadcrumb-current").textContent = view === "records" ? "Hồ sơ y tế" : "Lịch khám";
  });
});

setDefaultAppointmentTimes();
loadInitialData().catch(error => showToast(`Không thể khởi tạo giao diện: ${error.message}`, true));

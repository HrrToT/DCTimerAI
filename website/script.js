const dialog = document.querySelector(".image-dialog");
const dialogImage = dialog?.querySelector("img");
const closeButton = dialog?.querySelector(".image-dialog-close");
const zoomState = {
  scale: 1,
  x: 0,
  y: 0,
  startScale: 1,
  startX: 0,
  startY: 0,
  startDistance: 0,
  startMidX: 0,
  startMidY: 0,
  pointers: new Map(),
};

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

const applyTransform = () => {
  if (!dialogImage) return;
  dialogImage.style.transform = `translate3d(${zoomState.x}px, ${zoomState.y}px, 0) scale(${zoomState.scale})`;
};

const resetZoom = () => {
  zoomState.scale = 1;
  zoomState.x = 0;
  zoomState.y = 0;
  zoomState.startScale = 1;
  zoomState.startX = 0;
  zoomState.startY = 0;
  zoomState.startDistance = 0;
  zoomState.startMidX = 0;
  zoomState.startMidY = 0;
  zoomState.pointers.clear();
  applyTransform();
};

const getDistance = (a, b) => Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);

const getMidpoint = (a, b) => ({
  x: (a.clientX + b.clientX) / 2,
  y: (a.clientY + b.clientY) / 2,
});

const getPointerPair = () => [...zoomState.pointers.values()].slice(0, 2);

document.querySelectorAll(".image-zoom-trigger").forEach((button) => {
  button.addEventListener("click", () => {
    if (!dialog || !dialogImage) return;

    dialogImage.src = button.dataset.full || "";
    dialogImage.alt = button.dataset.alt || "";
    resetZoom();
    dialog.showModal();
  });
});

closeButton?.addEventListener("click", () => {
  dialog?.close();
});

dialog?.addEventListener("click", (event) => {
  if (event.target === dialog) {
    dialog.close();
  }
});

dialogImage?.addEventListener("pointerdown", (event) => {
  zoomState.pointers.set(event.pointerId, event);
  dialogImage.setPointerCapture(event.pointerId);

  const pointers = getPointerPair();
  if (pointers.length === 1) {
    zoomState.startX = pointers[0].clientX - zoomState.x;
    zoomState.startY = pointers[0].clientY - zoomState.y;
  }

  if (pointers.length === 2) {
    zoomState.startDistance = getDistance(pointers[0], pointers[1]);
    zoomState.startScale = zoomState.scale;
    const midpoint = getMidpoint(pointers[0], pointers[1]);
    zoomState.startMidX = midpoint.x - zoomState.x;
    zoomState.startMidY = midpoint.y - zoomState.y;
  }
});

dialogImage?.addEventListener("pointermove", (event) => {
  if (!zoomState.pointers.has(event.pointerId)) return;
  zoomState.pointers.set(event.pointerId, event);

  const pointers = getPointerPair();
  if (pointers.length === 2) {
    const distance = getDistance(pointers[0], pointers[1]);
    const nextScale = clamp((distance / zoomState.startDistance) * zoomState.startScale, 1, 4);
    const midpoint = getMidpoint(pointers[0], pointers[1]);
    zoomState.scale = nextScale;
    zoomState.x = midpoint.x - zoomState.startMidX;
    zoomState.y = midpoint.y - zoomState.startMidY;
    applyTransform();
    return;
  }

  if (pointers.length === 1 && zoomState.scale > 1) {
    zoomState.x = pointers[0].clientX - zoomState.startX;
    zoomState.y = pointers[0].clientY - zoomState.startY;
    applyTransform();
  }
});

const endPointer = (event) => {
  zoomState.pointers.delete(event.pointerId);

  if (zoomState.scale <= 1.02) {
    zoomState.scale = 1;
    zoomState.x = 0;
    zoomState.y = 0;
    applyTransform();
  }

  const pointers = getPointerPair();
  if (pointers.length === 1) {
    zoomState.startX = pointers[0].clientX - zoomState.x;
    zoomState.startY = pointers[0].clientY - zoomState.y;
  }
};

dialogImage?.addEventListener("pointerup", endPointer);
dialogImage?.addEventListener("pointercancel", endPointer);
dialogImage?.addEventListener("lostpointercapture", endPointer);

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && dialog?.open) {
    dialog.close();
  }
});

dialog?.addEventListener("close", resetZoom);

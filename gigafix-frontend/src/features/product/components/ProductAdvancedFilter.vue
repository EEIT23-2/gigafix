<script setup>
import { reactive } from "vue";

const props = defineProps({
  modelName: { type: String, default: "" },
  color: { type: String, default: "" },
  storage: { type: String, default: "" },
});

const emit = defineEmits(["apply", "reset", "close"]);

// 使用草稿避免使用者尚未按「套用」時就改變列表條件。
const draft = reactive({
  modelName: props.modelName,
  color: props.color,
  storage: props.storage,
});

const colorOptions = ["黑", "白", "藍", "綠", "紅", "粉紅", "紫", "金", "銀"];
const storageOptions = ["16G", "32G", "64G", "128G", "256G", "512G", "1TB"];

function toggleOption(field, value) {
  draft[field] = draft[field] === value ? "" : value;
}

function applyFilters() {
  emit("apply", {
    modelName: draft.modelName.trim(),
    color: draft.color.trim(),
    storage: draft.storage.trim(),
  });
}

function resetFilters() {
  draft.modelName = "";
  draft.color = "";
  draft.storage = "";
  emit("reset");
}
</script>

<template>
  <Teleport to="body">
    <div
      class="filter-layer position-fixed top-0 start-0 end-0 bottom-0"
      @click.self="emit('close')"
    >
      <aside
        class="filter-drawer position-absolute top-0 end-0 bottom-0 bg-white shadow-lg"
        aria-label="進階商品篩選"
      >
        <header
          class="d-flex align-items-center justify-content-between border-bottom px-4 py-3"
        >
          <div>
            <h2 class="h5 fw-bold mb-1">進階篩選</h2>
            <p class="small text-secondary mb-0">
              依型號、顏色與容量縮小查詢範圍
            </p>
          </div>
          <button
            class="btn-close"
            type="button"
            aria-label="關閉篩選"
            @click="emit('close')"
          ></button>
        </header>

        <div class="drawer-body p-4">
          <section class="mb-4">
            <label class="form-label fw-semibold" for="advanced-model-name"
              >型號</label
            >
            <input
              id="advanced-model-name"
              v-model.trim="draft.modelName"
              class="form-control"
              type="text"
              placeholder="例如：iPhone 15 Pro、iPad Air"
              @keyup.enter="applyFilters"
            />
            <div class="form-text">後端會在商品名稱中比對此文字。</div>
          </section>

          <section class="mb-4">
            <div class="form-label fw-semibold">顏色</div>
            <div class="row g-2">
              <div v-for="option in colorOptions" :key="option" class="col-6">
                <button
                  class="option-button btn w-100"
                  :class="
                    draft.color === option ? 'btn-primary' : 'btn-light border'
                  "
                  type="button"
                  @click="toggleOption('color', option)"
                >
                  {{ option }}
                </button>
              </div>
            </div>
            <input
              v-model.trim="draft.color"
              class="form-control mt-2"
              type="text"
              placeholder="或輸入其他顏色"
            />
          </section>

          <section>
            <div class="form-label fw-semibold">儲存容量</div>
            <div class="row g-2">
              <div v-for="option in storageOptions" :key="option" class="col-6">
                <button
                  class="option-button btn w-100"
                  :class="
                    draft.storage === option
                      ? 'btn-primary'
                      : 'btn-light border'
                  "
                  type="button"
                  @click="toggleOption('storage', option)"
                >
                  {{ option }}
                </button>
              </div>
            </div>
          </section>
        </div>

        <footer
          class="position-absolute bottom-0 start-0 end-0 bg-white border-top p-3 d-flex gap-2"
        >
          <button
            class="btn btn-outline-secondary flex-fill"
            type="button"
            @click="resetFilters"
          >
            清除條件
          </button>
          <button
            class="btn btn-primary flex-fill"
            type="button"
            @click="applyFilters"
          >
            套用篩選
          </button>
        </footer>
      </aside>
    </div>
  </Teleport>
</template>

<style scoped>
.filter-layer {
  z-index: 1090;
  background: rgba(15, 23, 42, 0.48);
}
.filter-drawer {
  width: min(100%, 460px);
}
.drawer-body {
  height: calc(100% - 155px);
  overflow-y: auto;
  padding-bottom: 2rem !important;
}
.option-button {
  min-height: 48px;
}
</style>

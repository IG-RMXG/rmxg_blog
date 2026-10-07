<!-- SecurityVerification.vue -->
<template>
  <div class="security-verification" v-if="showVerification">
    <div class="verification-modal">
      <div class="verification-header">
        <h3>安全验证</h3>
        <el-button type="text" @click="closeVerification">✕</el-button>
      </div>
      <div class="verification-content">
        <div class="background-image" @click="handleBackgroundClick">
          <img :src="verificationImage" alt="Verification Background" />
          <!-- Text elements to click in order -->
          <div 
            v-for="(item, index) in verificationTexts" 
            :key="index"
            class="clickable-text"
            :class="{ 'clicked': clickedOrder.includes(index) }"
            :style="item.style"
            @click.stop="handleTextClick(index)"
          >
            {{ item.text }}
          </div>
        </div>
        <div class="verification-instructions">
          <p>请按顺序点击以下文字: {{ targetOrder.map(i => verificationTexts[i].text).join(' → ') }}</p>
          <div class="clicked-sequence">
            已点击: {{ clickedOrder.map(i => verificationTexts[i].text).join(' → ') }}
          </div>
        </div>
      </div>
      <div class="verification-actions">
        <el-button @click="closeVerification">取消</el-button>
        <el-button type="primary" @click="confirmVerification" :disabled="!isVerificationComplete">确认</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  showVerification: Boolean
})

const emit = defineEmits(['close', 'success'])

const verificationImage = ref('../assets/images/verification-background.png')
const clickedOrder = ref([])
const targetOrder = ref([2, 0, 3, 1]) // Predefined order

const verificationTexts = ref([
  { text: '安全', style: { top: '20%', left: '30%' } },
  { text: '验证', style: { top: '60%', left: '70%' } },
  { text: '点击', style: { top: '40%', left: '20%' } },
  { text: '确认', style: { top: '70%', left: '50%' } }
])

const isVerificationComplete = computed(() => {
  return clickedOrder.value.length === targetOrder.value.length && 
         JSON.stringify(clickedOrder.value) === JSON.stringify(targetOrder.value)
})

function handleTextClick(index) {
  if (!clickedOrder.value.includes(index)) {
    clickedOrder.value.push(index)
  }
}

function handleBackgroundClick() {
  // Reset if clicked on background
  clickedOrder.value = []
}

function confirmVerification() {
  if (isVerificationComplete.value) {
    emit('success')
    closeVerification()
  }
}

function closeVerification() {
  clickedOrder.value = []
  emit('close')
}
</script>

<style scoped>
.security-verification {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.verification-modal {
  background: white;
  border-radius: 8px;
  width: 500px;
  max-width: 90%;
}

.verification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.verification-content {
  padding: 20px;
}

.background-image {
  position: relative;
  width: 100%;
  height: 300px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
}

.background-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.clickable-text {
  position: absolute;
  padding: 5px 10px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
}

.clickable-text:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.05);
}

.clickable-text.clicked {
  background: #409eff;
  color: white;
}

.verification-instructions {
  margin-top: 15px;
  text-align: center;
}

.clicked-sequence {
  margin-top: 10px;
  font-size: 14px;
  color: #666;
}

.verification-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 15px 20px;
  border-top: 1px solid #eee;
}
</style>
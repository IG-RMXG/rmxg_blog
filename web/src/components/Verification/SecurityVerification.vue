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
/**
 * 安全验证弹窗：人机校验（按提示顺序点选背景图上的文字）
 * - 显隐由父组件传入的 showVerification 控制，组件自身不持有开关状态
 * - emits：close（取消/关闭/验证通过后关闭）、success（顺序完全正确并点击"确认"）
 * - 校验纯前端比对，仅作为防机器人/误操作的交互门槛，安全边界仍需后端接口自行鉴权
 */
const props = defineProps({
  showVerification: Boolean
})

const emit = defineEmits(['close', 'success'])

const verificationImage = ref('../assets/images/verification-background.png')
const clickedOrder = ref([])
// 正确顺序（存的是 verificationTexts 的下标）：点击 → 安全 → 确认 → 验证，即 [2, 0, 3, 1]
const targetOrder = ref([2, 0, 3, 1]) // Predefined order

// 可点文字及其位置：style 用 top/left 百分比定位，基准是 .background-image 相对定位容器
const verificationTexts = ref([
  { text: '安全', style: { top: '20%', left: '30%' } },
  { text: '验证', style: { top: '60%', left: '70%' } },
  { text: '点击', style: { top: '40%', left: '20%' } },
  { text: '确认', style: { top: '70%', left: '50%' } }
])

// 完成条件：点击数量到位且序列与 targetOrder 完全一致（用 JSON.stringify 做有序比较，顺序不同即失败）
const isVerificationComplete = computed(() => {
  return clickedOrder.value.length === targetOrder.value.length && 
         JSON.stringify(clickedOrder.value) === JSON.stringify(targetOrder.value)
})

// 重复点击同一文字会被忽略：已点过的项不再入栈，避免误触把顺序彻底打乱
function handleTextClick(index) {
  if (!clickedOrder.value.includes(index)) {
    clickedOrder.value.push(index)
  }
}

// 点击空白背景即清空重来（文字上的点击带 .stop，不会冒泡到这里触发清空）
function handleBackgroundClick() {
  // Reset if clicked on background
  clickedOrder.value = []
}

// 校验通过先抛 success 再走关闭流程，注意父组件会先后收到 success 与 close 两个事件
function confirmVerification() {
  if (isVerificationComplete.value) {
    emit('success')
    closeVerification()
  }
}

// 关闭：清空已点序列，保证下次打开是干净的初始状态
function closeVerification() {
  clickedOrder.value = []
  emit('close')
}
</script>

<style scoped>
/* 全屏遮罩：fixed 铺满视口，层级抬高以盖住页面内容 */
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
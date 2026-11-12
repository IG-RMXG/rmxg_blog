<template>
    <!-- 小时维度的四种互斥写法；区间/步长的上界为 23 -->
    <el-form>
        <el-form-item>
            <el-radio v-model='radioValue' :value="1">
                小时，允许的通配符[, - * /]
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="2">
                周期从
                <el-input-number v-model='cycle01' :min="0" :max="22" /> -
                <el-input-number v-model='cycle02' :min="cycle01 + 1" :max="23" /> 时
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="3">
                从
                <el-input-number v-model='average01' :min="0" :max="22" /> 时开始，每
                <el-input-number v-model='average02' :min="1" :max="23 - average01" /> 小时执行一次
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="4">
                指定
                <el-select clearable v-model="checkboxList" placeholder="可多选" multiple :multiple-limit="10">
                    <el-option v-for="item in 24" :key="item" :label="item - 1" :value="item - 1" />
                </el-select>
            </el-radio>
        </el-form-item>
    </el-form>
</template>

<script setup>
// 「小时」维度编辑器：结构与 second.vue 同构，字段名 hour、取值 0-23；
// 额外承担一次「降级收窄」：小时字段一变，仍为 * 的分/秒会被压成 0（见 changeRadioValue）。
const emit = defineEmits(['update'])
// cron：父组件汇总的表达式对象，用于反解析回显与联动判断；check：父组件注入的整数夹紧函数
const props = defineProps({
    cron: {
        type: Object,
        default: {
            second: "*",
            min: "*",
            hour: "*",
            day: "*",
            month: "*",
            week: "?",
            year: "",
        }
    },
    check: {
        type: Function,
        default: () => {
        }
    }
})
// radioValue 模式：1=* 2=区间 3=步长 4=枚举
const radioValue = ref(1)
const cycle01 = ref(0)
const cycle02 = ref(1)
const average01 = ref(0)
const average02 = ref(1)
const checkboxList = ref([])
// 保存最后一次非空枚举，多选被清空时兜底
const checkCopy = ref([0])
// 三个 computed 兼做「夹紧输入」与「拼片段」，夹紧结果写回 ref 让输入框同步
const cycleTotal = computed(() => {
    cycle01.value = props.check(cycle01.value, 0, 22)
    cycle02.value = props.check(cycle02.value, cycle01.value + 1, 23)
    return cycle01.value + '-' + cycle02.value
})
const averageTotal = computed(() => {
    average01.value = props.check(average01.value, 0, 22)
    average02.value = props.check(average02.value, 1, 23 - average01.value)
    return average01.value + '/' + average02.value
})
const checkboxString = computed(() => {
    return checkboxList.value.join(',')
})
// 父组件改写 hour 时回显；模式或片段变化时回写父组件
watch(() => props.cron.hour, value => changeRadioValue(value))
watch([radioValue, cycleTotal, averageTotal, checkboxString], () => onRadioChange())
// 按 * → 区间(-) → 步长(/) → 枚举 识别写法
// 前置修正：小时字段任意变化时，先把仍为 * 的分、秒压成 0，避免组合出「每小时每分钟每秒都跑」的表达式；
// 该修正不看小时最终取值，小时被置为 * 时同样会执行
function changeRadioValue(value) {
    if (props.cron.min === '*') {
        emit('update', 'min', '0', 'hour')
    }
    if (props.cron.second === '*') {
        emit('update', 'second', '0', 'hour')
    }
    if (value === '*') {
        radioValue.value = 1
    } else if (value.indexOf('-') > -1) {
        const indexArr = value.split('-')
        cycle01.value = Number(indexArr[0])
        cycle02.value = Number(indexArr[1])
        radioValue.value = 2
    } else if (value.indexOf('/') > -1) {
        const indexArr = value.split('/')
        average01.value = Number(indexArr[0])
        average02.value = Number(indexArr[1])
        radioValue.value = 3
    } else {
        checkboxList.value = [...new Set(value.split(',').map(item => Number(item)))]
        radioValue.value = 4
    }
}
// 枚举模式去重转数字后回写；列表清空时补回上次选择，避免发出空片段
function onRadioChange() {
    switch (radioValue.value) {
        case 1:
            emit('update', 'hour', '*', 'hour')
            break
        case 2:
            emit('update', 'hour', cycleTotal.value, 'hour')
            break
        case 3:
            emit('update', 'hour', averageTotal.value, 'hour')
            break
        case 4:
            if (checkboxList.value.length === 0) {
                checkboxList.value.push(checkCopy.value[0])
            } else {
                checkCopy.value = checkboxList.value
            }
            emit('update', 'hour', checkboxString.value, 'hour')
            break
    }
}
</script>

<style lang="scss" scoped>
/* 数字输入框与下拉框左右留白，下拉框定宽保证四个单选行左边缘对齐 */
.el-input-number--small, .el-select, .el-select--small {
    margin: 0 0.2rem;
}
.el-select, .el-select--small {
    width: 18.8rem;
}
</style>
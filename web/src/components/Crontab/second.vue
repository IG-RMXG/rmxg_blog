<template>
    <!-- 秒维度的四种互斥写法：* / 区间 a-b / 从 a 起每 b 秒 / 枚举（最多 10 个） -->
    <el-form>
        <el-form-item>
            <el-radio v-model='radioValue' :value="1">
                秒，允许的通配符[, - * /]
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="2">
                周期从
                <el-input-number v-model='cycle01' :min="0" :max="58" /> -
                <el-input-number v-model='cycle02' :min="cycle01 + 1" :max="59" /> 秒
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="3">
                从
                <el-input-number v-model='average01' :min="0" :max="58" /> 秒开始，每
                <el-input-number v-model='average02' :min="1" :max="59 - average01" /> 秒执行一次
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="4">
                指定
                <el-select clearable v-model="checkboxList" placeholder="可多选" multiple :multiple-limit="10">
                    <el-option v-for="item in 60" :key="item" :label="item - 1" :value="item - 1" />
                </el-select>
            </el-radio>
        </el-form-item>
    </el-form>
</template>

<script setup>
// 「秒」维度编辑器：本身不拼整条表达式，只把本字段片段通过 update 交给父组件 index.vue
// 事件约定：emit('update', 'second', 片段字符串, 'second')，父组件用前两个参数写回 crontabValueObj
const emit = defineEmits(['update'])
// cron：父组件汇总的完整表达式对象，用于反解析回显
// check：父组件注入的整数夹紧函数 (value, min, max)，默认空实现避免未传时报错
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
// radioValue 对应四种模式：1=* 2=区间 3=步长 4=枚举
const radioValue = ref(1)
const cycle01 = ref(0)
const cycle02 = ref(1)
const average01 = ref(0)
const average02 = ref(1)
const checkboxList = ref([])
// 记住最后一次非空的枚举选择，用户清空多选框时用它兜底，避免发出空片段
const checkCopy = ref([0])
// 以下三个 computed 实际承担「取值夹紧 + 拼片段」两件事：
// 在 computed 里直接给 ref 赋值是刻意为之，让输入框即时显示被夹紧后的值
const cycleTotal = computed(() => {
    cycle01.value = props.check(cycle01.value, 0, 58)
    cycle02.value = props.check(cycle02.value, cycle01.value + 1, 59)
    return cycle01.value + '-' + cycle02.value
})
const averageTotal = computed(() => {
    average01.value = props.check(average01.value, 0, 58)
    average02.value = props.check(average02.value, 1, 59 - average01.value)
    return average01.value + '/' + average02.value
})
const checkboxString = computed(() => {
    return checkboxList.value.join(',')
})
// 父组件改写 second（回显历史表达式、重置、其他维度联动）时，把片段反解析回单选状态与输入框
watch(() => props.cron.second, value => changeRadioValue(value))
// 模式或片段任一变化都要回写父组件；片段 computed 已保证取值合法
watch([radioValue, cycleTotal, averageTotal, checkboxString], () => onRadioChange())
// 反解析顺序：* → 区间(-) → 步长(/) → 枚举，与 Quartz 允许的几种片段写法一一对应
function changeRadioValue(value) {
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
// 枚举模式被清空时，el-select 会给出空数组，这里塞回上次的选择，保证片段不为空串
// 单选按钮值变化时
function onRadioChange() {
    switch (radioValue.value) {
        case 1:
            emit('update', 'second', '*', 'second')
            break
        case 2:
            emit('update', 'second', cycleTotal.value, 'second')
            break
        case 3:
            emit('update', 'second', averageTotal.value, 'second')
            break
        case 4:
            if (checkboxList.value.length === 0) {
                checkboxList.value.push(checkCopy.value[0])
            } else {
                checkCopy.value = checkboxList.value
            }
            emit('update', 'second', checkboxString.value, 'second')
            break
    }
}
</script>

<style lang="scss" scoped>
/* 数字输入框与下拉框的间距、下拉框固定宽度，保持一行内文案对齐 */
.el-input-number--small, .el-select, .el-select--small {
    margin: 0 0.2rem;
}
.el-select, .el-select--small {
    width: 18.8rem;
}
</style>
<template>
    <!-- 「月」维度四种写法：* / 区间 / 步长 / 枚举（月份用中文名展示，值仍是 1-12） -->
    <el-form>
        <el-form-item>
            <el-radio v-model='radioValue' :value="1">
                月，允许的通配符[, - * /]
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="2">
                周期从
                <el-input-number v-model='cycle01' :min="1" :max="11" /> -
                <el-input-number v-model='cycle02' :min="cycle01 + 1" :max="12" /> 月
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="3">
                从
                <el-input-number v-model='average01' :min="1" :max="11" /> 月开始，每
                <el-input-number v-model='average02' :min="1" :max="12 - average01" /> 月月执行一次
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="4">
                指定
                <el-select clearable v-model="checkboxList" placeholder="可多选" multiple :multiple-limit="8">
                    <el-option v-for="item in monthList" :key="item.key" :label="item.value" :value="item.key" />
                </el-select>
            </el-radio>
        </el-form-item>
    </el-form>
</template>

<script setup>
// 「月」维度编辑器：字段名 month、取值 1-12，模式与秒/分一致（* / 区间 / 步长 / 枚举）。
// 月份下拉用中文名，但提交给表达式的始终是数字，避免后端解析失败。
const emit = defineEmits(['update'])
// cron：父组件汇总的表达式对象，用于反解析回显；check：父组件注入的整数夹紧函数
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
const cycle01 = ref(1)
const cycle02 = ref(2)
const average01 = ref(1)
const average02 = ref(1)
const checkboxList = ref([])
// 保存最后一次非空枚举，多选清空时兜底（月份下限为 1）
const checkCopy = ref([1])
// 下拉选项：key 为写入表达式的数值，value 仅用于界面展示
const monthList = ref([
    {key: 1, value: '一月'},
    {key: 2, value: '二月'},
    {key: 3, value: '三月'},
    {key: 4, value: '四月'},
    {key: 5, value: '五月'},
    {key: 6, value: '六月'},
    {key: 7, value: '七月'},
    {key: 8, value: '八月'},
    {key: 9, value: '九月'},
    {key: 10, value: '十月'},
    {key: 11, value: '十一月'},
    {key: 12, value: '十二月'}
])
// computed 兼做夹紧与拼片段，夹紧后的值写回 ref 让输入框同步
const cycleTotal = computed(() => {
    cycle01.value = props.check(cycle01.value, 1, 11)
    cycle02.value = props.check(cycle02.value, cycle01.value + 1, 12)
    return cycle01.value + '-' + cycle02.value
})
const averageTotal = computed(() => {
    average01.value = props.check(average01.value, 1, 11)
    average02.value = props.check(average02.value, 1, 12 - average01.value)
    return average01.value + '/' + average02.value
})
const checkboxString = computed(() => {
    return checkboxList.value.join(',')
})
// 父组件改写 month 时反解析回显；模式或片段变化时回写父组件
watch(() => props.cron.month, value => changeRadioValue(value))
watch([radioValue, cycleTotal, averageTotal, checkboxString], () => onRadioChange())
// 按 * → 区间(-) → 步长(/) → 枚举 的顺序识别写法
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
// 枚举模式去重转数字后回写；列表为空时补回上次选择，避免发出空片段
function onRadioChange() {
    switch (radioValue.value) {
        case 1:
            emit('update', 'month', '*', 'month')
            break
        case 2:
            emit('update', 'month', cycleTotal.value, 'month')
            break
        case 3:
            emit('update', 'month', averageTotal.value, 'month')
            break
        case 4:
            if (checkboxList.value.length === 0) {
                checkboxList.value.push(checkCopy.value[0])
            } else {
                checkCopy.value = checkboxList.value
            }
            emit('update', 'month', checkboxString.value, 'month')
            break
    }
}
</script>

<style lang="scss" scoped>
.el-input-number--small, .el-select, .el-select--small {
    margin: 0 0.2rem;
}
/* 下拉框定宽，四行单选的控件与文案纵向对齐 */
.el-select, .el-select--small {
    width: 18.8rem;
}
</style>
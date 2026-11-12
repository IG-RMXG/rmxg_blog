<template>
    <!-- 「日」维度七种写法：* / 不指定(?) / 区间 / 步长 / 最近工作日(nW) / 本月最后一天(L) / 枚举 -->
    <el-form>
        <el-form-item>
            <el-radio v-model='radioValue' :value="1">
                日，允许的通配符[, - * ? / L W]
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="2">
                不指定
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="3">
                周期从
                <el-input-number v-model='cycle01' :min="1" :max="30" /> -
                <el-input-number v-model='cycle02' :min="cycle01 + 1" :max="31" /> 日
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="4">
                从
                <el-input-number v-model='average01' :min="1" :max="30" /> 号开始，每
                <el-input-number v-model='average02' :min="1" :max="31 - average01" /> 日执行一次
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="5">
                每月
                <el-input-number v-model='workday' :min="1" :max="31" /> 号最近的那个工作日
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="6">
                本月最后一天
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="7">
                指定
                <el-select clearable v-model="checkboxList" placeholder="可多选" multiple :multiple-limit="10">
                    <el-option v-for="item in 31" :key="item" :label="item" :value="item" />
                </el-select>
            </el-radio>
        </el-form-item>
    </el-form>
</template>
<script setup>
// 「日」维度编辑器：字段名 day，取值 1-31，比秒/分多出 ?、W、L 三个 Quartz 专有写法。
// 关键约束：日与周互斥——选择“不指定(?)”时把 week 放开为 *，选择其他写法时把 week 压回 ?，
// 该联动写在 onRadioChange 顶部，保证导出的表达式始终满足 Quartz 的“两者仅一个为 ?”。
const emit = defineEmits(['update'])
// cron：父组件汇总的表达式对象，需读 week 判断联动；check：父组件注入的整数夹紧函数
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
// radioValue 模式：1=* 2=? 3=区间 4=步长 5=nW 6=L 7=枚举
const radioValue = ref(1)
const cycle01 = ref(1)
const cycle02 = ref(2)
const average01 = ref(1)
const average02 = ref(1)
// 「最近的工作日」中的日期，最终拼成 nW（Quartz 语义：该日最近的一个工作日）
const workday = ref(1)
const checkboxList = ref([])
// 保存最后一次非空枚举，多选清空时兜底（日的默认下限是 1）
const checkCopy = ref([1])
// computed 兼做夹紧与拼片段：区间上界随起点收缩，步长值不超过 31-起点，避免生成无效跨度
const cycleTotal = computed(() => {
    cycle01.value = props.check(cycle01.value, 1, 30)
    cycle02.value = props.check(cycle02.value, cycle01.value + 1, 31)
    return cycle01.value + '-' + cycle02.value
})
const averageTotal = computed(() => {
    average01.value = props.check(average01.value, 1, 30)
    average02.value = props.check(average02.value, 1, 31 - average01.value)
    return average01.value + '/' + average02.value
})
const workdayTotal = computed(() => {
    workday.value = props.check(workday.value, 1, 31)
    return workday.value + 'W'
})
const checkboxString = computed(() => {
    return checkboxList.value.join(',')
})
// 父组件改写 day 时回显（含切换周维度后被置为 ? 的情况）；模式或片段变化时回写父组件
watch(() => props.cron.day, value => changeRadioValue(value))
watch([radioValue, cycleTotal, averageTotal, workdayTotal, checkboxString], () => onRadioChange())
// 解析顺序需与 Quartz 符号匹配：先 ?、再区间/步长，W/L 是单值写法，其余按枚举处理
function changeRadioValue(value) {
    if (value === "*") {
        radioValue.value = 1
    } else if (value === "?") {
        radioValue.value = 2
    } else if (value.indexOf("-") > -1) {
        const indexArr = value.split('-')
        cycle01.value = Number(indexArr[0])
        cycle02.value = Number(indexArr[1])
        radioValue.value = 3
    } else if (value.indexOf("/") > -1) {
        const indexArr = value.split('/')
        average01.value = Number(indexArr[0])
        average02.value = Number(indexArr[1])
        radioValue.value = 4
    } else if (value.indexOf("W") > -1) {
        const indexArr = value.split("W")
        workday.value = Number(indexArr[0])
        radioValue.value = 5
    } else if (value === "L") {
        radioValue.value = 6
    } else {
        checkboxList.value = [...new Set(value.split(',').map(item => Number(item)))]
        radioValue.value = 7
    }
}
// 日/周互斥联动：日选“不指定(?)”时周必须是 *，日选其他模式时周必须是 ?，
// 两次 emit 顺序无要求，父组件按字段名写入，后一次不会覆盖前一次
// 单选按钮值变化时
function onRadioChange() {
    if (radioValue.value === 2 && props.cron.week === '?') {
        emit('update', 'week', '*', 'day')
    }
    if (radioValue.value !== 2 && props.cron.week !== '?') {
        emit('update', 'week', '?', 'day')
    }
    switch (radioValue.value) {
        case 1:
            emit('update', 'day', '*', 'day')
            break
        case 2:
            emit('update', 'day', '?', 'day')
            break
        case 3:
            emit('update', 'day', cycleTotal.value, 'day')
            break
        case 4:
            emit('update', 'day', averageTotal.value, 'day')
            break
        case 5:
            emit('update', 'day', workdayTotal.value, 'day')
            break
        case 6:
            emit('update', 'day', 'L', 'day')
            break
        case 7:
            if (checkboxList.value.length === 0) {
                checkboxList.value.push(checkCopy.value[0])
            } else {
                checkCopy.value = checkboxList.value
            }
            emit('update', 'day', checkboxString.value, 'day')
            break
    }
}
</script>

<style lang="scss" scoped>
.el-input-number--small, .el-select, .el-select--small {
    margin: 0 0.2rem;
}
/* 下拉框定宽，保证七行单选内的控件与文案纵向对齐 */
.el-select, .el-select--small {
    width: 18.8rem;
}
</style>
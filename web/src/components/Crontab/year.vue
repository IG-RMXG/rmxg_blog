<template>
    <!-- 「年」维度：不填（输出 6 位表达式）/ 每年(*) / 区间 / 每 n 年 / 枚举（当年起共 9 个候选年） -->
    <el-form>
        <el-form-item>
            <el-radio :value="1" v-model='radioValue'>
                不填，允许的通配符[, - * /]
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio :value="2" v-model='radioValue'>
                每年
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio :value="3" v-model='radioValue'>
                周期从
                <el-input-number v-model='cycle01' :min='fullYear' :max="2098"/> -
                <el-input-number v-model='cycle02' :min="cycle01 ? cycle01 + 1 : fullYear + 1" :max="2099"/>
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio :value="4" v-model='radioValue'>
                从
                <el-input-number v-model='average01' :min='fullYear' :max="2098"/> 年开始，每
                <el-input-number v-model='average02' :min="1" :max="2099 - average01 || fullYear"/> 年执行一次
            </el-radio>

        </el-form-item>

        <el-form-item>
            <el-radio :value="5" v-model='radioValue'>
                指定
                <el-select clearable v-model="checkboxList" placeholder="可多选" multiple :multiple-limit="8">
                    <el-option v-for="item in 9" :key="item" :value="item - 1 + fullYear" :label="item -1 + fullYear" />
                </el-select>
            </el-radio>
        </el-form-item>
    </el-form>
</template>

<script setup>
// 「年」维度编辑器：字段名 year，带年份就是 7 位表达式，为空串则退回 6 位（Quartz 中年是可省略字段）。
// 候选年从当前年份起算，避免用户选出已经过去的年份。
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
            year: ""
        }
    },
    check: {
        type: Function,
        default: () => {
        }
    }
})

// 以运行时的当前年份为下界；maxFullYear 只用于 computed 里的夹紧，
// 模板中 input-number 的 max 是硬编码的 2098/2099，两处上界并不一致（历史写法，改动前需同步两处）
const fullYear = Number(new Date().getFullYear())
const maxFullYear = fullYear + 10
// radioValue 模式：1=不填 2=每年(*) 3=区间 4=每 n 年 5=枚举
const radioValue = ref(1)
const cycle01 = ref(fullYear)
const cycle02 = ref(fullYear + 1)
const average01 = ref(fullYear)
const average02 = ref(1)
const checkboxList = ref([])
// 保存最后一次非空枚举，多选清空时兜底（默认回落到当前年）
const checkCopy = ref([fullYear])

// computed 兼做夹紧与拼片段：区间终点至少比起点大 1，步长的步数上限 10（约十年跨度）
const cycleTotal = computed(() => {
    cycle01.value = props.check(cycle01.value, fullYear, maxFullYear - 1)
    cycle02.value = props.check(cycle02.value, cycle01.value + 1, maxFullYear)
    return cycle01.value + '-' + cycle02.value
})
const averageTotal = computed(() => {
    average01.value = props.check(average01.value, fullYear, maxFullYear - 1)
    average02.value = props.check(average02.value, 1, 10)
    return average01.value + '/' + average02.value
})
const checkboxString = computed(() => {
    return checkboxList.value.join(',')
})
// 父组件改写 year 时回显；year 为空串也要走一次反解析，把界面切回“不填”
watch(() => props.cron.year, value => changeRadioValue(value))
// 模式或片段变化时回写父组件
watch([radioValue, cycleTotal, averageTotal, checkboxString], () => onRadioChange())
// 空串代表不填年份（6 位表达式），必须最先判断，否则会落到枚举分支
function changeRadioValue(value) {
    if (value === '') {
        radioValue.value = 1
    } else if (value === "*") {
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
    } else {
        checkboxList.value = [...new Set(value.split(',').map(item => Number(item)))]
        radioValue.value = 5
    }
}
// 模式 1 发空串而非 *，父组件拼串时据此决定是否输出第 7 段
// 枚举模式去重转数字后回写，列表清空时补回上次选择
function onRadioChange() {
    switch (radioValue.value) {
        case 1:
            emit('update', 'year', '', 'year')
            break
        case 2:
            emit('update', 'year', '*', 'year')
            break
        case 3:
            emit('update', 'year', cycleTotal.value, 'year')
            break
        case 4:
            emit('update', 'year', averageTotal.value, 'year')
            break
        case 5:
            if (checkboxList.value.length === 0) {
                checkboxList.value.push(checkCopy.value[0])
            } else {
                checkCopy.value = checkboxList.value
            }
            emit('update', 'year', checkboxString.value, 'year')
            break
    }
}
</script>

<style lang="scss" scoped>
.el-input-number--small, .el-select, .el-select--small {
    margin: 0 0.2rem;
}
/* 下拉框定宽，五行单选的控件与文案纵向对齐 */
.el-select, .el-select--small {
    width: 18.8rem;
}
</style>
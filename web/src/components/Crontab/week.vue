<template>
    <!-- 「周」维度六种写法：* / 不指定(?) / 区间 / 第 n 周的星期几(n#d) / 本月最后一个星期几(dL) / 枚举 -->
    <el-form>
        <el-form-item>
            <el-radio v-model='radioValue' :value="1">
                周，允许的通配符[, - * ? / L #]
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
                <el-select clearable v-model="cycle01">
                    <el-option
                        v-for="(item,index) of weekList"
                        :key="index"
                        :label="item.value"
                        :value="item.key"
                        :disabled="item.key === 7"
                    >{{item.value}}</el-option>
                </el-select>
                -
                <el-select clearable v-model="cycle02">
                    <el-option
                        v-for="(item,index) of weekList"
                        :key="index"
                        :label="item.value"
                        :value="item.key"
                        :disabled="item.key <= cycle01"
                    >{{item.value}}</el-option>
                </el-select>
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="4">
                第
                <el-input-number v-model='average01' :min="1" :max="4" /> 周的
                <el-select clearable v-model="average02">
                    <el-option v-for="item in weekList" :key="item.key" :label="item.value" :value="item.key" />
                </el-select>
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="5">
                本月最后一个
                <el-select clearable v-model="weekday">
                    <el-option v-for="item in weekList" :key="item.key" :label="item.value" :value="item.key" />
                </el-select>
            </el-radio>
        </el-form-item>

        <el-form-item>
            <el-radio v-model='radioValue' :value="6">
                指定
                <el-select class="multiselect" clearable v-model="checkboxList" placeholder="可多选" multiple :multiple-limit="6">
                    <el-option v-for="item in weekList" :key="item.key" :label="item.value" :value="item.key" />
                </el-select>
            </el-radio>
        </el-form-item>

    </el-form>
</template>

<script setup>
// 「周」维度编辑器：字段名 week，取值 1-7（Quartz 规定 1=星期日，与 JS getDay 的 0=星期日不同）。
// 与 day.vue 互为镜像的约束：周选“不指定(?)”时把 day 放开为 *，否则把 day 压回 ?，
// 保证表达式里日和周有且只有一个为 ?；具体联动见 onRadioChange 顶部。
const emit = defineEmits(['update'])
// cron：父组件汇总的表达式对象，需读 day 判断联动；check：父组件注入的整数夹紧函数
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
// radioValue 模式：1=* 2=? 3=区间 4=n#d 5=dL 6=枚举（默认 2，与 week 的 "?" 初值对应）
const radioValue = ref(2)
const cycle01 = ref(2)
const cycle02 = ref(3)
// average01 存“第几周”，average02 存“星期几”，拼接时反序输出为 d#n（与 Quartz 的 n#d 对应）
const average01 = ref(1)
const average02 = ref(2)
// 「本月最后一个星期几」中的星期值，片段为 dL
const weekday = ref(2)
const checkboxList = ref([])
// 保存最后一次非空枚举，多选清空时兜底（周的默认下限为 1）
const checkCopy = ref([2])
// 选项 key 直接对应 Quartz 的周序号：1=星期日、2=星期一 …… 7=星期六
const weekList = ref([
    {key: 1, value: '星期日'},
    {key: 2, value: '星期一'},
    {key: 3, value: '星期二'},
    {key: 4, value: '星期三'},
    {key: 5, value: '星期四'},
    {key: 6, value: '星期五'},
    {key: 7, value: '星期六'}
])
// computed 兼做夹紧与拼片段：区间不跨周回绕（起点最大 6、终点必须大于起点，模板里也用 disabled 做了同样限制）；
// n#d 的周次上限是 4，dL 的星期值范围是 1-7
const cycleTotal = computed(() => {
    cycle01.value = props.check(cycle01.value, 1, 6)
    cycle02.value = props.check(cycle02.value, cycle01.value + 1, 7)
    return cycle01.value + '-' + cycle02.value
})
const averageTotal = computed(() => {
    average01.value = props.check(average01.value, 1, 4)
    average02.value = props.check(average02.value, 1, 7)
    return average02.value + '#' + average01.value
})
const weekdayTotal = computed(() => {
    weekday.value = props.check(weekday.value, 1, 7)
    return weekday.value + 'L'
})
const checkboxString = computed(() => {
    return checkboxList.value.join(',')
})
// 父组件改写 week 时回显（含切换日维度后被置为 ? 的情况）；模式或片段变化时回写父组件
watch(() => props.cron.week, value => changeRadioValue(value))
watch([radioValue, cycleTotal, averageTotal, weekdayTotal, checkboxString], () => onRadioChange())
// 解析顺序：# 与 L 必须先于区间判断，否则 "1#2" 这类片段会落到枚举分支
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
    } else if (value.indexOf("#") > -1) {
        const indexArr = value.split('#')
        average01.value = Number(indexArr[1])
        average02.value = Number(indexArr[0])
        radioValue.value = 4
    } else if (value.indexOf("L") > -1) {
        const indexArr = value.split("L")
        weekday.value = Number(indexArr[0])
        radioValue.value = 5
    } else {
        checkboxList.value = [...new Set(value.split(',').map(item => Number(item)))]
        radioValue.value = 6
    }
}
// 日/周互斥联动（day.vue 的镜像）：周选“不指定(?)”时 day 必须是 *，否则 day 必须是 ?
function onRadioChange() {
    if (radioValue.value === 2 && props.cron.day === '?') {
        emit('update', 'day', '*', 'week')
    }
    if (radioValue.value !== 2 && props.cron.day !== '?') {
        emit('update', 'day', '?', 'week')
    }
    switch (radioValue.value) {
        case 1:
            emit('update', 'week', '*', 'week')
            break
        case 2:
            emit('update', 'week', '?', 'week')
            break
        case 3:
            emit('update', 'week', cycleTotal.value, 'week')
            break
        case 4:
            emit('update', 'week', averageTotal.value, 'week')
            break
        case 5:
            emit('update', 'week', weekdayTotal.value, 'week')
            break
        case 6:
            if (checkboxList.value.length === 0) {
                checkboxList.value.push(checkCopy.value[0])
            } else {
                checkCopy.value = checkboxList.value
            }
            emit('update', 'week', checkboxString.value, 'week')
            break
    }
}
</script>

<style lang="scss" scoped>
.el-input-number--small, .el-select, .el-select--small {
    margin: 0 0.5rem;
}
/* 周维度的下拉更窄（单值选择），多选框单独放宽到 17.8rem 以容纳最多 6 个星期 */
.el-select, .el-select--small {
    width: 8rem;
}
.el-select.multiselect, .el-select--small.multiselect {
    width: 17.8rem;
}
</style>
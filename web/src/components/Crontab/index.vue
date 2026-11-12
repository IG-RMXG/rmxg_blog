<template>
    <div>
        <!-- Cron 表达式生成器：秒/分/时/日/月/周/年 各占一个 tab，改动实时汇总成整条表达式 -->
        <!-- shouldHide(key) 为 true 才显示该 tab；列在 hideComponent 里的维度不渲染 -->
        <el-tabs type="border-card">
            <el-tab-pane label="秒" v-if="shouldHide('second')">
                <CrontabSecond
                    @update="updateCrontabValue"
                    :check="checkNumber"
                    :cron="crontabValueObj"
                    ref="cronsecond"
                />
            </el-tab-pane>

            <el-tab-pane label="分钟" v-if="shouldHide('min')">
                <CrontabMin
                    @update="updateCrontabValue"
                    :check="checkNumber"
                    :cron="crontabValueObj"
                    ref="cronmin"
                />
            </el-tab-pane>

            <el-tab-pane label="小时" v-if="shouldHide('hour')">
                <CrontabHour
                    @update="updateCrontabValue"
                    :check="checkNumber"
                    :cron="crontabValueObj"
                    ref="cronhour"
                />
            </el-tab-pane>

            <el-tab-pane label="日" v-if="shouldHide('day')">
                <CrontabDay
                    @update="updateCrontabValue"
                    :check="checkNumber"
                    :cron="crontabValueObj"
                    ref="cronday"
                />
            </el-tab-pane>

            <el-tab-pane label="月" v-if="shouldHide('month')">
                <CrontabMonth
                    @update="updateCrontabValue"
                    :check="checkNumber"
                    :cron="crontabValueObj"
                    ref="cronmonth"
                />
            </el-tab-pane>

            <el-tab-pane label="周" v-if="shouldHide('week')">
                <CrontabWeek
                    @update="updateCrontabValue"
                    :check="checkNumber"
                    :cron="crontabValueObj"
                    ref="cronweek"
                />
            </el-tab-pane>

            <el-tab-pane label="年" v-if="shouldHide('year')">
                <CrontabYear
                    @update="updateCrontabValue"
                    :check="checkNumber"
                    :cron="crontabValueObj"
                    ref="cronyear"
                />
            </el-tab-pane>
        </el-tabs>

        <!-- 表达式预览区：按字段展示当前取值，超过宽度时用 tooltip 浮出完整值 -->
        <div class="popup-main">
            <!-- 表头顺序与 crontabValueObj 的 7 个字段严格对应，最后一列是拼接后的完整表达式 -->
            <div class="popup-result">
                <p class="title">时间表达式</p>
                <table>
                    <thead>
                        <tr>
                            <th v-for="item of tabTitles" :key="item">{{item}}</th>
                            <th>Cron 表达式</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>
                                <span v-if="crontabValueObj.second.length < 10">{{crontabValueObj.second}}</span>
                                <el-tooltip v-else :content="crontabValueObj.second" placement="top"><span>{{crontabValueObj.second}}</span></el-tooltip>
                            </td>
                            <td>
                                <span v-if="crontabValueObj.min.length < 10">{{crontabValueObj.min}}</span>
                                <el-tooltip v-else :content="crontabValueObj.min" placement="top"><span>{{crontabValueObj.min}}</span></el-tooltip>
                            </td>
                            <td>
                                <span v-if="crontabValueObj.hour.length < 10">{{crontabValueObj.hour}}</span>
                                <el-tooltip v-else :content="crontabValueObj.hour" placement="top"><span>{{crontabValueObj.hour}}</span></el-tooltip>
                            </td>
                            <td>
                                <span v-if="crontabValueObj.day.length < 10">{{crontabValueObj.day}}</span>
                                <el-tooltip v-else :content="crontabValueObj.day" placement="top"><span>{{crontabValueObj.day}}</span></el-tooltip>
                            </td>
                            <td>
                                <span v-if="crontabValueObj.month.length < 10">{{crontabValueObj.month}}</span>
                                <el-tooltip v-else :content="crontabValueObj.month" placement="top"><span>{{crontabValueObj.month}}</span></el-tooltip>
                            </td>
                            <td>
                                <span v-if="crontabValueObj.week.length < 10">{{crontabValueObj.week}}</span>
                                <el-tooltip v-else :content="crontabValueObj.week" placement="top"><span>{{crontabValueObj.week}}</span></el-tooltip>
                            </td>
                            <td>
                                <span v-if="crontabValueObj.year.length < 10">{{crontabValueObj.year}}</span>
                                <el-tooltip v-else :content="crontabValueObj.year" placement="top"><span>{{crontabValueObj.year}}</span></el-tooltip>
                            </td>
                            <td class="result">
                                <span v-if="crontabValueString.length < 90">{{crontabValueString}}</span>
                                <el-tooltip v-else :content="crontabValueString" placement="top"><span>{{crontabValueString}}</span></el-tooltip>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <!-- 依据当前表达式推算最近 5 次执行时间，推算逻辑由子组件内部完成 -->
            <CrontabResult :ex="crontabValueString"></CrontabResult>

            <!-- 确定：回填表达式并关闭；重置：恢复默认通配符；取消：只关闭不回填 -->
            <div class="pop_btn">
                <el-button type="primary" @click="submitFill">确定</el-button>
                <el-button type="warning" @click="clearCron">重置</el-button>
                <el-button @click="hidePopup">取消</el-button>
            </div>
        </div>
    </div>
</template>

<script setup>
// 定时任务表单里的 cron 生成器弹层，遵循 Quartz 7 位语法（秒 分 时 日 月 周 年），纯前端拼串、不请求接口。
// 对外契约：fill 事件把最终表达式交回父组件写入表单字段，hide 事件只负责关闭弹层。
import CrontabSecond from "./second.vue"
import CrontabMin from "./min.vue"
import CrontabHour from "./hour.vue"
import CrontabDay from "./day.vue"
import CrontabMonth from "./month.vue"
import CrontabWeek from "./week.vue"
import CrontabYear from "./year.vue"
import CrontabResult from "./result.vue"
const { proxy } = getCurrentInstance()
// hide：请求父组件关闭弹层；fill：回填完整 cron 字符串（仅一个 String 参数）
const emit = defineEmits(['hide', 'fill'])
// hideComponent：需要隐藏的维度 key 数组，如只想要 6 位表达式时传 ["second", "year"]
// expression：回显用的初始表达式；调用方把本组件放进带 destroy-on-close 的 el-dialog，
// 每次打开都会重建并重新执行 onMounted，因此只在挂载时读一次即可
const props = defineProps({
    hideComponent: {
        type: Array,
        default: () => [],
    },
    expression: {
        type: String,
        default: ""
    }
})
// ===== 组件状态 =====
// 表头顺序必须与 crontabValueObj 的字段顺序一致，拼接时按同一顺序输出
const tabTitles = ref(["秒", "分钟", "小时", "日", "月", "周", "年"])
const tabActive = ref(0)
// props 在 onMounted 时拷贝到本地，子组件与判断逻辑统一读本地值
const hideComponent = ref([])
const expression = ref('')
// 各维度当前取值，键名即子组件 update 事件的第一个参数
// week 默认 "?" 是 Quartz 硬性约束：日与周必须且只能有一个为 "?"，这里默认让「日」生效
const crontabValueObj = ref({
    second: "*",
    min: "*",
    hour: "*",
    day: "*",
    month: "*",
    week: "?",
    year: "",
})
// 按 Quartz 字段顺序拼接；year 为空串表示输出 6 位表达式（Quartz 允许省略年），此时不能再补空格
const crontabValueString = computed(() => {
    const obj = crontabValueObj.value
    return obj.second
        + " "
        + obj.min
        + " "
        + obj.hour
        + " "
        + obj.day
        + " "
        + obj.month
        + " "
        + obj.week
        + (obj.year === "" ? "" : " " + obj.year)
})
// 监听本地 expression 触发反解析；该值只在 onMounted 赋一次，父组件之后再改 expression 不会同步过来
watch(expression, () => resolveExp())
// 只有出现在 hideComponent 中的维度才隐藏，返回 true 表示显示
function shouldHide(key) {
    return !(hideComponent.value && hideComponent.value.includes(key))
}
// 反解析：把 "0 0 12 * * ?" 按空白拆回 7 个字段，少于 6 段视为非法表达式直接忽略
function resolveExp() {
    // 反解析 表达式
    if (expression.value) {
        const arr = expression.value.split(/\s+/)
        if (arr.length >= 6) {
            //6 位以上是合法表达式
            let obj = {
                second: arr[0],
                min: arr[1],
                hour: arr[2],
                day: arr[3],
                month: arr[4],
                week: arr[5],
                year: arr[6] ? arr[6] : ""
            }
            crontabValueObj.value = {
                ...obj,
            }
        }
    } else {
        // 没有传入的表达式 则还原
        clearCron()
    }
}
// tab切换值
// 记录当前下标；当前模板由 el-tabs 自行维护选中态，未绑定此方法
function tabCheck(index) {
    tabActive.value = index
}
// 子组件 update 事件签名固定为 (字段名, 取值, 来源维度)，from 仅用于标识触发者，这里无需区分
// 由子组件触发，更改表达式组成的字段值
function updateCrontabValue(name, value, from) {
    crontabValueObj.value[name] = value
}
// 通过 props 下发给 7 个子组件复用的整数夹紧函数：先向下取整，再夹到 [minLimit, maxLimit]
// 子组件 input-number 的 max 会随上一项变化，这里兜底处理手工输入/回显越界的情况
// 表单选项的子组件校验数字格式（通过-props传递）
function checkNumber(value, minLimit, maxLimit) {
    // 检查必须为整数
    value = Math.floor(value)
    if (value < minLimit) {
        value = minLimit
    } else if (value > maxLimit) {
        value = maxLimit
    }
    return value
}
// 隐藏弹窗
function hidePopup() {
    emit("hide")
}
// 回填表达式并关闭弹层；父组件收到 fill 后写表单字段，弹层显隐由父组件控制
// 填充表达式
function submitFill() {
    emit("fill", crontabValueString.value)
    hidePopup()
}
// 重置为初始值：日/周恢复成 "*/?" 的默认组合，年清空（即输出 6 位表达式）
function clearCron() {
    // 还原选择项
    crontabValueObj.value = {
        second: "*",
        min: "*",
        hour: "*",
        day: "*",
        month: "*",
        week: "?",
        year: "",
    }
}
// 首次挂载才同步 props：给 expression 赋值会触发 watch 完成一次反解析，hideComponent 决定哪些 tab 渲染
onMounted(() => {
    expression.value = props.expression
    hideComponent.value = props.hideComponent
})
</script>

<style lang="scss" scoped>
.pop_btn {
    text-align: center;
    margin-top: 20px;
}
.popup-main {
    position: relative;
    margin: 10px auto;
    border-radius: 5px;
    font-size: 12px;
    overflow: hidden;
}
.popup-title {
    overflow: hidden;
    line-height: 34px;
    padding-top: 6px;
    background: #f2f2f2;
}
.popup-result {
    box-sizing: border-box;
    line-height: 24px;
    margin: 25px auto;
    padding: 15px 10px 10px;
    border: 1px solid #ccc;
    position: relative;
}
/* 标题压在上边框中间：绝对定位 + 固定宽度配负 margin 做居中，不用 transform 以兼容旧浏览器 */
.popup-result .title {
    position: absolute;
    top: -28px;
    left: 50%;
    width: 140px;
    font-size: 14px;
    margin-left: -70px;
    text-align: center;
    line-height: 30px;
    background: #fff;
}
.popup-result table {
    text-align: center;
    width: 100%;
    margin: 0 auto;
}
/* 7 个维度列宽固定，避免表达式长短变化时表格左右抖动 */
.popup-result table td:not(.result) {
    width: 3.5rem;
    min-width: 3.5rem;
    max-width: 3.5rem;
}
/* 单元格单行截断，超长内容由模板里的 el-tooltip 浮出完整值 */
.popup-result table span {
    display: block;
    width: 100%;
    font-family: arial;
    line-height: 30px;
    height: 30px;
    white-space: nowrap;
    overflow: hidden;
    border: 1px solid #e8e8e8;
}
/* 结果列表固定高度，条数变化时弹层整体高度不跳动 */
.popup-result-scroll {
    font-size: 12px;
    line-height: 24px;
    height: 10em;
    overflow-y: auto;
}
</style>
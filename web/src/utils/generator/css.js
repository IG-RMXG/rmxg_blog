/**
 * 表单生成器的样式补丁模块。
 * 设计器导出的页面里，少数 Element Plus 组件需要额外微调内部元素样式才算「好看」，
 * 这里维护「组件 tag -> 固定 CSS 片段」的映射，生成时按表单实际用到的组件挑出对应片段，
 * 交给调用方拼进 <style>。新增物料若需要样式补丁，只需在此补一条映射。
 */

const styles = {
  // 评分默认是 inline-block，顶部对齐避免与表单项 label 基线错位
  'el-rate': '.el-rate{display: inline-block; vertical-align: text-top;}',
  'el-upload': '.el-upload__tip{line-height: 1.2;}'
}

/**
 * 递归收集组件树里命中样式补丁的 CSS 片段。
 * 命中判断直接按 el.tag 查表；用 indexOf 去重，保证同一 tag 在表单中出现多次时只输出一份样式。
 * 行容器的内容放在 children 里（可多层嵌套），需要继续往下递归。
 * @param {string[]} cssList 累积结果，就地追加
 * @param {object} el 设计器元素节点
 */
function addCss(cssList, el) {
  const css = styles[el.tag]
  css && cssList.indexOf(css) === -1 && cssList.push(css)
  if (el.children) {
    el.children.forEach(el2 => addCss(cssList, el2))
  }
}

/**
 * 生成表单所需的样式文本，返回内容不带 <style> 标签，由调用方用 cssStyle() 包装。
 * 只遍历顶层 conf.fields，嵌套节点由 addCss 递归处理。
 * @param {object} conf 表单级配置，conf.fields 为顶层元素数组
 * @returns {string} 多条样式以换行拼接；没有任何组件命中补丁时返回空串
 */
export function makeUpCss(conf) {
  const cssList = []
  conf.fields.forEach(el => addCss(cssList, el))
  return cssList.join('\n')
}

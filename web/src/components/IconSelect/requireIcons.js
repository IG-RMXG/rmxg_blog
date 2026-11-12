/**
 * 图标清单：构建期用 import.meta.glob 扫描 assets/icons/svg 下的所有 SVG，把文件名当作 icon-class。
 * glob 未加 eager，modules 的值是懒加载函数；这里只用到 key，不会真正加载 SVG 内容。
 * 新增图标只需把 .svg 放进该目录，无需改代码；若目录位置调整，需同步修改下面的匹配串。
 */
let icons = []
const modules = import.meta.glob('./../../assets/icons/svg/*.svg')
// key 形如 '../../assets/icons/svg/xxx.svg'，截取文件名部分去掉扩展名
for (const path in modules) {
  const p = path.split('assets/icons/svg/')[1].split('.svg')[0]
  icons.push(p)
}

export default icons
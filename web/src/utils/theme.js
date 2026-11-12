/**
 * Element Plus 主题色（CSS 变量）动态覆盖。
 *
 * 换肤时由设置面板传入主色（如 #409EFF），这里写入 --el-color-primary 以及
 * light-1..9 / dark-1..9 共 18 个梯度变量，Element Plus 组件内部通过 var() 引用。
 * 变量名必须与其约定完全一致，不能改名，否则组件取不到色值。
 */
// 处理主题样式
export function handleThemeStyle(theme) {
	// level 取 i/10（0.1~0.9）：越小越接近主色；两轮循环分别生成浅色与深色梯度
	document.documentElement.style.setProperty('--el-color-primary', theme)
	for (let i = 1; i <= 9; i++) {
		document.documentElement.style.setProperty(`--el-color-primary-light-${i}`, `${getLightColor(theme, i / 10)}`)
	}
	for (let i = 1; i <= 9; i++) {
		document.documentElement.style.setProperty(`--el-color-primary-dark-${i}`, `${getDarkColor(theme, i / 10)}`)
	}
}

// hex颜色转rgb颜色
// 只支持 #rrggbb 六位写法，不接受 #rgb 简写，也不处理 alpha 通道
export function hexToRgb(str) {
	str = str.replace('#', '')
	let hexs = str.match(/../g)
	for (let i = 0; i < 3; i++) {
		hexs[i] = parseInt(hexs[i], 16)
	}
	return hexs
}

// rgb颜色转Hex颜色
// 单通道转 16 进制后补足两位，再拼回 #rrggbb
export function rgbToHex(r, g, b) {
	let hexs = [r.toString(16), g.toString(16), b.toString(16)]
	for (let i = 0; i < 3; i++) {
		if (hexs[i].length == 1) {
			hexs[i] = `0${hexs[i]}`
		}
	}
	return `#${hexs.join('')}`
}

// 变浅颜色值
// 向白色插值：c + (255 - c) * level，level 越大越浅
export function getLightColor(color, level) {
	let rgb = hexToRgb(color)
	for (let i = 0; i < 3; i++) {
		rgb[i] = Math.floor((255 - rgb[i]) * level + rgb[i])
	}
	return rgbToHex(rgb[0], rgb[1], rgb[2])
}

// 变深颜色值
// 向黑色插值：c * (1 - level)，level 越大越深
export function getDarkColor(color, level) {
	let rgb = hexToRgb(color)
	for (let i = 0; i < 3; i++) {
		rgb[i] = Math.floor(rgb[i] * (1 - level))
	}
	return rgbToHex(rgb[0], rgb[1], rgb[2])
}

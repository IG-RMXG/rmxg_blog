<template >
  <!-- 目录型菜单的占位组件：把 children 路由渲染在当前位置，自身不产生 DOM 与样式。
       由后端菜单表把「目录」节点的 component 指向它，使嵌套路由有一层父级出口。 -->
  <router-view />
</template>

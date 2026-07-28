import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'personnel',
        name: 'PersonnelManage',
        component: () => import('@/views/PersonnelManage.vue'),
        meta: { title: '人员管理' }
      },
      {
        path: 'project',
        name: 'ProjectManage',
        component: () => import('@/views/ProjectManage.vue'),
        meta: { title: '项目管理' }
      },
      {
        path: 'assignment',
        name: 'AssignmentManage',
        component: () => import('@/views/AssignmentManage.vue'),
        meta: { title: '分配管理' }
      },
      {
        path: 'conflict',
        name: 'ConflictList',
        component: () => import('@/views/ConflictList.vue'),
        meta: { title: '冲突清单' }
      },
      {
        path: 'calendar',
        name: 'CalendarView',
        component: () => import('@/views/CalendarView.vue'),
        meta: { title: '日历视图' }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录拦截
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router

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
        meta: { title: '仪表盘', roles: ['ADMIN', 'PROJECT_LEAD', 'USER'] }
      },
      {
        path: 'personnel',
        name: 'PersonnelManage',
        component: () => import('@/views/PersonnelManage.vue'),
        meta: { title: '人员管理', roles: ['ADMIN', 'PROJECT_LEAD'] }
      },
      {
        path: 'project',
        name: 'ProjectManage',
        component: () => import('@/views/ProjectManage.vue'),
        meta: { title: '项目管理', roles: ['ADMIN', 'PROJECT_LEAD'] }
      },
      {
        path: 'assignment',
        name: 'AssignmentManage',
        component: () => import('@/views/AssignmentManage.vue'),
        meta: { title: '分配管理', roles: ['ADMIN', 'PROJECT_LEAD'] }
      },
      {
        path: 'conflict',
        name: 'ConflictList',
        component: () => import('@/views/ConflictList.vue'),
        meta: { title: '冲突清单', roles: ['ADMIN', 'PROJECT_LEAD', 'USER'] }
      },
      {
        path: 'calendar',
        name: 'CalendarView',
        component: () => import('@/views/CalendarView.vue'),
        meta: { title: '日历视图', roles: ['ADMIN', 'PROJECT_LEAD', 'USER'] }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录拦截 + 角色权限校验
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  // 清理旧的 mock token（之前 mock 登录遗留的假 token）
  if (userStore.token && userStore.token.startsWith('mock-token-')) {
    userStore.logout()
    next({ path: '/login' })
    return
  }

  // 未登录拦截
  if (to.meta.requiresAuth && !userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 角色权限校验（大小写不敏感）
  const roles = to.meta.roles as string[] | undefined
  if (roles && roles.length > 0) {
    const userRole = (userStore.role || '').toUpperCase()
    if (!roles.includes(userRole)) {
      // 防止无限重定向：如果已经在去 /dashboard 的路上，直接放行
      if (to.path === '/dashboard') {
        next()
        return
      }
      // 无权限访问该路由，重定向到仪表盘
      next({ path: '/dashboard' })
      return
    }
  }

  next()
})

export default router

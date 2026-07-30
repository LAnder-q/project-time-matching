<template>
  <div class="app-shell" :class="{ collapsed: isCollapsed }">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-brand" @click="router.push('/dashboard')">
        <div class="brand-mark">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="#4f46e5" />
            <path d="M8 10h12M8 14h12M8 18h7" stroke="#fff" stroke-width="2" stroke-linecap="round" />
            <circle cx="20" cy="18" r="3" fill="#818cf8" stroke="#fff" stroke-width="1.5" />
          </svg>
        </div>
        <transition name="fade-slide">
          <div v-show="!isCollapsed" class="brand-text">
            <span class="brand-title">资源匹配</span>
            <span class="brand-sub">Project Time Matching</span>
          </div>
        </transition>
      </div>

      <nav class="sidebar-nav">
        <div v-for="item in filteredMenus" :key="item.path" class="nav-item-wrapper">
          <router-link
            :to="item.path"
            class="nav-item"
            :class="{ active: activeMenu === item.path }"
          >
            <div class="nav-icon">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <transition name="fade-slide">
              <span v-show="!isCollapsed" class="nav-label">{{ item.title }}</span>
            </transition>
            <div v-show="activeMenu === item.path" class="active-bar" />
          </router-link>
        </div>
      </nav>

      <div v-show="!isCollapsed" class="sidebar-footer">
        <div class="version-badge">v1.0.0</div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-area">
      <header class="topbar">
        <button class="collapse-btn" @click="toggleCollapse">
          <el-icon :size="18">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
        </button>

        <div class="topbar-breadcrumb">
          <span class="breadcrumb-current">{{ currentMenuTitle }}</span>
        </div>

        <div class="topbar-right">
          <div class="role-chip" :class="roleClass">
            <span class="role-dot" />
            {{ roleLabel }}
          </div>
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-chip">
              <div class="user-avatar">
                {{ displayName.charAt(0).toUpperCase() }}
              </div>
              <span class="user-name">{{ displayName }}</span>
              <el-icon :size="14"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Odometer, User, Folder, Connection, Warning, Calendar,
  ArrowDown, Fold, Expand, SwitchButton, OfficeBuilding
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

const allMenus = [
  { path: '/dashboard', title: '仪表盘', icon: Odometer, roles: ['ADMIN', 'PROJECT_LEAD', 'USER'] },
  { path: '/personnel', title: '人员管理', icon: User, roles: ['ADMIN', 'PROJECT_LEAD'] },
  { path: '/department', title: '部门管理', icon: OfficeBuilding, roles: ['ADMIN'] },
  { path: '/project', title: '项目管理', icon: Folder, roles: ['ADMIN', 'PROJECT_LEAD'] },
  { path: '/assignment', title: '分配管理', icon: Connection, roles: ['ADMIN', 'PROJECT_LEAD'] },
  { path: '/conflict', title: '冲突清单', icon: Warning, roles: ['ADMIN', 'PROJECT_LEAD', 'USER'] },
  { path: '/calendar', title: '日历视图', icon: Calendar, roles: ['ADMIN', 'PROJECT_LEAD', 'USER'] }
]

const filteredMenus = computed(() => {
  const role = (userStore.role || '').toUpperCase()
  return allMenus.filter(item => item.roles.includes(role))
})

const currentMenuTitle = computed(() => {
  const found = allMenus.find(m => m.path === route.path)
  return found?.title || ''
})

const displayName = computed(() => {
  const info = userStore.userInfo as { realName?: string; username?: string } | null
  return info?.realName || info?.username || '用户'
})

const roleLabel = computed(() => {
  const roleMap: Record<string, string> = {
    ADMIN: '管理员',
    PROJECT_LEAD: '项目负责人',
    USER: '运维人员'
  }
  return roleMap[userStore.role] || '未知'
})

const roleClass = computed(() => {
  const c = (userStore.role || '').toLowerCase()
  if (c === 'admin') return 'role-admin'
  if (c === 'project_lead') return 'role-lead'
  return 'role-user'
})

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  }
}
</script>

<style scoped lang="scss">
.app-shell {
  display: flex;
  height: 100%;
  --sidebar-w: 240px;
  transition: --sidebar-w 0.3s ease;

  &.collapsed {
    --sidebar-w: 72px;
  }
}

/* ---- 侧边栏 ---- */
.sidebar {
  width: var(--sidebar-w);
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid var(--pw-border);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  z-index: 10;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px;
  cursor: pointer;
  border-bottom: 1px solid var(--pw-border-light);
  min-height: 64px;

  .brand-mark {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .brand-text {
    display: flex;
    flex-direction: column;
    overflow: hidden;
    white-space: nowrap;

    .brand-title {
      font-size: 16px;
      font-weight: 700;
      color: var(--pw-text-primary);
      letter-spacing: -0.01em;
      line-height: 1.2;
    }

    .brand-sub {
      font-size: 10px;
      color: var(--pw-text-secondary);
      letter-spacing: 0.05em;
      text-transform: uppercase;
    }
  }
}

.sidebar-nav {
  flex: 1;
  padding: 12px 12px;
  overflow-y: auto;
  overflow-x: hidden;

  .nav-item-wrapper {
    margin-bottom: 2px;
  }
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--pw-radius);
  color: var(--pw-text-regular);
  cursor: pointer;
  transition: all var(--pw-transition);
  position: relative;
  text-decoration: none;

  .nav-icon {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    transition: color var(--pw-transition);
  }

  .nav-label {
    font-size: 14px;
    font-weight: 500;
    white-space: nowrap;
  }

  &:hover {
    background-color: var(--pw-primary-lightest);
    color: var(--pw-primary);

    .nav-icon {
      color: var(--pw-primary);
    }
  }

  &.active {
    background-color: var(--pw-primary-lightest);
    color: var(--pw-primary);
    font-weight: 600;

    .nav-icon {
      color: var(--pw-primary);
    }
  }

  .active-bar {
    position: absolute;
    left: -12px;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 24px;
    background: var(--pw-primary);
    border-radius: 0 3px 3px 0;
  }
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--pw-border-light);

  .version-badge {
    font-size: 11px;
    color: var(--pw-text-secondary);
    text-align: center;
  }
}

/* ---- 主区域 ---- */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--pw-bg);
}

.topbar {
  height: 64px;
  flex-shrink: 0;
  background: #fff;
  border-bottom: 1px solid var(--pw-border);
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 16px;

  .collapse-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: 1px solid var(--pw-border);
    border-radius: var(--pw-radius-sm);
    background: #fff;
    color: var(--pw-text-regular);
    cursor: pointer;
    transition: all var(--pw-transition);

    &:hover {
      border-color: var(--pw-primary);
      color: var(--pw-primary);
      background: var(--pw-primary-lightest);
    }
  }

  .topbar-breadcrumb {
    flex: 1;

    .breadcrumb-current {
      font-size: 17px;
      font-weight: 600;
      color: var(--pw-text-primary);
    }
  }

  .topbar-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}

.role-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 600;

  .role-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  &.role-admin {
    background: #fef2f2;
    color: #dc2626;
    .role-dot { background: #dc2626; }
  }

  &.role-lead {
    background: #fffbeb;
    color: #d97706;
    .role-dot { background: #d97706; }
  }

  &.role-user {
    background: #f0f9ff;
    color: #0284c7;
    .role-dot { background: #0284c7; }
  }
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: 100px;
  transition: background var(--pw-transition);
  outline: none;

  &:hover {
    background: var(--pw-bg-hover);
  }

  .user-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: linear-gradient(135deg, #4f46e5, #6366f1);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    flex-shrink: 0;
  }

  .user-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--pw-text-primary);
  }
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* ---- 过渡动画 ---- */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(-8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.15s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>

// 1. 声明vite/client
declare module 'vite/client' {
    const client: any
    export default client
}

// 2. 声明@dcloudio/uni-app（解决找不到模块+所有生命周期API）
declare module '@dcloudio/uni-app' {
    export const onLoad: any
    export const onPullDownRefresh: any
    export const onReachBottom: any
}

// 3. 完整声明vue（解决ref泛型+computed/watch+defineProps所有报错）
declare module 'vue' {
    // 给ref添加泛型支持，彻底解决ref<SpecialItem>红线
    export function ref<T>(value: T): { value: T }
    export const computed: any
    export const watch: any
    export const defineComponent: any
    export function defineProps<T>(): T
    export type DefineComponent = any
}

// 4. 声明.vue文件
declare module '*.vue' {
    const component: any
    export default component
}

// 5. 声明uni全局API
declare namespace UniApp {
    interface Uni {
        request: any
        showToast: any
        stopPullDownRefresh: any
        navigateTo: any
    }
}

// 6. 声明@/types/special（彻底解决SpecialItem/SpecialPageResult导入报错）
declare module '@/types/special' {
    export interface SpecialItem {
        id: string | number
        title?: string
        introduction?: string
        banner?: string
        view_count?: number
        followers_count?: number
        is_following?: boolean
        updated?: string
        cover?: string
        [key: string]: any
    }
    export interface SpecialPageResult {
        list: SpecialItem[]
        total: number
    }
}
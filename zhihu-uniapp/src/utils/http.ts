// src/utils/http.ts
// 通用GET请求封装，适配uni-app
export async function httpGet<T>(url: string, options?: {
    params?: Record<string, any>;
    [key: string]: any;
}): Promise<T> {
    return new Promise((resolve, reject) => {
        uni.request({
            url: `http://你的局域网IP:8080${url}`, // 替换为你的后端SpringBoot地址
            method: "GET",
            data: options?.params,
            success: (res) => {
                if (res.statusCode === 200) {
                    resolve(res.data as T);
                } else {
                    reject(new Error(`请求失败: ${res.statusCode}`));
                }
            },
            fail: (err) => reject(err),
            ...options,
        });
    });
}
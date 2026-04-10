// src/api/special.ts
import { httpGet } from "@/utils/http";
import {
    asRecord,
    normalizeItem,
    pickOneRecord,
    pickRows,
    pickTotal,
} from "@/api/specialNormalize";

import type { SpecialItem, SpecialPageResult } from "@/types/special";

/** 分页列表 */
export async function fetchSpecialByTitle(params: {
    title: string;
    pageNum: number;
    pageSize: number;
}): Promise<SpecialPageResult> {
    const data = await httpGet<unknown>("/v1/special/page", {
        params: {
            title: params.title,
            pageNum: params.pageNum,
            pageSize: params.pageSize,
        },
    });

    // 给 r 显式标注 unknown，彻底解决隐式 any 报错
    const list = pickRows(data)
        .map((r: unknown) => asRecord(r))
        .filter(Boolean)
        .map((r) => normalizeItem(r!));

    let total = pickTotal(data);
    if (total <= 0 && list.length > 0) total = list.length;
    return { list, total };
}

/** 详情 */
export async function fetchSpecialById(
    id: string,
): Promise<SpecialItem | null> {
    const data = await httpGet<unknown>("/v1/special/detail", {
        params: { id },
    });
    const row = pickOneRecord(data);
    return row ? normalizeItem(row) : null;
}
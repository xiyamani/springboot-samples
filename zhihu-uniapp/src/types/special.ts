export interface SpecialItem {
    id: string | number;
    title: string;
    introduction: string;
    section_list: string;
    banner: string;
    view_count: number;
    followers_count: number;
    is_following: boolean;
    updated: string;
    cover?: string;
}

export interface SpecialPageResult {
    list: SpecialItem[];
    total: number;
}
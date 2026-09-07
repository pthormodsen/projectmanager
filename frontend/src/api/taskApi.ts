export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";
const AUTH_STORAGE_KEY = "projectmanager.basicAuth";

export class UnauthorizedError extends Error {
    constructor() {
        super("Sign in required");
        this.name = "UnauthorizedError";
    }
}

function apiUrl(path: string) {
    const base = API_BASE_URL.endsWith("/") ? API_BASE_URL.slice(0, -1) : API_BASE_URL;
    const normalizedPath = path.startsWith("/") ? path : `/${path}`;
    return `${base}${normalizedPath}`;
}

async function readErrorMessage(res: Response) {
    const fallback = `Request failed with status ${res.status}`;
    const contentType = res.headers.get("content-type") ?? "";
    const body = await res.text();

    if (!body) {
        return fallback;
    }

    if (contentType.includes("application/json")) {
        try {
            const parsed = JSON.parse(body) as {
                message?: string;
                error?: string;
            };
            return parsed.message || parsed.error || fallback;
        } catch {
            return fallback;
        }
    }

    return body.length > 300 ? fallback : body;
}

async function apiFetch(path: string, init?: RequestInit, includeAuth = true) {
    const auth = localStorage.getItem(AUTH_STORAGE_KEY);
    const res = await fetch(apiUrl(path), {
        ...init,
        headers: {
            "Content-Type": "application/json",
            ...(includeAuth && auth ? { Authorization: `Basic ${auth}` } : {}),
            ...init?.headers,
        },
    });

    if (res.status === 401) {
        throw new UnauthorizedError();
    }

    if (!res.ok) {
        throw new Error(await readErrorMessage(res));
    }

    return res;
}

export function hasStoredCredentials() {
    return localStorage.getItem(AUTH_STORAGE_KEY) !== null;
}

export function setApiCredentials(username: string, password: string) {
    localStorage.setItem(AUTH_STORAGE_KEY, btoa(`${username.trim()}:${password}`));
}

export function clearApiCredentials() {
    localStorage.removeItem(AUTH_STORAGE_KEY);
}

export interface Project {
    id: number;
    name: string;
    description?: string;
}

export interface User {
    id: number;
    username: string;
    email: string;
}

export interface Task {
    id: number;
    title: string;
    description?: string;
    completed: boolean;
    status: TaskStatus;
    project: Project;
    user?: User | null;
}

export async function getTasks(): Promise<Task[]> {
    const res = await apiFetch("/tasks");
    return res.json();
}

export async function updateTaskStatus(
    taskId: number,
    status: TaskStatus
): Promise<void> {
    await apiFetch(`/tasks/${taskId}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
    });
}

export async function createTask(task: {
    title: string;
    description?: string;
    status: TaskStatus;
}) {
    const res = await apiFetch("/tasks", {
        method: "POST",
        body: JSON.stringify(task),
    });

    return res.json();
}

export async function registerUser(user: {
    username: string;
    email: string;
    password: string;
}): Promise<User> {
    const res = await apiFetch("/users", {
        method: "POST",
        body: JSON.stringify(user),
    }, false);

    return res.json();
}

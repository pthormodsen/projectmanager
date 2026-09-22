export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";
const AUTH_STORAGE_KEY = "projectmanager.basicAuth";
const DEMO_STORAGE_KEY = "projectmanager.demo.tasks.v2";

const DEMO_PROJECT: Project = {
    id: 1,
    name: "Portfolio Demo",
    description: "Example project data for demo visitors",
};

const DEMO_USER: User = {
    id: 1,
    username: "demo",
    email: "demo@example.com",
};

const DEMO_TASKS: Task[] = [
    {
        id: 1,
        title: "Confirm API contract for customer import",
        description: "Check required CSV fields with Sara before the backend endpoint is finalized.",
        completed: false,
        status: "TODO",
        project: DEMO_PROJECT,
        user: DEMO_USER,
    },
    {
        id: 2,
        title: "Wire task status updates to the board",
        description: "Patch cards optimistically and refresh from the API after each move.",
        completed: false,
        status: "IN_PROGRESS",
        project: DEMO_PROJECT,
        user: DEMO_USER,
    },
    {
        id: 3,
        title: "Write handoff notes for Friday demo",
        description: "Summarize login, project setup, and the remaining edge cases.",
        completed: true,
        status: "DONE",
        project: DEMO_PROJECT,
        user: DEMO_USER,
    },
    {
        id: 4,
        title: "Add empty-state copy for new projects",
        description: "The board should explain what to do before the first task is created.",
        completed: false,
        status: "TODO",
        project: DEMO_PROJECT,
        user: DEMO_USER,
    },
];

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

export function isDemoMode() {
    const params = new URLSearchParams(window.location.search);
    return window.location.pathname === "/demo" || params.get("demo") === "true";
}

function readDemoTasks() {
    const storedTasks = sessionStorage.getItem(DEMO_STORAGE_KEY);
    if (storedTasks) {
        try {
            const tasks = JSON.parse(storedTasks) as Task[];
            if (Array.isArray(tasks) && tasks.length > 0) {
                return tasks;
            }
        } catch {
            sessionStorage.removeItem(DEMO_STORAGE_KEY);
        }
    }

    sessionStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(DEMO_TASKS));
    return DEMO_TASKS;
}

function writeDemoTasks(tasks: Task[]) {
    sessionStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(tasks));
}

export function resetDemoTasks() {
    sessionStorage.removeItem(DEMO_STORAGE_KEY);
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
    if (isDemoMode()) {
        return true;
    }

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
    if (isDemoMode()) {
        return readDemoTasks();
    }

    const res = await apiFetch("/tasks");
    return res.json();
}

export async function updateTaskStatus(
    taskId: number,
    status: TaskStatus
): Promise<void> {
    if (isDemoMode()) {
        const tasks = readDemoTasks().map(task =>
            task.id === taskId
                ? { ...task, status, completed: status === "DONE" }
                : task
        );
        writeDemoTasks(tasks);
        return;
    }

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
    if (isDemoMode()) {
        const tasks = readDemoTasks();
        const nextId = Math.max(0, ...tasks.map(existingTask => existingTask.id)) + 1;
        const newTask: Task = {
            id: nextId,
            title: task.title,
            description: task.description,
            status: task.status,
            completed: task.status === "DONE",
            project: DEMO_PROJECT,
            user: DEMO_USER,
        };
        writeDemoTasks([...tasks, newTask]);
        return newTask;
    }

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

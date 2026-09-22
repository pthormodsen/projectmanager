import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import type { Task, TaskStatus } from "./api/taskApi";
import {
    clearApiCredentials,
    createTask,
    getTasks,
    hasStoredCredentials,
    isDemoMode,
    registerUser,
    resetDemoTasks,
    setApiCredentials,
    UnauthorizedError,
    updateTaskStatus,
} from "./api/taskApi";
import KanbanBoard from "./components/KanbanBoard";
import AddTaskModal from "./components/AddTaskModal";
import { LogOut, Play, Plus } from "lucide-react";

export default function App() {
    const demoMode = isDemoMode();

    const [tasks, setTasks] = useState<Task[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [needsSignIn, setNeedsSignIn] = useState(!hasStoredCredentials());
    const [isModalOpen, setIsModalOpen] = useState(false);

    async function loadTasks() {
        setLoading(true);
        try {
            const tasks = await getTasks();
            setTasks(tasks);
            setError(null);
            setNeedsSignIn(false);
        } catch (err) {
            if (err instanceof UnauthorizedError) {
                setNeedsSignIn(true);
                setError(null);
                throw err;
            }
            setNeedsSignIn(false);
            setError(err instanceof Error ? err.message : "Failed to fetch tasks");
            throw err;
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        if (!hasStoredCredentials()) {
            setLoading(false);
            return;
        }

        loadTasks().catch(() => {});
    }, []);

    async function handleSignIn(username: string, password: string) {
        try {
            setApiCredentials(username, password);
            await loadTasks();
        } catch (err) {
            if (err instanceof UnauthorizedError) {
                clearApiCredentials();
            }
            throw err;
        }
    }

    function handleSessionAction() {
        if (demoMode) {
            resetDemoTasks();
            clearApiCredentials();
            window.location.assign("/");
            return;
        }

        clearApiCredentials();
        setTasks([]);
        setNeedsSignIn(true);
        setError(null);
    }

    async function moveTask(taskId: number, status: TaskStatus) {
        try {
            await updateTaskStatus(taskId, status);
            setTasks(prev =>
                prev.map(task =>
                    task.id === taskId
                        ? { ...task, status, completed: status === "DONE" }
                        : task
                )
            );
            setError(null);
        } catch (err) {
            if (err instanceof UnauthorizedError) {
                setNeedsSignIn(true);
                return;
            }
            setError(err instanceof Error ? err.message : "Failed to update task");
        }
    }

    async function handleCreateTask(title: string, description: string) {
        try {
            const newTask = await createTask({
                title,
                description,
                status: "TODO",
            });

            setTasks(prev => [...prev, newTask]);
            setIsModalOpen(false);
            setError(null);
        } catch (err) {
            if (err instanceof UnauthorizedError) {
                setNeedsSignIn(true);
                return;
            }
            setError(err instanceof Error ? err.message : "Failed to create task");
        }
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
            <div className="max-w-7xl mx-auto p-6">
                {/* Header */}
                <div className="mb-8 flex items-center justify-between">
                    <div>
                        <h1 className="text-4xl font-bold text-gray-900 mb-2">
                            Project Manager
                        </h1>
                        <p className="text-gray-600">
                            {demoMode
                                ? "Demo mode uses example tasks in this browser session only"
                                : "Manage your tasks efficiently"}
                        </p>
                    </div>

                    {!needsSignIn && (
                        <div className="flex items-center gap-2">
                            <button
                                onClick={handleSessionAction}
                                className="flex items-center gap-2 px-3 py-2 sm:px-4 bg-white text-gray-700 rounded-lg hover:bg-gray-50 border border-gray-200 shadow-sm"
                                title={demoMode ? "Exit demo" : "Sign out"}
                            >
                                <LogOut size={18} />
                                <span className="hidden sm:inline font-medium">
                                    {demoMode ? "Exit demo" : "Sign out"}
                                </span>
                            </button>
                            <button
                                onClick={() => setIsModalOpen(true)}
                                className="flex items-center gap-2 px-3 py-2 sm:px-4 bg-blue-600 text-white rounded-lg hover:bg-blue-700 shadow-sm"
                            >
                                <Plus size={18} />
                                <span className="hidden sm:inline font-medium">Add Task</span>
                            </button>
                        </div>
                    )}
                </div>

                {needsSignIn ? (
                    <SignInPanel onSignIn={handleSignIn} />
                ) : loading ? (
                    <div className="flex justify-center py-20 text-gray-500">
                        Loading tasks...
                    </div>
                ) : (
                    <>
                        {error && (
                            <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                                {error}
                            </div>
                        )}
                        <KanbanBoard tasks={tasks} onMoveTask={moveTask} />
                    </>
                )}
            </div>

            <AddTaskModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onCreate={handleCreateTask}
            />
        </div>
    );
}

function SignInPanel({
    onSignIn,
}: {
    onSignIn: (username: string, password: string) => Promise<void>;
}) {
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [isRegistering, setIsRegistering] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setSubmitting(true);
        setError(null);

        try {
            if (isRegistering) {
                await registerUser({ username, email, password });
            }
            await onSignIn(username, password);
        } catch (err) {
            if (err instanceof UnauthorizedError) {
                setError("Wrong username or password");
            } else {
                setError(err instanceof Error ? err.message : "Request failed");
            }
        } finally {
            setSubmitting(false);
        }
    }

    function handleTryDemo() {
        clearApiCredentials();
        resetDemoTasks();
        window.location.assign("/?demo=true");
    }

    return (
        <form
            onSubmit={handleSubmit}
            className="mx-auto max-w-sm rounded-lg border border-gray-200 bg-white p-6 shadow-sm"
        >
            <h2 className="mb-4 text-lg font-semibold text-gray-900">
                {isRegistering ? "Create account" : "Sign in"}
            </h2>

            {error && (
                <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {error}
                </div>
            )}

            <label className="mb-3 block">
                <span className="mb-1 block text-sm font-medium text-gray-700">Username</span>
                <input
                    value={username}
                    onChange={event => setUsername(event.target.value)}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2"
                    autoComplete="username"
                />
            </label>

            {isRegistering && (
                <label className="mb-3 block">
                    <span className="mb-1 block text-sm font-medium text-gray-700">Email</span>
                    <input
                        value={email}
                        onChange={event => setEmail(event.target.value)}
                        className="w-full rounded-lg border border-gray-300 px-3 py-2"
                        autoComplete="email"
                        type="email"
                    />
                </label>
            )}

            <label className="mb-5 block">
                <span className="mb-1 block text-sm font-medium text-gray-700">Password</span>
                <input
                    type="password"
                    value={password}
                    onChange={event => setPassword(event.target.value)}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2"
                    autoComplete="current-password"
                />
            </label>

            <button
                type="submit"
                disabled={submitting || !username || !password || (isRegistering && !email)}
                className="w-full rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
                {submitting
                    ? isRegistering ? "Creating account..." : "Signing in..."
                    : isRegistering ? "Create account" : "Sign in"}
            </button>

            <button
                type="button"
                onClick={() => {
                    setIsRegistering(prev => !prev);
                    setError(null);
                }}
                className="mt-3 w-full rounded-lg px-4 py-2 text-sm font-medium text-blue-700 hover:bg-blue-50"
            >
                {isRegistering ? "Use an existing account" : "Create a new account"}
            </button>

            <button
                type="button"
                onClick={handleTryDemo}
                className="mt-2 flex w-full items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium text-blue-700 hover:bg-blue-50"
            >
                <Play size={16} />
                Try demo
            </button>
        </form>
    );
}

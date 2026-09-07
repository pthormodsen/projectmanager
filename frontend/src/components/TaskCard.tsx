import type { Task, TaskStatus } from "../api/taskApi";
import { ArrowLeft, ArrowRight, User } from "lucide-react";

type Props = {
    task: Task;
    onMoveTask: (taskId: number, status: TaskStatus) => void;
};

export default function TaskCard({ task, onMoveTask }: Props) {
    const canMoveLeft = task.status !== "TODO";
    const canMoveRight = task.status !== "DONE";

    return (
        <div className="bg-white rounded-lg p-4 shadow-sm border border-gray-200 hover:shadow-md transition-shadow">
            {/* Task Title */}
            <h3 className="font-semibold text-gray-900 text-sm mb-2 leading-snug">
                {task.title}
            </h3>

            {/* Task Description */}
            {task.description && (
                <p className="text-xs text-gray-600 mb-3 leading-relaxed">
                    {task.description}
                </p>
            )}

            {/* Task Meta Info */}
            <div className="flex items-center gap-2 mb-3 pb-3 border-b border-gray-100">
                {task.user && (
                    <div className="flex items-center gap-1.5 text-xs text-gray-500">
                        <User size={14} />
                        <span>{task.user.username}</span>
                    </div>
                )}
                {task.project && (
                    <div className="text-xs text-gray-500 px-2 py-0.5 bg-gray-100 rounded">
                        {task.project.name}
                    </div>
                )}
            </div>

            {/* Action Buttons */}
            <div className="flex gap-2">
                {canMoveLeft && (
                    <button
                        onClick={() => {
                            const newStatus = task.status === "DONE" ? "IN_PROGRESS" : "TODO";
                            onMoveTask(task.id, newStatus);
                        }}
                        className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-50 rounded-md border border-gray-300 hover:bg-gray-100 hover:border-gray-400 transition-colors"
                        title="Move left"
                    >
                        <ArrowLeft size={14} />
                        <span>Back</span>
                    </button>
                )}

                {canMoveRight && (
                    <button
                        onClick={() => {
                            const newStatus = task.status === "TODO" ? "IN_PROGRESS" : "DONE";
                            onMoveTask(task.id, newStatus);
                        }}
                        className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-blue-700 bg-blue-50 rounded-md border border-blue-200 hover:bg-blue-100 hover:border-blue-300 transition-colors ml-auto"
                        title="Move right"
                    >
                        <span>Next</span>
                        <ArrowRight size={14} />
                    </button>
                )}
            </div>
        </div>
    );
}
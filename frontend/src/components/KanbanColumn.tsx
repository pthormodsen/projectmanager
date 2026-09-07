import type { Task, TaskStatus } from "../api/taskApi";
import TaskCard from "./TaskCard";

type Props = {
    title: string;
    status: TaskStatus;
    tasks: Task[];
    onMoveTask: (taskId: number, status: TaskStatus) => void;
};

export default function KanbanColumn({
                                         title,
                                         status,
                                         tasks,
                                         onMoveTask,
                                     }: Props) {
    const filteredTasks = tasks.filter(task => task.status === status);

    return (
        <div className="bg-white rounded-lg shadow p-4">
            <h2 className="font-semibold mb-4">{title}</h2>

            <div className="space-y-3">
                {filteredTasks.map(task => (
                    <TaskCard
                        key={task.id}
                        task={task}
                        onMoveTask={onMoveTask}
                    />
                ))}
            </div>
        </div>
    );
}

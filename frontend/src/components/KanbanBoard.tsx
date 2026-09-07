import KanbanColumn from "./KanbanColumn";
import type { Task, TaskStatus } from "../api/taskApi";

type Props = {
    tasks: Task[];
    onMoveTask: (taskId: number, status: TaskStatus) => void;
};

export default function KanbanBoard({ tasks, onMoveTask }: Props) {
    return (
        <div className="grid grid-cols-3 gap-6">
            <KanbanColumn
                title="TODO"
                status="TODO"
                tasks={tasks}
                onMoveTask={onMoveTask}
            />

            <KanbanColumn
                title="IN PROGRESS"
                status="IN_PROGRESS"
                tasks={tasks}
                onMoveTask={onMoveTask}
            />

            <KanbanColumn
                title="DONE"
                status="DONE"
                tasks={tasks}
                onMoveTask={onMoveTask}
            />
        </div>
    );
}

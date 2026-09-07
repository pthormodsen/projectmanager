import { useState } from "react";

type Props = {
    isOpen: boolean;
    onClose: () => void;
    onCreate: (title: string, description: string) => Promise<void>;
};

export default function AddTaskModal({ isOpen, onClose, onCreate }: Props) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [submitting, setSubmitting] = useState(false);

    if (!isOpen) return null;

    async function handleSubmit() {
        if (!title.trim()) return;
        setSubmitting(true);
        try {
            await onCreate(title.trim(), description.trim());
            setTitle("");
            setDescription("");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
            <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-lg">
                <h2 className="text-xl font-semibold mb-4">Add Task</h2>

                <input
                    type="text"
                    placeholder="Title"
                    className="w-full border rounded-lg px-3 py-2 mb-3"
                    value={title}
                    onChange={e => setTitle(e.target.value)}
                />

                <textarea
                    placeholder="Description"
                    className="w-full border rounded-lg px-3 py-2 mb-4"
                    value={description}
                    onChange={e => setDescription(e.target.value)}
                />

                <div className="flex justify-end gap-2">
                    <button
                        onClick={onClose}
                        disabled={submitting}
                        className="px-4 py-2 rounded-lg border"
                    >
                        Cancel
                    </button>

                    <button
                        onClick={handleSubmit}
                        disabled={submitting || !title.trim()}
                        className="px-4 py-2 rounded-lg bg-blue-600 text-white disabled:opacity-50"
                    >
                        {submitting ? "Creating..." : "Create"}
                    </button>
                </div>
            </div>
        </div>
    );
}

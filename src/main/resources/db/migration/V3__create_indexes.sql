create index idx_tasks_status on tasks (status);
create index idx_tasks_category on tasks (category);
create index idx_tasks_scheduled_date on tasks (scheduled_date);
create index idx_tasks_due_date on tasks (due_date);
create index idx_tasks_responsible_user on tasks (responsible_user_id);
create index idx_tasks_created_by on tasks (created_by);
